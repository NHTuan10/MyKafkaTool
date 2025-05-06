package com.example.mytool.serde.deserializer;

import com.example.mytool.api.PluggableDeserializer;

public class StringDeserializer implements PluggableDeserializer {
    @Override
    public String getName() {
        return "String";
    }

    @Override
    public String getDeserializerClass() {
        return "org.apache.kafka.common.serialization.StringDeserializer";
    }
}
