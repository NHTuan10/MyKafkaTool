package com.example.mytool.serde;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;

public class SerdeUtil {

    public static final String SERDE_STRING = "String";
    public static final String SERDE_AVRO = "AVRO";
    public static final ObservableList<String> SUPPORT_VALUE_CONTENT_TYPES = FXCollections.observableArrayList(SerdeUtil.SERDE_STRING, SerdeUtil.SERDE_AVRO);

    public static String getSerializeClass(String contentType) {
        switch (contentType) {
            case SERDE_AVRO:
                return AvroSerializer.class.getName();
            default:
                return StringSerializer.class.getName();
        }
    }

    public static String getDeserializeClass(String contentType) {
        switch (contentType) {
            case SERDE_AVRO:
                return ByteArrayDeserializer.class.getName();
            default:
                return StringDeserializer.class.getName();
        }
    }

    public static Object convert(String serdeName, String content, String schemaStr) throws IOException {
        switch (serdeName) {
            case SERDE_STRING:
                return content;
            case SERDE_AVRO:

                    return AvroUtil.convertJsonToAvro(content, schemaStr);

        }
        return content;
    }

    public static ValidationResult validateMessageAgainstSchema(String contentType, String content, String schemaStr) {
        if (SERDE_AVRO.equals(contentType)) {
            try {
                GenericRecord s = AvroUtil.convertJsonToAvro(content, schemaStr);
                return new ValidationResult((s != null), new Exception("Empty content type"));
            } catch (Exception e) {
                return new ValidationResult(false, e);
            }
        }
        return new ValidationResult(true, null);
    }

    public record ValidationResult(boolean isValid, Throwable exception) {
    }
    
}
