package io.github.nhtuan10.mykafkatool.serdes;

import io.github.nhtuan10.mykafkatool.api.PluggableDeserializer;
import io.github.nhtuan10.mykafkatool.api.PluggableSerializer;
import io.github.nhtuan10.mykafkatool.api.model.KafkaMessage;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.exception.DeserializationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class SerDesHelper {

    public static final String SERDE_STRING = "String";
    public static final String IS_KEY_PROP = "isKey";
    public static final String SERDE_AVRO = "AVRO";
    public static final String SCHEMA_PROP = "schema";
    //    public static final ObservableList<String> SUPPORT_VALUE_CONTENT_TYPES = FXCollections.observableArrayList(SerdeUtil.SERDE_STRING, SerdeUtil.SERDE_AVRO);
    private final Map<String, PluggableSerializer> serializerMap;
    private final Map<String, PluggableDeserializer> deserializerMap;

    public static boolean isValidSchemaForSerialization(SerDesHelper serDesHelper, String valueContentType, String schema) {
        boolean valid = true;
        PluggableSerializer serializer = serDesHelper.getPluggableSerialize(valueContentType);
        if (StringUtils.isNotBlank(schema) && serializer.mayNeedUserInputForSchema()) {
            try {
                valid = StringUtils.isNotBlank(schema) &&
                        serializer.parseSchema(schema) != null;
            } catch (Exception e) {
                log.warn("Error when parse schema", e);
                valid = false;
            }
        }
        return valid;
    }

    public Class<? extends Serializer> getSerializeClass(String contentType) {
        try {
            return (Class<? extends Serializer>) Class.forName(getPluggableSerialize(contentType).getSerializerClass());
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

    public Set<String> getSupportedValueDeserializer() {
        return deserializerMap.keySet();
    }

    public Set<String> getSupportedValueSerializer() {
        return serializerMap.keySet();
    }

    public Set<String> getSupportedKeyDeserializer() {
        return Set.of(SERDE_STRING);
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
            return (Class<? extends Deserializer>) Class.forName(getPluggableDeserialize(contentType).getDeserializerClass());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Object convertStringToObjectBeforeSerialize(String topic, Integer partition, KafkaMessage kafkaMessage, boolean isKey) throws IOException {
        PluggableSerializer serializer = getPluggableSerialize(isKey ? kafkaMessage.keyContentType() : kafkaMessage.valueContentType());
        if (serializer.isCustomSerializeMethodUsed()) {
            return serializer.serialize(topic, partition, kafkaMessage, kafkaMessage.headers(), Map.of(SerDesHelper.IS_KEY_PROP, Boolean.toString(isKey)));
        } else {
            return serializer.convertStringToObject(isKey ? kafkaMessage.key() : kafkaMessage.value(),
                    Map.of(AppConstant.SCHEMA, kafkaMessage.schema()));
        }
    }

    public String deserializeToJsonString(ConsumerRecord<String, Object> record, String contentType, Headers headers, Map<String, Object> consumerProps, Map<String, String> others) throws DeserializationException {
        Map<String, byte[]> headerMap = convertKafkaHeadersObjectToMap(headers);
        PluggableDeserializer deserializer = getPluggableDeserialize(contentType);
        Object payload = record.value();
        if (deserializer.isCustomDeserializeMethodUsed()) {
            if (payload instanceof byte[] payloadBytes) {

                try {
                    return deserializerMap.get(contentType).deserialize(record.topic(), record.partition(), payloadBytes, headerMap, consumerProps, others);
                } catch (Exception e) {
                    throw new DeserializationException("Deserialize error", payload, e);
                }
            } else {
                throw new DeserializationException("Internal Error: custom deserialize non byte array is not supported yet", payload);
            }
        } else {
            if (payload instanceof byte[] payloadBytes) {
                return Base64.getEncoder().encodeToString(payloadBytes);
            } else {
                return payload.toString();
            }
        }

    }

    private static Map<String, byte[]> convertKafkaHeadersObjectToMap(Headers headers) {
        return Arrays.stream(headers.toArray()).collect(Collectors.toMap(Header::key, Header::value, (v1, v2) -> v2));
    }


    public ValidationResult validateMessageAgainstSchema(String contentType, String content, String schemaStr) {
        PluggableSerializer serializer = serializerMap.get(contentType);
        if (serializer.mayNeedUserInputForSchema()) {
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
