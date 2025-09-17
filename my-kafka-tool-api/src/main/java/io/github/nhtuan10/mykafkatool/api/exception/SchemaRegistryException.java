package io.github.nhtuan10.mykafkatool.api.exception;

public class SchemaRegistryException extends Exception {
    public SchemaRegistryException(String message) {
        super(message);
    }

    public SchemaRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
