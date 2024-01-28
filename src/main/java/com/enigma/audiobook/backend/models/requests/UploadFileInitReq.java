package com.enigma.audiobook.backend.models.requests;

import lombok.Data;

@Data
public class UploadFileInitReq {
    String fileName;
    long totalSize;
}
