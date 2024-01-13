package com.enigma.audiobook.backend.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Mandir {
    @JsonProperty("_id")
    String mandirId;
    String name;
    String imageUrl;
    String description;
    Address address;
    Quality quality;

}
