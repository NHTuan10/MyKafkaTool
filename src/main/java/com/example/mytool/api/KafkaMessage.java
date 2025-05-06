package com.example.mytool.api;

import com.example.mytool.serde.SerdeUtil;

import java.util.Map;

public record KafkaMessage(String key, String keyContentType, String value, String valueContentType, String schema,
                           Map<String, String> headers) {
    public KafkaMessage(String key, String value, String valueContentType, String schema, Map<String, String> headers) {
        this(key, SerdeUtil.SERDE_STRING, value, valueContentType, schema, headers);
    }
}