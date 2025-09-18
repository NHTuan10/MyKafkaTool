package io.github.nhtuan10.mykafkatool.api.model;

import org.apache.kafka.common.header.Headers;

import java.io.Serializable;

public record KafkaMessage(String key, String keyContentType, String value, String valueContentType, String schema,
                           Headers headers) implements Serializable {
    public static final String SERDES_STRING = "String";

    public KafkaMessage(String key, String value, String valueContentType, String schema, Headers headers) {
        this(key, SERDES_STRING, value, valueContentType, schema, headers);
    }
}