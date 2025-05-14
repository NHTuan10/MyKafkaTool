package com.example.mytool.exception;

public class DeserializationException extends Exception {

    private Object data;

    public DeserializationException(String message, Object data) {
        super(message);
        this.data = data;
    }

    public DeserializationException(String message, Object data, Throwable e) {
        super(message, e);
        this.data = data;
    }
}
