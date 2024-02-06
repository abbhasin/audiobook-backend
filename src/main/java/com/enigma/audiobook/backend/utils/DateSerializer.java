package com.enigma.audiobook.backend.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateSerializer extends JsonSerializer<Date> {
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    @Override
    public void serialize(Date date, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        if (date == null) {
            gen.writeNull();
        } else {
            gen.writeString(formatter.format(date.getTime()));
        }
    }
}
