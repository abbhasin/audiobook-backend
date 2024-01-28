package com.enigma.audiobook.backend.models.requests;

import com.enigma.audiobook.backend.models.Influencer;
import lombok.Data;

@Data
public class InfluencerContentUploadReq {
    Influencer influencer;
    UploadCompletionReq uploadCompletionReq;
}
