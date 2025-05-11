package com.example.mytool.serdes.deserializer;

import com.example.mytool.api.PluggableDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SchemaRegistryAvroDeserializer implements PluggableDeserializer {
    private Map<Object, KafkaAvroDeserializer> kafkaAvroDeserializerMap;

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
    public byte[] deserialize(String topic, Integer partition, byte[] payload, Map<String, byte[]> headerMap, Map<String, Object> consumerProps, Map<String, String> others) {
        KafkaAvroDeserializer kafkaAvroDeserializer;

        if (!kafkaAvroDeserializerMap.containsKey(consumerProps)) {
            kafkaAvroDeserializer = new KafkaAvroDeserializer();
            kafkaAvroDeserializerMap.put(consumerProps, kafkaAvroDeserializer);
        } else {
            kafkaAvroDeserializer = kafkaAvroDeserializerMap.get(consumerProps);
        }
        boolean isKey = Boolean.getBoolean(others.getOrDefault("isKey", "false"));
        kafkaAvroDeserializer.configure(consumerProps, false);
        Headers headers = new RecordHeaders(headerMap.entrySet().stream().map(entry -> (Header) new RecordHeader(entry.getKey(), entry.getValue())).toList());
        Object deserializedObject = kafkaAvroDeserializer.deserialize(topic, headers, payload);
        return deserializedObject.toString().getBytes(StandardCharsets.UTF_8);
    }
}
