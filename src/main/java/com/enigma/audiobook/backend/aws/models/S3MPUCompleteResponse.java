package com.enigma.audiobook.backend.aws.models;

import lombok.Data;

@Data
public class S3MPUCompleteResponse {
    MPURequestStatus state;
    MPUAbortedReason abortedReason;
}
