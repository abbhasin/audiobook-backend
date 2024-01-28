package com.enigma.audiobook.backend.models.requests;

import com.enigma.audiobook.backend.aws.models.S3MPUCompletedPart;
import lombok.Data;

import java.util.List;

@Data
public class UploadFileCompletionReq {
    String fileName;
    String uploadId;
    String objectKey;
    List<S3MPUCompletedPart> s3MPUCompletedParts;
}
