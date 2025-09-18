package io.github.nhtuan10.mykafkatool.serdes.deserializer;

import com.google.common.annotations.VisibleForTesting;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.github.nhtuan10.mykafkatool.api.Config;
import io.github.nhtuan10.mykafkatool.api.model.DisplayType;
import io.github.nhtuan10.mykafkatool.api.serdes.AvroUtil;
import io.github.nhtuan10.mykafkatool.api.serdes.PluggableDeserializer;
import org.apache.kafka.common.header.Headers;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SchemaRegistryAvroDeserializer implements PluggableDeserializer {
    private final Map<Object, KafkaAvroDeserializer> kafkaAvroDeserializerMap;

    public SchemaRegistryAvroDeserializer() {
        this.kafkaAvroDeserializerMap = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return "Schema Registry Avro";
    }

    public boolean isCustomDeserializeMethodUsed() {
        return true;
    }

    @Override
    public String deserialize(String topic, Integer partition, byte[] payload, Headers headers, Map<String, Object> consumerProps, Map<String, String> others) throws Exception {
        KafkaAvroDeserializer kafkaAvroDeserializer;
        boolean isKey = Boolean.parseBoolean(others.getOrDefault(Config.IS_KEY_PROP, "false"));
        Map<String, Object> serializerMapKey = new HashMap<>(Map.of(Config.IS_KEY_PROP, isKey));
        if (!kafkaAvroDeserializerMap.containsKey(serializerMapKey)) {
            kafkaAvroDeserializer = createKafkaAvroDeserializer(consumerProps, isKey);
            kafkaAvroDeserializerMap.put(serializerMapKey, kafkaAvroDeserializer);
        } else {
            kafkaAvroDeserializer = kafkaAvroDeserializerMap.get(serializerMapKey);
        }
//        Headers headers = new RecordHeaders(headerMap.entrySet().stream().map(entry -> (Header) new RecordHeader(entry.getKey(), entry.getValue())).toList());
        Object deserializedObject = kafkaAvroDeserializer.deserialize(topic, headers, payload);
        others.put(Config.SCHEMA_ID_PROP, String.valueOf(extractSchemaId(payload)));
        return AvroUtil.convertObjectToJsonString(deserializedObject);
    }

    @VisibleForTesting
    KafkaAvroDeserializer createKafkaAvroDeserializer(Map<String, Object> consumerProps, boolean isKey) {
        KafkaAvroDeserializer kafkaAvroDeserializer;
        kafkaAvroDeserializer = new KafkaAvroDeserializer();
        kafkaAvroDeserializer.configure(consumerProps, isKey);
        return kafkaAvroDeserializer;
    }

    @Override
    public DisplayType getDisplayType() {
        return DisplayType.JSON;
    }

    public int extractSchemaId(byte[] payload) {
        if (payload.length >= 5 && payload[0] == 0x0) {
            return ByteBuffer.wrap(payload, 1, 4).getInt();
        }
        return -1;
    }
//    @Override
//    public boolean mayNeedUserInputForSchema() {
//        return true;
//    }
}
