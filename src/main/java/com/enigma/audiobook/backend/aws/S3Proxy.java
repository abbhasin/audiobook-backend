package com.enigma.audiobook.backend.aws;

import com.enigma.audiobook.backend.aws.models.S3MPUCompletedPart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class S3Proxy {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3Proxy() {
        this.s3Client = S3Client.builder().region(Region.AP_SOUTH_1)
                .build();
        this.s3Presigner = S3Presigner.builder().region(Region.AP_SOUTH_1).build();
    }


    public String generatePreSignedS3UrlForPut(String bucket, String objectKey,
                                               Optional<String> contentType) {
        PutObjectRequest.Builder putObjectReqBuilder = PutObjectRequest.builder();
        putObjectReqBuilder
                .bucket(bucket)
                .key(objectKey);
        contentType.ifPresent(putObjectReqBuilder::contentType);
        PutObjectRequest putObjectRequest = putObjectReqBuilder.build();

        PutObjectPresignRequest putObjectPresignRequest =
                PutObjectPresignRequest
                        .builder()
                        .putObjectRequest(putObjectRequest)
                        .signatureDuration(Duration.ofMinutes(150))
                        .build();

        PresignedPutObjectRequest presignedPutObjectRequest =
                s3Presigner.presignPutObject(putObjectPresignRequest);

        URL url = presignedPutObjectRequest.url();
        log.info("presigned s3 url put request, bucket:{}, key:{}, url:{}",
                bucket, objectKey, url);
        return url.toString();
    }

    public String initiateMultipartUploadRequest(String bucket, String objectKey,
                                                 Optional<String> contentType) {
        CreateMultipartUploadRequest.Builder createMultipartUploadRequestBuilder
                = CreateMultipartUploadRequest
                .builder()
                .bucket(bucket)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .key(objectKey);
        contentType.ifPresent(createMultipartUploadRequestBuilder::contentType);

        CreateMultipartUploadResponse response =
                s3Client.createMultipartUpload(createMultipartUploadRequestBuilder.build());
        String uploadId = response.uploadId();
        log.info("multi part upload create:" + response);
        log.info("multipart upload s3 created, bucket:{}, key:{}, uploadId:{}",
                bucket, objectKey, uploadId);
        return uploadId;
    }

    public void completeMultipartUploadRequest(String bucket, String objectKey,
                                               String uploadId, List<S3MPUCompletedPart> s3MPUCompletedParts) {
        List<CompletedPart> parts =
                s3MPUCompletedParts.stream()
                        .map(cp -> CompletedPart.builder()
                                .partNumber(cp.getPartNum())
                                .eTag(cp.getETag())
                                .build())
                        .toList();

        CompletedMultipartUpload multipartUpload =
                CompletedMultipartUpload
                        .builder()
                        .parts(parts)
                        .build();
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                CompleteMultipartUploadRequest
                        .builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .uploadId(uploadId)
                        .multipartUpload(multipartUpload)
                        .build();
        CompleteMultipartUploadResponse response =
                s3Client.completeMultipartUpload(completeMultipartUploadRequest);
        log.info("completed multi part upload, bucket:{}, key:{}, uploadId:{}",
                response.bucket(), response.key(), uploadId);
    }

    public void abortMultipartUploadRequest(String bucket, String objectKey,
                                            String uploadId) {
        AbortMultipartUploadRequest abortMultipartUploadRequest
                = AbortMultipartUploadRequest
                .builder()
                .bucket(bucket)
                .key(objectKey)
                .uploadId(uploadId)
                .build();
        AbortMultipartUploadResponse response = s3Client.abortMultipartUpload(abortMultipartUploadRequest);
        log.info("aborted multi part upload, bucket:{}, key:{}, uploadId:{}",
                bucket, objectKey, uploadId);
    }

    public String preSignMultipartUploadPartsRequests(String bucket, String objectKey,
                                                      String uploadId, int partNum,
                                                      long contentLength) {
        UploadPartRequest uploadPartRequest =
                UploadPartRequest
                        .builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .uploadId(uploadId)
                        .partNumber(partNum)
                        .contentLength(contentLength)
                        .build();

        UploadPartPresignRequest upPresignReq =
                UploadPartPresignRequest
                        .builder()
                        .signatureDuration(Duration.ofMinutes(150))
                        .uploadPartRequest(uploadPartRequest)
                        .build();
        PresignedUploadPartRequest presignedUploadPartRequest =
                s3Presigner.presignUploadPart(upPresignReq);

        URL url = presignedUploadPartRequest.url();
        log.info("presigned s3 url upload part request, bucket:{}, key:{}, url:{}",
                bucket, objectKey, url);
        return url.toString();
    }

    public List<S3MPUCompletedPart> listMPUParts(String bucket, String objectKey,
                                                 String uploadId) {
        ListPartsRequest listPartsRequest = ListPartsRequest
                .builder()
                .bucket(bucket)
                .key(objectKey)
                .uploadId(uploadId)
                .build();
        ListPartsResponse listPartsResponse = s3Client.listParts(listPartsRequest);
        return listPartsResponse.parts()
                .stream()
                .map(part -> {
                    S3MPUCompletedPart cp = new S3MPUCompletedPart();
                    cp.setETag(part.eTag());
                    cp.setPartNum(part.partNumber());
                    cp.setSize(part.size());
                    return cp;
                }).toList();
    }

    public void putObject(String bucket, String objectKey, File file, Optional<String> contentType) {
        PutObjectRequest.Builder putObjectRequest =
                PutObjectRequest.builder()
                        .acl(ObjectCannedACL.PUBLIC_READ)
                        .bucket(bucket)
                        .key(objectKey);

        contentType.ifPresent(putObjectRequest::contentType);

        PutObjectResponse response = s3Client.putObject(putObjectRequest.build(), Path.of(file.toURI()));
        log.info("added object to bucket:{}, key:{}, eTag:{}", bucket, objectKey, response.eTag());
    }

    public void getObject(String s3ObjectUri, URI fileOutputLocation) {
        URI uri = URI.create(s3ObjectUri);
        String objectKey = uri.getPath().substring(1); // remove the prefix '/'

        int firstDotIndex = uri.getAuthority().indexOf(".");
        String bucket = uri.getAuthority();
        if (firstDotIndex != -1) {
            bucket = uri.getAuthority().substring(0, firstDotIndex);
        }

        GetObjectRequest getObjectRequest =
                GetObjectRequest
                        .builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .build();
        GetObjectResponse response = s3Client.getObject(getObjectRequest, Path.of(fileOutputLocation));
    }

    public void deleteObject(String bucket, String objectKey) {
        DeleteObjectRequest deleteObjectRequest =
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .build();
        DeleteObjectResponse response = s3Client.deleteObject(deleteObjectRequest);
    }

    public void deleteObject(String s3ObjectUri) {
        URI uri = URI.create(s3ObjectUri);
        String objectKey = uri.getPath().substring(1); // remove the prefix '/'

        int firstDotIndex = uri.getAuthority().indexOf(".");
        String bucket = uri.getAuthority();
        if (firstDotIndex != -1) {
            bucket = uri.getAuthority().substring(0, firstDotIndex);
        }

        DeleteObjectRequest deleteObjectRequest =
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .build();
        DeleteObjectResponse response = s3Client.deleteObject(deleteObjectRequest);
    }

    public void markForExpiration(String s3ObjectUri) {
        URI uri = URI.create(s3ObjectUri);
        String objectKey = uri.getPath().substring(1); // remove the prefix '/'

        int firstDotIndex = uri.getAuthority().indexOf(".");
        String bucket = uri.getAuthority();
        if (firstDotIndex != -1) {
            bucket = uri.getAuthority().substring(0, firstDotIndex);
        }

        Tag tag = Tag.builder().key("lifecycle-policy-category").value("marked-for-deletion").build();
        Tagging tagging = Tagging.builder().tagSet(tag).build();
        PutObjectTaggingRequest objectTaggingRequest =
                PutObjectTaggingRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .tagging(tagging)
                        .build();

        s3Client.putObjectTagging(objectTaggingRequest);
    }

}
