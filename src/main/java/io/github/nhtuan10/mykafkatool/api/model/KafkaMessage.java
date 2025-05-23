package io.github.nhtuan10.mykafkatool.api.model;

import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;

import java.util.Map;

public record KafkaMessage(String key, String keyContentType, String value, String valueContentType, String schema,
                           Map<String, byte[]> headers) {
    public KafkaMessage(String key, String value, String valueContentType, String schema, Map<String, byte[]> headers) {
        this(key, SerDesHelper.SERDE_STRING, value, valueContentType, schema, headers);
    }
}