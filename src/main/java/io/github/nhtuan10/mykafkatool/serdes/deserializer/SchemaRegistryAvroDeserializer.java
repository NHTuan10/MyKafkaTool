package io.github.nhtuan10.mykafkatool.serdes.deserializer;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.github.nhtuan10.mykafkatool.api.DisplayType;
import io.github.nhtuan10.mykafkatool.api.PluggableDeserializer;
import io.github.nhtuan10.mykafkatool.serdes.AvroUtil;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;

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
    public String deserialize(String topic, Integer partition, byte[] payload, Map<String, byte[]> headerMap, Map<String, Object> consumerProps, Map<String, String> others) throws Exception {
        KafkaAvroDeserializer kafkaAvroDeserializer;
        boolean isKey = Boolean.getBoolean(others.getOrDefault(SerDesHelper.IS_KEY_PROP, "false"));
        Map<String, Object> serializerMapKey = new HashMap<>();
        serializerMapKey.putAll(Map.of(SerDesHelper.IS_KEY_PROP, isKey));
        if (!kafkaAvroDeserializerMap.containsKey(serializerMapKey)) {
            kafkaAvroDeserializer = new KafkaAvroDeserializer();
            kafkaAvroDeserializer.configure(consumerProps, isKey);
            kafkaAvroDeserializerMap.put(serializerMapKey, kafkaAvroDeserializer);
        } else {
            kafkaAvroDeserializer = kafkaAvroDeserializerMap.get(serializerMapKey);
        }
        Headers headers = new RecordHeaders(headerMap.entrySet().stream().map(entry -> (Header) new RecordHeader(entry.getKey(), entry.getValue())).toList());
        Object deserializedObject = kafkaAvroDeserializer.deserialize(topic, headers, payload);
        return AvroUtil.convertObjectToJsonString(deserializedObject);
    }

    @Override
    public DisplayType getDisplayType() {
        return DisplayType.JSON;
    }

}
