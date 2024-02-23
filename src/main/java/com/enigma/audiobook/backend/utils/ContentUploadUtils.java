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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class ContentUploadUtils {
    @Autowired
    S3UploadHandler uploadHandler;

    static String bucket = "one-god-dev";
    public static String bucket_url = "https://one-god-dev.s3.ap-south-1.amazonaws.com";
    public static String key_format = "test-upload/ID-%s/%s";
    static long ONE_MB = 1024 * 1024;
    static long ONE_GB = ONE_MB * 1024;
    static long allowed_size = 500 * ONE_MB;
    static long chunk_size = 5 * ONE_MB;

    public String getObjectUrl(String objectKey) {
        return String.format("%s/%s", bucket_url, objectKey);
    }

    public UploadInitRes initUpload(UploadInitReq uploadInitReq, String keyFormat,
                                    ContentTypeByExtension contentTypeByExtension) {
        log.info("upload init request:" + uploadInitReq);
        UploadInitRes initRes = new UploadInitRes();
        initRes.setRequestStatus(MPURequestStatus.COMPLETED);

        // TODO: add total files size validation and number of files validation
        Map<String, UploadFileInitRes> fileNameToUploadFileResponse = new HashMap<>();

        for (UploadFileInitReq uploadFileInitReq : uploadInitReq.getUploadFileInitReqs()) {
            UploadFileInitRes fileInitRes = initFile(uploadFileInitReq, keyFormat,
                    contentTypeByExtension);
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
        return initFile(uploadFileInitReq, objectKeyFormat, ContentTypeByExtension.VIDEO);
    }

    public UploadFileInitRes initFile(UploadFileInitReq uploadFileInitReq, String keyFormat,
                                      ContentTypeByExtension contentTypeByExtension) {
        String fn = uploadFileInitReq.getFileName();
        boolean suffixExtensionExists = fn.lastIndexOf(".") != -1;

        String prefixFileName = fn;
        String suffixExtension = null;
        if (suffixExtensionExists) {
            prefixFileName = fn.substring(0, fn.lastIndexOf("."));
            suffixExtension = fn.substring(fn.lastIndexOf(".") + 1);
        }


        String encodedFileNamePrefix = new String(Base64.getEncoder().encode(prefixFileName.getBytes(StandardCharsets.UTF_8)));
        String objectKey = suffixExtensionExists ?
                String.format(keyFormat, encodedFileNamePrefix) + "." + suffixExtension :
                String.format(keyFormat, encodedFileNamePrefix);

        String contentType = getContentType(suffixExtension, contentTypeByExtension);
        S3MPUInitiationResponse response =
                uploadHandler.initiateMultipartUploadRequest(bucket, objectKey, Optional.of(contentType),
                        uploadFileInitReq.getTotalSize(), contentTypeByExtension.getAllowedContentSize());
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
                        chunk_size, contentTypeByExtension.getAllowedContentSize());

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

    private String getContentType(String suffixExtension, ContentTypeByExtension contentTypeByExtension) {
        Map<String, String> fileExtensionToS3ContentType = contentTypeByExtension.getFileExtensionToS3ContentType();
        if (StringUtils.isEmpty(suffixExtension)) {
            return fileExtensionToS3ContentType.get("*");
        }
        return Optional.ofNullable(fileExtensionToS3ContentType.get(suffixExtension.toLowerCase()))
                .orElse(fileExtensionToS3ContentType.get("*"));
    }

    public void abortAllCompletionReq(UploadCompletionReq uploadCompletionReq) {
        for (UploadFileCompletionReq uploadFileCompletionReq : uploadCompletionReq.getUploadFileCompletionReqs()) {
            uploadHandler.abort(bucket,
                    uploadFileCompletionReq.getObjectKey(),
                    uploadFileCompletionReq.getUploadId());
        }
    }

    public UploadCompletionRes completeUpload(UploadCompletionReq uploadCompletionReq,
                                              ContentTypeByExtension contentTypeByExtension) {
        UploadCompletionRes res = new UploadCompletionRes();
        res.setRequestStatus(MPURequestStatus.COMPLETED);

        Map<String, UploadFileCompletionRes> fileNameToUploadFileResponse = new HashMap<>();
        for (UploadFileCompletionReq uploadFileCompletionReq : uploadCompletionReq.getUploadFileCompletionReqs()) {
            UploadFileCompletionRes fileCompletionRes = completeUpload(uploadFileCompletionReq, contentTypeByExtension);
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

    public UploadFileCompletionRes completeUpload(UploadFileCompletionReq uploadFileCompletionReq,
                                                  ContentTypeByExtension contentTypeByExtension) {
        S3MPUCompleteResponse s3MPUCompleteResponse = uploadHandler.completeMultipartUploadRequest(bucket,
                uploadFileCompletionReq.getObjectKey(),
                uploadFileCompletionReq.getUploadId(),
                uploadFileCompletionReq.getS3MPUCompletedParts(),
                contentTypeByExtension.getAllowedContentSize());
        UploadFileCompletionRes res = new UploadFileCompletionRes();
        res.setS3MPUCompleteResponse(s3MPUCompleteResponse);
        return res;
    }

    @Getter
    public enum ContentTypeByExtension {
        VIDEO(Map.ofEntries(Map.entry("mp4", "video/mp4"), Map.entry("*", "video/mpeg")),
                2 * ONE_GB),
        AUDIO(Map.ofEntries(Map.entry("mp3", "audio/mpeg"), Map.entry("*", "audio/mpeg")),
                50 * ONE_MB),
        IMAGE(Map.ofEntries(Map.entry("jpeg", "image/jpeg"),
                Map.entry("jpg", "image/jpeg"),
                Map.entry("png", "image/png"),
                Map.entry("*", "image/jpeg")),
                5 * ONE_MB);

        private final Map<String, String> fileExtensionToS3ContentType;
        private final long allowedContentSize;

        ContentTypeByExtension(Map<String, String> fileExtensionToS3ContentType, long allowedContentSize) {
            this.fileExtensionToS3ContentType = fileExtensionToS3ContentType;
            this.allowedContentSize = allowedContentSize;
        }

    }
}
