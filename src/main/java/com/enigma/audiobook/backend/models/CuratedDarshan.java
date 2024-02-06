package com.enigma.audiobook.backend.models;

import com.enigma.audiobook.backend.utils.DateSerializer;
import com.enigma.audiobook.backend.utils.MongoDateConverter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class CuratedDarshan {
    String curatedDarshanId;
    Map<String, List<String>> godToDarshanIds;
    @JsonDeserialize(using = MongoDateConverter.class)
    @JsonSerialize(using = DateSerializer.class)
    Date createTime;
    @JsonDeserialize(using = MongoDateConverter.class)
    @JsonSerialize(using = DateSerializer.class)
    Date updateTime;
}
