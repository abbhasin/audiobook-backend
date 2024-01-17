package com.enigma.audiobook.backend.utils;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.bson.types.ObjectId;

import java.io.IOException;

public class ObjectIdDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException, JacksonException {
//        ObjectId objectId = jp.readValueAs(ObjectId.class);
//        return objectId.toString();
        JsonNode oid = ((JsonNode)jp.readValueAsTree()).get("$oid");
        return oid.asText();
    }
}
