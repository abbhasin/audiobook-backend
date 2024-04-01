package com.enigma.audiobook.backend.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class MongoDateConverter extends JsonDeserializer<Date> {
    private static final String[] acceptedFormats = {"yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"};

    @Override
    public Date deserialize(JsonParser jp, DeserializationContext ctxt) {
        try {
            JsonNode node = jp.readValueAsTree();
            if (node.isTextual()) {
                return DateUtils.parseDate(node.asText(), acceptedFormats);
            }

            return DateUtils.parseDate(node.get("$date").asText(), acceptedFormats);
        } catch (Exception e) {
            log.error("unable to parse date", e);
            throw new RuntimeException(e);
        }
    }
}