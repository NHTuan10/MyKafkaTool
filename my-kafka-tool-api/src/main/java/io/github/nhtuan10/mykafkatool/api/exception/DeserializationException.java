package io.github.nhtuan10.mykafkatool.api.exception;

import lombok.Getter;

public class DeserializationException extends Exception {

    @Getter
    private final Object data;

    public DeserializationException(String message, Object data) {
        super(message);
        this.data = data;
    }

    public DeserializationException(String message, Object data, Throwable e) {
        super(message, e);
        this.data = data;
    }
}
