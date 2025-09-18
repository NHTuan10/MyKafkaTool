package io.github.nhtuan10.mykafkatool.api.serdes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.kafka.common.header.Headers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class KafkaHeaderSerializer extends StdSerializer<Headers> {

    public KafkaHeaderSerializer() {
        this(null);
    }

    public KafkaHeaderSerializer(Class<Headers> t) {
        super(t);
    }

    @Override
    public void serialize(
            Headers value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jgen.writeStartArray();
        for (var header : value) {
            jgen.writeStartObject();
            jgen.writeStringField("key", header.key());
            jgen.writeStringField("value", new String(header.value(), StandardCharsets.UTF_8));
            jgen.writeEndObject();
        }
        jgen.writeEndArray();
    }
}