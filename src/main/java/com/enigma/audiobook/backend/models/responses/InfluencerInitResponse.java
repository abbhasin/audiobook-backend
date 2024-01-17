package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.models.Influencer;
import lombok.Data;

@Data
public class InfluencerInitResponse {
    final String imageUrlToUpload;
    final Influencer influencer;
}
