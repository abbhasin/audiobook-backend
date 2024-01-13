package com.enigma.audiobook.backend.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Influencer {
    @JsonProperty("_id")
    String recordId;
    String userId;
    String imageUrl;
    String description;
}
