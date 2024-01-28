package com.enigma.audiobook.backend.models.requests;

import com.enigma.audiobook.backend.models.God;
import lombok.Data;

@Data
public class GodContentUploadReq {
    God god;
    UploadCompletionReq uploadCompletionReq;
}
