package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.aws.models.S3MPUCompleteResponse;
import lombok.Data;

@Data
public class UploadFileCompletionRes {
    S3MPUCompleteResponse s3MPUCompleteResponse;
}
