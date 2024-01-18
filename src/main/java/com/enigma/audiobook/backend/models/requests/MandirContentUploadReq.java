package com.enigma.audiobook.backend.models.requests;

import com.enigma.audiobook.backend.models.ContentUploadStatus;
import lombok.Data;

@Data
public class MandirContentUploadReq {
    String mandirId;
    String imageUrl;
    ContentUploadStatus status;
}
