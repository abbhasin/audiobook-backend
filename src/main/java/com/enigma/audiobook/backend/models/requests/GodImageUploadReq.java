package com.enigma.audiobook.backend.models.requests;

import lombok.Data;

@Data
public class GodImageUploadReq {
    String godId;
    String imageUrl;
}
