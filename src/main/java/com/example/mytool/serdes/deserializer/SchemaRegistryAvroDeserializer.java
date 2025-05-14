package com.example.mytool.serdes.deserializer;

import com.example.mytool.api.PluggableDeserializer;
import com.example.mytool.serdes.AvroUtil;
import com.example.mytool.serdes.SerdeUtil;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;

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
    public String deserialize(String topic, Integer partition, byte[] payload, Map<String, byte[]> headerMap, Map<String, Object> consumerProps, Map<String, String> others) throws Exception {
        KafkaAvroDeserializer kafkaAvroDeserializer;

        if (!kafkaAvroDeserializerMap.containsKey(consumerProps)) {
            kafkaAvroDeserializer = new KafkaAvroDeserializer();
            kafkaAvroDeserializerMap.put(consumerProps, kafkaAvroDeserializer);
        } else {
            kafkaAvroDeserializer = kafkaAvroDeserializerMap.get(consumerProps);
        }
        boolean isKey = Boolean.getBoolean(others.getOrDefault(SerdeUtil.IS_KEY_PROP, "false"));
        kafkaAvroDeserializer.configure(consumerProps, isKey);
        Headers headers = new RecordHeaders(headerMap.entrySet().stream().map(entry -> (Header) new RecordHeader(entry.getKey(), entry.getValue())).toList());
        Object deserializedObject = kafkaAvroDeserializer.deserialize(topic, headers, payload);
        return AvroUtil.convertObjectToJsonString(deserializedObject);
    }
}
