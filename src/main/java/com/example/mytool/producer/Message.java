package com.example.mytool.producer;

public record Message(String key, String keyContentType, String value, String valueContentType, String schema
) {
    public Message(String key, String value, String valueContentType, String schema) {
        this(key, null, value, valueContentType, schema);
    }
}