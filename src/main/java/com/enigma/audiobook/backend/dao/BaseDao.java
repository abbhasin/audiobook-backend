package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.utils.SerDe;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class BaseDao {
    static final SerDe serde = new SerDe();

    Date getCurrentTime() {
        LocalDateTime ldt = LocalDateTime.now(ZoneId.of(ZoneId.SHORT_IDS.get("IST")));
        return Date.from(ldt.atZone(ZoneId.of(ZoneId.SHORT_IDS.get("IST"))).toInstant());
    }
}
