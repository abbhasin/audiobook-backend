package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.aws.S3UploadHandler;
import com.enigma.audiobook.backend.aws.models.*;
import com.enigma.audiobook.backend.models.Darshan;
import com.enigma.audiobook.backend.models.responses.DarshanInitResponse;
import lombok.Data;
import org.apache.commons.math3.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class UploadController {

    @Autowired
    S3UploadHandler uploadHandler;

    static String bucket = "one-god-dev";
    static String key_format = "test-upload/ID-%s/%s";
    static long ONE_MB = 1024 * 1024;
    static long ONE_GB = ONE_MB * 1024;
    static long allowed_size = 100 * ONE_MB;
    static long chunk_size = 50 * ONE_GB;


    @PostMapping("/uploads/init")
    @ResponseBody
    public UploadInitRes initUpload(@RequestBody UploadInitReq uploadInitReq) {
        UploadInitRes initRes = new UploadInitRes();
        initRes.setRequestStatus(MPURequestStatus.COMPLETED);

        // TODO: add total files size validation and number of files validation
        Map<String, UploadFileInitRes> fileNameToUploadFileResponse = new HashMap<>();

        for (UploadFileInitReq uploadFileInitReq : uploadInitReq.getUploadFileInitReqs()) {
            UploadFileInitRes fileInitRes = initFile(uploadFileInitReq);
            fileNameToUploadFileResponse.put(uploadFileInitReq.getFileName(), fileInitRes);

            if (!fileInitRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
                initRes.setRequestStatus(fileInitRes.getRequestStatus());
                initRes.setAbortedReason(fileInitRes.getAbortedReason());
                break;
            }
        }

        initRes.setFileNameToUploadFileResponse(fileNameToUploadFileResponse);

        return initRes;
    }

    private UploadFileInitRes initFile(UploadFileInitReq uploadFileInitReq) {
        String fn = uploadFileInitReq.getFileName();
        String prefixFileName = fn.substring(0, fn.lastIndexOf("."));
        String suffixExtension = fn.substring(fn.lastIndexOf(".") + 1);

        String encodedFileNamePrefix = new String(Base64.getEncoder().encode(prefixFileName.getBytes(StandardCharsets.UTF_8)));
        String objectKey = String.format(key_format, UUID.randomUUID().toString(),
                encodedFileNamePrefix)
                + "." + suffixExtension;

        S3MPUInitiationResponse response =
                uploadHandler.initiateMultipartUploadRequest(bucket, objectKey, Optional.empty(),
                        uploadFileInitReq.getTotalSize(), allowed_size);

        UploadFileInitRes uploadFileInitRes = new UploadFileInitRes();
        uploadFileInitRes.setObjectKey(objectKey);
        uploadFileInitRes.setUploadId(response.getUploadId());


        if (!response.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            uploadFileInitRes.setRequestStatus(response.getRequestStatus());
            uploadFileInitRes.setAbortedReason(response.getAbortedReason());

            uploadHandler.abort(bucket, objectKey, response.getUploadId());
            return uploadFileInitRes;
        }

        S3MPUPreSignedUrlsResponse preSignedUrlsResponse =
                uploadHandler.generatePreSignedS3Urls(bucket, objectKey, response.getUploadId(),
                        uploadFileInitReq.getTotalSize(),
                        chunk_size, allowed_size);

        uploadFileInitRes.setS3MPUPreSignedUrlsResponse(preSignedUrlsResponse);

        if (!preSignedUrlsResponse.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            uploadFileInitRes.setRequestStatus(preSignedUrlsResponse.getRequestStatus());
            uploadFileInitRes.setAbortedReason(preSignedUrlsResponse.getAbortedReason());

            uploadHandler.abort(bucket, objectKey, response.getUploadId());
            return uploadFileInitRes;
        }

        uploadFileInitRes.setRequestStatus(MPURequestStatus.COMPLETED);
        return uploadFileInitRes;
    }

    @PostMapping("/uploads/completion")
    @ResponseBody
    public UploadCompletionRes completeUpload(@RequestBody UploadCompletionReq uploadCompletionReq) {
        UploadCompletionRes res = new UploadCompletionRes();

        Map<String, UploadFileCompletionRes> fileNameToUploadFileResponse = new HashMap<>();
        for (UploadFileCompletionReq uploadFileCompletionReq : uploadCompletionReq.getUploadFileCompletionReqs()) {
            UploadFileCompletionRes fileCompletionRes = completeUpload(uploadFileCompletionReq);
            fileNameToUploadFileResponse.put(uploadFileCompletionReq.getFileName(), fileCompletionRes);

            if (!fileCompletionRes.getS3MPUCompleteResponse().getState().equals(MPURequestStatus.COMPLETED)) {
                res.setRequestStatus(fileCompletionRes.getS3MPUCompleteResponse().getState());
                res.setAbortedReason(fileCompletionRes.getS3MPUCompleteResponse().getAbortedReason());
                break;
            }
        }

        res.setFileNameToUploadFileResponse(fileNameToUploadFileResponse);

        return res;
    }

    private UploadFileCompletionRes completeUpload(UploadFileCompletionReq uploadFileCompletionReq) {
        S3MPUCompleteResponse s3MPUCompleteResponse = uploadHandler.completeMultipartUploadRequest(bucket,
                uploadFileCompletionReq.getObjectKey(),
                uploadFileCompletionReq.getUploadId(),
                uploadFileCompletionReq.getS3MPUCompletedParts(),
                allowed_size);
        UploadFileCompletionRes res = new UploadFileCompletionRes();
        res.setS3MPUCompleteResponse(s3MPUCompleteResponse);
        return res;
    }

    @Data
    public static class UploadInitReq {
        List<UploadFileInitReq> uploadFileInitReqs;

    }

    @Data
    public static class UploadFileInitReq {
        String fileName;
        long totalSize;
    }

    @Data
    public static class UploadInitRes {
        Map<String, UploadFileInitRes> fileNameToUploadFileResponse;
        MPURequestStatus requestStatus;
        MPUAbortedReason abortedReason;
    }

    @Data
    public static class UploadFileInitRes {
        String uploadId;
        String objectKey;
        MPURequestStatus requestStatus;
        MPUAbortedReason abortedReason;
        S3MPUPreSignedUrlsResponse s3MPUPreSignedUrlsResponse;
    }

    @Data
    public static class UploadCompletionReq {
        List<UploadFileCompletionReq> uploadFileCompletionReqs;
    }

    @Data
    public static class UploadFileCompletionReq {
        String fileName;
        String uploadId;
        String objectKey;
        List<S3MPUCompletedPart> s3MPUCompletedParts;
    }

    @Data
    public static class UploadCompletionRes {
        Map<String, UploadFileCompletionRes> fileNameToUploadFileResponse;
        MPURequestStatus requestStatus;
        MPUAbortedReason abortedReason;
    }

    @Data
    public static class UploadFileCompletionRes {
        S3MPUCompleteResponse s3MPUCompleteResponse;
    }
}
