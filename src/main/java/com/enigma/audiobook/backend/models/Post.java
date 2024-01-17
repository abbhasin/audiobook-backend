package com.enigma.audiobook.backend.models;

import com.enigma.audiobook.backend.utils.MongoDateConverter;
import com.enigma.audiobook.backend.utils.ObjectIdDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class Post {
    @JsonProperty("_id")
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    String postId;
    @JsonDeserialize(using = MongoDateConverter.class)
    Date createTime;
    @JsonDeserialize(using = MongoDateConverter.class)
    Date updateTime;
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    String associatedMandirId;
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    String associatedInfluencerId;
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    String associatedGodId;
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    String fromUserId;
    PostTag tag;
    PostAssociationType associationType;
    PostType type;
    String title;
    String description;
    String thumbnailUrl;
    String videoUrl;
    List<String> imagesUrl;
    String audioUrl;
    ContentUploadStatus contentUploadStatus;
    Quality quality;


}
