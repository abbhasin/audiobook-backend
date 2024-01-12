package com.enigma.audiobook.backend.dao;

import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class BaseDao {
    static final Gson gson = new Gson();

    LocalDateTime getCurrentTime() {
        return LocalDateTime.now(ZoneId.of(ZoneId.SHORT_IDS.get("IST")));
    }
}
