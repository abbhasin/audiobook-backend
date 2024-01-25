package com.enigma.audiobook.backend.aws;

import com.enigma.audiobook.backend.aws.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
@Component
public class S3UploadHandler {

    private final S3Proxy s3Proxy;

    public S3UploadHandler(S3Proxy s3Proxy) {
        this.s3Proxy = s3Proxy;
    }

    public S3MPUInitiationResponse initiateMultipartUploadRequest(String bucket,
                                                                  String objectKey,
                                                                  Optional<String> contentType,
                                                                  long totalSize,
                                                                  long allowedTotalSize) {
        if (totalSize > allowedTotalSize) {
            S3MPUInitiationResponse response = new S3MPUInitiationResponse();
            response.setRequestStatus(MPURequestStatus.ABORTED);
            response.setAbortedReason(MPUAbortedReason.TOTAL_SIZE_TOO_LARGE);
            return response;
        }
        String uploadId = s3Proxy.initiateMultipartUploadRequest(bucket, objectKey, contentType);
        S3MPUInitiationResponse response = new S3MPUInitiationResponse();
        response.setUploadId(uploadId);
        response.setRequestStatus(MPURequestStatus.COMPLETED);
        return response;
    }

    public S3MPUCompleteResponse completeMultipartUploadRequest(String bucket, String objectKey,
                                                                String uploadId,
                                                                List<S3MPUCompletedPart> s3MPUCompletedParts,
                                                                long allowedTotalSize) {
        S3MPUCompleteResponse response = new S3MPUCompleteResponse();

        List<S3MPUCompletedPart> s3MPUCompletedPartsSrc
                = s3Proxy.listMPUParts(bucket, objectKey, uploadId);

        List<S3MPUCompletedPart> s3MPUCompletedPartsSrcSorted =
                s3MPUCompletedPartsSrc.stream().sorted((cp1, cp2) -> cp1.getPartNum() - cp2.getPartNum()).toList();
        List<S3MPUCompletedPart> s3MPUCompletedPartsProvidedSorted =
                s3MPUCompletedParts.stream().sorted((cp1, cp2) -> cp1.getPartNum() - cp2.getPartNum()).toList();

        if (!s3MPUCompletedPartsSrcSorted.equals(s3MPUCompletedPartsProvidedSorted)) {
            s3Proxy.abortMultipartUploadRequest(bucket, objectKey, uploadId);
            response.setState(MPURequestStatus.ABORTED);
            response.setAbortedReason(MPUAbortedReason.PARTS_NOT_MATCHING);
            return response;
        }

        long totalSize = s3MPUCompletedPartsSrcSorted
                .stream().mapToLong(S3MPUCompletedPart::getSize).sum();

        if (totalSize > allowedTotalSize) {
            s3Proxy.abortMultipartUploadRequest(bucket, objectKey, uploadId);
            response.setState(MPURequestStatus.ABORTED);
            response.setAbortedReason(MPUAbortedReason.TOTAL_SIZE_TOO_LARGE);
            return response;
        }

        s3Proxy.completeMultipartUploadRequest(bucket, objectKey, uploadId, s3MPUCompletedPartsProvidedSorted);

        response.setState(MPURequestStatus.COMPLETED);
        response.setAbortedReason(null);
        return response;
    }

    public S3MPUPreSignedUrlsResponse generatePreSignedS3Urls(String bucket,
                                                              String objectKey,
                                                              String uploadId,
                                                              long totalSize,
                                                              long chunkSize,
                                                              long allowedTotalSize) {
        if (totalSize > allowedTotalSize) {
            S3MPUPreSignedUrlsResponse response = new S3MPUPreSignedUrlsResponse();
            response.setRequestStatus(MPURequestStatus.ABORTED);
            response.setAbortedReason(MPUAbortedReason.TOTAL_SIZE_TOO_LARGE);
            return response;
        }

        AtomicLong numOfChunks = new AtomicLong(totalSize / chunkSize);
        long lastChunkSize = totalSize % chunkSize;
        if (lastChunkSize > 0) {
            // last chunk
            numOfChunks.incrementAndGet();
        }

        Map<Integer, String> partNumToUrl =
                IntStream.range(1, (int) numOfChunks.get())
                        .mapToObj(partNum -> {
                                    long contentLength = (partNum != numOfChunks.get()) ? chunkSize : lastChunkSize;
                                    String url =
                                            s3Proxy.preSignMultipartUploadPartsRequests(bucket, objectKey, uploadId,
                                                    partNum, contentLength);
                                    return new Pair<>(partNum, url);
                                }
                        ).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        S3MPUPreSignedUrlsResponse response = new S3MPUPreSignedUrlsResponse();
        response.setChunkSize(chunkSize);
        response.setTotalNumOfParts(numOfChunks.get());
        response.setPartNumToUrl(partNumToUrl);

        response.setRequestStatus(MPURequestStatus.COMPLETED);
        response.setAbortedReason(null);

        return response;
    }

    public void abort(String bucket,
                      String objectKey,
                      String uploadId) {
        s3Proxy.abortMultipartUploadRequest(bucket, objectKey, uploadId);
    }
}
