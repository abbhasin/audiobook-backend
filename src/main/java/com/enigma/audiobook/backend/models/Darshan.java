package com.enigma.audiobook.backend.models;

import com.enigma.audiobook.backend.utils.DateSerializer;
import com.enigma.audiobook.backend.utils.MongoDateConverter;
import com.enigma.audiobook.backend.utils.ObjectIdDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class Darshan {
    @JsonProperty("_id")
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    String darshanId;
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    String mandirId;
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    String godId;
    @JsonDeserialize(using = MongoDateConverter.class)
    @JsonSerialize(using = DateSerializer.class)
    Date createTime;
    @JsonDeserialize(using = MongoDateConverter.class)
    @JsonSerialize(using = DateSerializer.class)
    Date updateTime;
    String shortDescription;
    String thumbnailUrl;
    String videoUrl;
    ContentUploadStatus videoUploadStatus;
    Quality quality;

}
