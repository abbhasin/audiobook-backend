package com.enigma.audiobook.backend.models;

import com.enigma.audiobook.backend.utils.MongoDateConverter;
import com.enigma.audiobook.backend.utils.ObjectIdDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.Date;

@Data
public class View {
    @JsonProperty("_id")
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    String id;
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    String postId;
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    String userId;
    Integer viewDurationSec;
    Integer totalLengthSec;
    @JsonDeserialize(using = MongoDateConverter.class)
    Date createTime;
    @JsonDeserialize(using = MongoDateConverter.class)
    Date updateTime;
}
