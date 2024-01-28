package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.aws.models.MPUAbortedReason;
import com.enigma.audiobook.backend.aws.models.MPURequestStatus;
import com.enigma.audiobook.backend.aws.models.S3MPUPreSignedUrlsResponse;
import lombok.Data;

@Data
public class UploadFileInitRes {
    String uploadId;
    String objectKey;
    MPURequestStatus requestStatus;
    MPUAbortedReason abortedReason;
    S3MPUPreSignedUrlsResponse s3MPUPreSignedUrlsResponse;
}
