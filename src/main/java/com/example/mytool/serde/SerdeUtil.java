package com.example.mytool.serde;

import com.example.mytool.api.PluggableDeserializer;
import com.example.mytool.api.PluggableSerializer;
import com.example.mytool.constant.AppConstant;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class SerdeUtil {

    public static final String SERDE_STRING = "String";
    public static final String SERDE_AVRO = "AVRO";
    //    public static final ObservableList<String> SUPPORT_VALUE_CONTENT_TYPES = FXCollections.observableArrayList(SerdeUtil.SERDE_STRING, SerdeUtil.SERDE_AVRO);
    private final Map<String, PluggableSerializer> serializerMap;
    private final Map<String, PluggableDeserializer> deserializerMap;

    public Class<? extends Serializer> getSerializeClass(String contentType) {
//        switch (contentType) {
//            case SERDE_AVRO:
//                return KafkaAvroSerializer.class;
////            return AvroSerializer.class.getName();
//            default:
//                return StringSerializer.class;
//        }
        try {
            return (Class<? extends Serializer>) Class.forName(serializerMap.get(contentType).getSerializerClass());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public PluggableDeserializer getPluggableDeserialize(String contentType) {
        return deserializerMap.get(contentType);
    }


    public PluggableSerializer getPluggableSerialize(String contentType) {
        return serializerMap.get(contentType);
    }

    public Set<String> getSupportedContentTypes() {
        return deserializerMap.keySet();
    }


//    public static Class<? extends Deserializer> getDeserializeClass(String contentType) {
//        switch (contentType) {
//            case SERDE_AVRO:

    /// /                return ByteArrayDeserializer.class.getName();
//                return KafkaAvroDeserializer.class;
//            default:
//                return StringDeserializer.class;
//        }
//    }
    public Class<? extends Deserializer> getDeserializeClass(String contentType) {
        try {
            return (Class<? extends Deserializer>) Class.forName(deserializerMap.get(contentType).getDeserializerClass());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Object convert(String serdeName, String content, String schemaStr) throws IOException {
//        switch (serdeName) {
//            case SERDE_STRING:
//                return content;
//            case SERDE_AVRO:
//
//                return AvroUtil.convertJsonToAvro(content, schemaStr);
//
//        }
//        return content;
        return serializerMap.get(serdeName).convertStringToObject(content, Map.of(AppConstant.SCHEMA, schemaStr));
    }

    public ValidationResult validateMessageAgainstSchema(String contentType, String content, String schemaStr) {
//        if (SERDE_AVRO.equals(contentType)) {
        PluggableSerializer serializer = serializerMap.get(contentType);
        if (serializer.isUserSchemaInputRequired()) {
            try {
                Object s = serializer.convertStringToObject(content, Map.of(AppConstant.SCHEMA, schemaStr));
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
