package com.enigma.audiobook.backend.models;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class CuratedDarshan {
    String curatedDarshanId;
    Map<String, List<String>> godToDarshanIds;
    Date createTime;
    Date updateTime;
}
