package com.enigma.audiobook.backend.aws.models;

import lombok.Data;

@Data
public class S3MPUInitiationResponse {
    String uploadId;
    MPURequestStatus requestStatus;
    MPUAbortedReason abortedReason;
}
