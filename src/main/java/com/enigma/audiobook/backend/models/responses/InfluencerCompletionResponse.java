package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.models.Influencer;
import lombok.Data;

@Data
public class InfluencerCompletionResponse {
    final Influencer influencer;
    final UploadCompletionRes uploadCompletionRes;
}
