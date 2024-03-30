package com.enigma.audiobook.backend.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class MongoDateConverter extends JsonDeserializer<Date> {

    @Override
    public Date deserialize(JsonParser jp, DeserializationContext ctxt) {
        // formatter is not thread safe, make local: https://medium.com/@daveford/numberformatexception-multiple-points-when-parsing-date-650baa6829b6
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        try {
            JsonNode node = jp.readValueAsTree();
            if (node.isTextual()) {
                return formatter.parse(node.asText());
            }

            return  formatter.parse(node.get("$date").asText());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}