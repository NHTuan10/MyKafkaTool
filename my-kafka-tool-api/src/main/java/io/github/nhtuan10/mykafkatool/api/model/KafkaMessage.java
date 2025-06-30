package io.github.nhtuan10.mykafkatool.api.model;

import java.io.Serializable;
import java.util.Map;

public record KafkaMessage(String key, String keyContentType, String value, String valueContentType, String schema,
                           Map<String, byte[]> headers) implements Serializable {
    public static final String SERDES_STRING = "String";

    public KafkaMessage(String key, String value, String valueContentType, String schema, Map<String, byte[]> headers) {
        this(key, SERDES_STRING, value, valueContentType, schema, headers);
    }
}