package com.enigma.audiobook.backend.aws.models;

import lombok.Data;

import java.util.Map;

@Data
public class S3MPUPreSignedUrlsResponse {
    long chunkSize;
    long totalNumOfParts;
    Map<Integer, String> partNumToUrl;
    MPURequestStatus requestStatus;
    MPUAbortedReason abortedReason;
}
