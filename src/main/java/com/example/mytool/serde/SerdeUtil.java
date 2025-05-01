package com.example.mytool.serde;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;

public class SerdeUtil {

    public static final String SERDE_STRING = "String";
    public static final String SERDE_AVRO = "AVRO";
    public static final ObservableList<String> SUPPORT_VALUE_CONTENT_TYPES = FXCollections.observableArrayList(SerdeUtil.SERDE_STRING, SerdeUtil.SERDE_AVRO);

    public static Class<? extends Serializer> getSerializeClass(String contentType) {
        switch (contentType) {
            case SERDE_AVRO:
                return KafkaAvroSerializer.class;
//            return AvroSerializer.class.getName();
            default:
                return StringSerializer.class;
        }
    }

    public static Class<? extends Deserializer> getDeserializeClass(String contentType) {
        switch (contentType) {
            case SERDE_AVRO:
//                return ByteArrayDeserializer.class.getName();
                return KafkaAvroDeserializer.class;
            default:
                return StringDeserializer.class;
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
