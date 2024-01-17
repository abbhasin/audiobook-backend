package com.enigma.audiobook.backend.models.requests;

import lombok.Data;

@Data
public class InfluencerImageUploadReq {
    String userId;
    String imageUrl;
}
