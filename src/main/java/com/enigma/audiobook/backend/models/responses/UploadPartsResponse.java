package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.aws.models.S3MPUCompletedPart;
import lombok.Data;

import java.util.List;

@Data
public class UploadPartsResponse {
    List<S3MPUCompletedPart> s3MPUCompletedParts;
}
