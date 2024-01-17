package com.enigma.audiobook.backend.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MongoDateConverter extends JsonDeserializer<Date> {
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Override
    public Date deserialize(JsonParser jp, DeserializationContext ctxt) {

        try {
            JsonNode node = jp.readValueAsTree();
            return formatter.parse(node.get("$date").asText());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}