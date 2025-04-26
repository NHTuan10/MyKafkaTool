package com.example.mytool.producer;

public record Message(String key, String keyContentType, String value, String valueContentType, String schema
) {
}