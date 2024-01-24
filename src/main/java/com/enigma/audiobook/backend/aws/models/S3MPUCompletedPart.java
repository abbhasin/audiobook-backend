package com.enigma.audiobook.backend.aws.models;

import lombok.Data;

@Data
public class S3MPUCompletedPart {
    String eTag;
    int partNum;
    long size; // in bytes
}
