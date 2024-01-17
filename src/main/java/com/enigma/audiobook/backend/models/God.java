package com.enigma.audiobook.backend.models;

import com.enigma.audiobook.backend.utils.ObjectIdDeserializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class God {
    @JsonProperty("_id")
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    String godId;
    String godName;
    String imageUrl;
    String description;
    ContentUploadStatus contentUploadStatus;
}
