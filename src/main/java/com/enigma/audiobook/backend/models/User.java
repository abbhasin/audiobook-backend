package com.enigma.audiobook.backend.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class User {
    @JsonProperty("_id")
    String userId;
    String authUserId;
}
