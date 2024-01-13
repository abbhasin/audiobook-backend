package com.enigma.audiobook.backend.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class God {
    @JsonProperty("_id")
    String godId;
    String godName;
    String imageUrl;
    String description;
}
