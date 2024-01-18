package com.enigma.audiobook.backend.models.requests;

import com.enigma.audiobook.backend.models.ContentUploadStatus;
import lombok.Data;

@Data
public class DarshanContentUploadReq {
    String darshanId;
    String thumbnailUrl;
    String videoUrl;
    ContentUploadStatus status;
}
