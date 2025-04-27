package com.example.mytool.serde;

import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;

public class SerdeUtil {

    public static final String SERDE_STRING = "String";
    public static final String SERDE_AVRO = "AVRO";

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

    public static Object convert(String serdeName, String content, String schemaStr) {
        switch (serdeName) {
            case SERDE_STRING:
                return content;
            case SERDE_AVRO:
                try {
                    return AvroUtil.convertJsonToAvro(content, schemaStr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return content;
    }

}
