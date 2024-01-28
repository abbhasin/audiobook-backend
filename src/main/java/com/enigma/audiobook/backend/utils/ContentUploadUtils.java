package com.enigma.audiobook.backend.utils;

import com.enigma.audiobook.backend.aws.S3UploadHandler;
import com.enigma.audiobook.backend.aws.models.MPURequestStatus;
import com.enigma.audiobook.backend.aws.models.S3MPUCompleteResponse;
import com.enigma.audiobook.backend.aws.models.S3MPUInitiationResponse;
import com.enigma.audiobook.backend.aws.models.S3MPUPreSignedUrlsResponse;
import com.enigma.audiobook.backend.models.requests.UploadCompletionReq;
import com.enigma.audiobook.backend.models.requests.UploadFileCompletionReq;
import com.enigma.audiobook.backend.models.requests.UploadFileInitReq;
import com.enigma.audiobook.backend.models.requests.UploadInitReq;
import com.enigma.audiobook.backend.models.responses.UploadCompletionRes;
import com.enigma.audiobook.backend.models.responses.UploadFileCompletionRes;
import com.enigma.audiobook.backend.models.responses.UploadFileInitRes;
import com.enigma.audiobook.backend.models.responses.UploadInitRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class ContentUploadUtils {
    @Autowired
    S3UploadHandler uploadHandler;

    static String bucket = "one-god-dev";
    static String bucket_url = "https://one-god-dev.s3.ap-south-1.amazonaws.com";
    static String key_format = "test-upload/ID-%s/%s";
    static long ONE_MB = 1024 * 1024;
    static long ONE_GB = ONE_MB * 1024;
    static long allowed_size = 500 * ONE_MB;
    static long chunk_size = 5 * ONE_MB;

    public String getObjectUrl(String objectKey) {
        return String.format("%s/%s", bucket_url, objectKey);
    }

    public UploadInitRes initUpload(UploadInitReq uploadInitReq, String keyFormat) {
        log.info("upload init request:" + uploadInitReq);
        UploadInitRes initRes = new UploadInitRes();
        initRes.setRequestStatus(MPURequestStatus.COMPLETED);

        // TODO: add total files size validation and number of files validation
        Map<String, UploadFileInitRes> fileNameToUploadFileResponse = new HashMap<>();

        for (UploadFileInitReq uploadFileInitReq : uploadInitReq.getUploadFileInitReqs()) {
            UploadFileInitRes fileInitRes = initFile(uploadFileInitReq, keyFormat);
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

    public UploadFileInitRes initFile(UploadFileInitReq uploadFileInitReq) {
        String objectKeyFormat = String.format(key_format, UUID.randomUUID().toString(), "%s");
        return initFile(uploadFileInitReq, objectKeyFormat);
    }

    public UploadFileInitRes initFile(UploadFileInitReq uploadFileInitReq, String keyFormat) {
        String fn = uploadFileInitReq.getFileName();
        String prefixFileName = fn.substring(0, fn.lastIndexOf("."));
        String suffixExtension = fn.substring(fn.lastIndexOf(".") + 1);

        String encodedFileNamePrefix = new String(Base64.getEncoder().encode(prefixFileName.getBytes(StandardCharsets.UTF_8)));
        String objectKey = String.format(keyFormat, encodedFileNamePrefix) + "." + suffixExtension;

        S3MPUInitiationResponse response =
                uploadHandler.initiateMultipartUploadRequest(bucket, objectKey, Optional.empty(),
                        uploadFileInitReq.getTotalSize(), allowed_size);
        log.info("initiation response:" + response);

        UploadFileInitRes uploadFileInitRes = new UploadFileInitRes();
        uploadFileInitRes.setObjectKey(objectKey);
        uploadFileInitRes.setUploadId(response.getUploadId());


        if (!response.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            uploadFileInitRes.setRequestStatus(response.getRequestStatus());
            uploadFileInitRes.setAbortedReason(response.getAbortedReason());

//            uploadHandler.abort(bucket, objectKey, response.getUploadId());
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

    public void abortAllCompletionReq(UploadCompletionReq uploadCompletionReq) {
        for (UploadFileCompletionReq uploadFileCompletionReq : uploadCompletionReq.getUploadFileCompletionReqs()) {
            uploadHandler.abort(bucket,
                    uploadFileCompletionReq.getObjectKey(),
                    uploadFileCompletionReq.getUploadId());
        }
    }

    public UploadCompletionRes completeUpload(UploadCompletionReq uploadCompletionReq) {
        UploadCompletionRes res = new UploadCompletionRes();
        res.setRequestStatus(MPURequestStatus.COMPLETED);

        Map<String, UploadFileCompletionRes> fileNameToUploadFileResponse = new HashMap<>();
        for (UploadFileCompletionReq uploadFileCompletionReq : uploadCompletionReq.getUploadFileCompletionReqs()) {
            UploadFileCompletionRes fileCompletionRes = completeUpload(uploadFileCompletionReq);
            fileNameToUploadFileResponse.put(uploadFileCompletionReq.getFileName(), fileCompletionRes);

            if (!fileCompletionRes.getS3MPUCompleteResponse().getState().equals(MPURequestStatus.COMPLETED)) {
                res.setRequestStatus(fileCompletionRes.getS3MPUCompleteResponse().getState());
                res.setAbortedReason(fileCompletionRes.getS3MPUCompleteResponse().getAbortedReason());

//                abortAllCompletionReq(uploadCompletionReq);
                break;
            }
        }

        res.setFileNameToUploadFileResponse(fileNameToUploadFileResponse);

        return res;
    }

    public UploadFileCompletionRes completeUpload(UploadFileCompletionReq uploadFileCompletionReq) {
        S3MPUCompleteResponse s3MPUCompleteResponse = uploadHandler.completeMultipartUploadRequest(bucket,
                uploadFileCompletionReq.getObjectKey(),
                uploadFileCompletionReq.getUploadId(),
                uploadFileCompletionReq.getS3MPUCompletedParts(),
                allowed_size);
        UploadFileCompletionRes res = new UploadFileCompletionRes();
        res.setS3MPUCompleteResponse(s3MPUCompleteResponse);
        return res;
    }
}
