package com.enigma.audiobook.backend.aws;

import com.enigma.audiobook.backend.aws.models.MPURequestStatus;
import com.enigma.audiobook.backend.aws.models.S3MPUCompletedPart;
import com.enigma.audiobook.backend.aws.models.S3MPUPreSignedUrlsResponse;
import com.enigma.audiobook.backend.controllers.UploadController;
import com.enigma.audiobook.backend.models.requests.UploadCompletionReq;
import com.enigma.audiobook.backend.models.requests.UploadFileCompletionReq;
import com.enigma.audiobook.backend.models.requests.UploadFileInitReq;
import com.enigma.audiobook.backend.models.requests.UploadInitReq;
import com.enigma.audiobook.backend.models.responses.UploadCompletionRes;
import com.enigma.audiobook.backend.models.responses.UploadFileInitRes;
import com.enigma.audiobook.backend.models.responses.UploadInitRes;
import com.enigma.audiobook.backend.proxies.RestClient;
import com.enigma.audiobook.backend.utils.SerDe;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.apache.http.NameValuePair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class S3MPUClientMain {
    static RestClient restClient = new RestClient();
    static SerDe serDe = new SerDe();
    static ExecutorService executorService = Executors.newFixedThreadPool(5);

    public static void main(String[] args) {
        String filePath = args[0];
        uploadFile(filePath);
    }

    private static void uploadFile(String filePath) {


        File file = new File(filePath);
        UploadInitRes response = initUpload(file);

        if (!response.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            log.error("upload init request failed:" + response);
            return;
        }

        log.info("upload init response:" + response);
        Map<String, UploadFileInitRes> fileNameToUploadFileResponse =
                response.getFileNameToUploadFileResponse();

        UploadFileInitRes fileInitRes = fileNameToUploadFileResponse.get(file.getName());
        S3MPUPreSignedUrlsResponse s3MPUPreSignedUrlsResponse = fileInitRes.getS3MPUPreSignedUrlsResponse();

        List<S3MPUCompletedPart> completedParts = uploadParts(s3MPUPreSignedUrlsResponse, file);
        UploadCompletionReq completionReq = new UploadCompletionReq();

        List<UploadFileCompletionReq> uploadFileCompletionReqs = new ArrayList<>();
        UploadFileCompletionReq uploadFileCompletionReq = new UploadFileCompletionReq();
        uploadFileCompletionReq.setUploadId(fileInitRes.getUploadId());
        uploadFileCompletionReq.setObjectKey(fileInitRes.getObjectKey());
        uploadFileCompletionReq.setFileName(file.getName());
        uploadFileCompletionReq.setS3MPUCompletedParts(completedParts);

        uploadFileCompletionReqs.add(uploadFileCompletionReq);
        completionReq.setUploadFileCompletionReqs(uploadFileCompletionReqs);

        UploadCompletionRes completionRes = completeUpload(completionReq);
        if (!completionRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            log.error("upload completion request failed:" + completionRes);
            return;
        }

    }

    private static UploadCompletionRes completeUpload(UploadCompletionReq completionReq) {
        String URL_PREFIX = "localhost:8080";
        String URL_PATH_UPLOAD_INIT = "uploads/completion";


        UploadCompletionRes response =
                restClient.doPost(URL_PREFIX, URL_PATH_UPLOAD_INIT, serDe.toJson(completionReq),
                        UploadCompletionRes.class);
        return response;
    }

    private static List<S3MPUCompletedPart> uploadParts(S3MPUPreSignedUrlsResponse s3MPUPreSignedUrlsResponse, File file) {
        long chunkSize = s3MPUPreSignedUrlsResponse.getChunkSize();
        long totalNumOfParts = s3MPUPreSignedUrlsResponse.getTotalNumOfParts();
        Map<Integer, String> partNumToUrl = s3MPUPreSignedUrlsResponse.getPartNumToUrl();


        int concurrentPacketUploads = 3;
        List<Future<S3MPUCompletedPart>> packets = new ArrayList<>();
        List<S3MPUCompletedPart> completedParts = new ArrayList<>();
        int partNum = 1;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) chunkSize];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                Pair<Integer, byte[]> pair = new Pair<>(partNum++, Arrays.copyOf(buffer, len));

                // add to futures of uploads
                Future<S3MPUCompletedPart> s3MPUCompletedPartFuture = uploadPart(pair, partNumToUrl);
                packets.add(s3MPUCompletedPartFuture);

                if (packets.size() == concurrentPacketUploads) {
                    // wait for completion
                    completedParts.addAll(waitForCompletion(packets));
                    packets.clear();
                }
            }
            if (!packets.isEmpty()) {
                // wait for completion
                completedParts.addAll(waitForCompletion(packets));
                packets.clear();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (totalNumOfParts != partNum - 1 || completedParts.size() != totalNumOfParts) {
            throw new IllegalStateException(String.format("parts count is different, partNum:%s, totalParts:%s, completeParts:%s",
                    partNum - 1, totalNumOfParts, completedParts));
        }
        return completedParts;
    }

    private static List<S3MPUCompletedPart> waitForCompletion(List<Future<S3MPUCompletedPart>> packets) {
        List<S3MPUCompletedPart> completedParts = new ArrayList<>();
        for (Future<S3MPUCompletedPart> completedPartFuture : packets) {
            try {
                S3MPUCompletedPart completedPart = completedPartFuture.get();
                completedParts.add(completedPart);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return completedParts;
    }

    private static Future<S3MPUCompletedPart> uploadPart(Pair<Integer, byte[]> partNumAndData, Map<Integer, String> partNumToUrl) {
        return executorService.submit(
                new UploadPartCallable(
                        partNumAndData.getKey(),
                        partNumAndData.getValue(),
                        partNumToUrl.get(partNumAndData.getKey())));

    }

    @Data
    public static class UploadPartCallable implements Callable<S3MPUCompletedPart> {
        final int partNum;
        final byte[] data;
        final String url;

        @Override
        public S3MPUCompletedPart call() throws Exception {
            // TODO: add retries
            RestClient.HeaderAndEntity response = restClient.doPut(url, data);
            Optional<String> ETag = response.getHeaders().stream().filter(header -> header.getName().equals("ETag"))
                    .map(NameValuePair::getValue)
                    .findFirst();

            S3MPUCompletedPart completedPart = new S3MPUCompletedPart();
            completedPart.setPartNum(partNum);
            completedPart.setSize(data.length);
            completedPart.setETag(ETag.orElseThrow(() -> new IllegalStateException("no etag found from upload")));

            return completedPart;
        }
    }

    private static UploadInitRes initUpload(File file) {
        String URL_PREFIX = "localhost:8080";
        String URL_PATH_UPLOAD_INIT = "uploads/init";

        UploadInitReq uploadInitReq = new UploadInitReq();
        List<UploadFileInitReq> uploadFileInitReqs = new ArrayList<>();

        UploadFileInitReq uploadFileInitReq = new UploadFileInitReq();
        uploadFileInitReq.setFileName(file.getName());
        uploadFileInitReq.setTotalSize(file.length());

        uploadFileInitReqs.add(uploadFileInitReq);
        uploadInitReq.setUploadFileInitReqs(uploadFileInitReqs);

        UploadInitRes response = restClient.doPost(URL_PREFIX, URL_PATH_UPLOAD_INIT, serDe.toJson(uploadInitReq),
                UploadInitRes.class);
        return response;
    }
}
