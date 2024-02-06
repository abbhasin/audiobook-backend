package com.enigma.audiobook.backend.models.requests;

import com.enigma.audiobook.backend.aws.models.S3MPUPreSignedUrlsResponse;
import lombok.Data;

@Data
public class UploadPartsRequest {
    S3MPUPreSignedUrlsResponse s3MPUPreSignedUrlsResponse;
    String filePath;
}
