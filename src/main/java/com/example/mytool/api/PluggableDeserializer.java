package com.example.mytool.api;

import java.util.Map;

public interface PluggableDeserializer {
    String getName();

    default String getDeserializerClass() {
        return "org.apache.kafka.common.serialization.ByteArrayDeserializer";
    }

    default boolean isCustomDeserializeMethodUsed() {
        return false;
    }

    default boolean isUserSchemaInputRequired() {
        return false;
    }

    default byte[] deserialize(String topic, Integer partition, byte[] payload, Map<String, byte[]> headerMap, Map<String, String> others) {
        return payload;
    }

}
