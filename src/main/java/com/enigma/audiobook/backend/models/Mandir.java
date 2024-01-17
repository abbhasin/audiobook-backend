package com.enigma.audiobook.backend.models;

import com.enigma.audiobook.backend.utils.ObjectIdDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@Data
public class Mandir {
    @JsonProperty("_id")
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    String mandirId;
    String name;
    String imageUrl;
    String description;
    Address address;
    Quality quality;

}
