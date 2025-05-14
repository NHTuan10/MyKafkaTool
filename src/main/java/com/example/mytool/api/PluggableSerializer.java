package com.example.mytool.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public interface PluggableSerializer {
    String getName();

    default String getSerializerClass() {
        return "org.apache.kafka.common.serialization.ByteArraySerializer";
    }

    default boolean isCustomSerializeMethodUsed() {
        return false;
    }

    default boolean mayUseSchema() {
        return false;
    }

    default Object parseSchema(String schema) {
        return schema;
    }
//     default ProducerRecord<Object,Object> serialize(String topic, Integer partition, KafkaMessage kafkaMessage, Map<String, byte[]> headerMap, Map<String, String> others) {
//          List<Header> headers = kafkaMessage.headers().entrySet().stream().map(entry -> new RecordHeader(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8))).collect(Collectors.toList());
//          return new ProducerRecord<>(topic, partition, kafkaMessage.key(), kafkaMessage.value(), headers);
//     }

    default byte[] serialize(String topic, Integer partition, KafkaMessage kafkaMessage, Map<String, byte[]> headerMap, Map<String, String> others) {
        return kafkaMessage.value().getBytes(StandardCharsets.UTF_8);
    }

    default Object convertStringToObject(String str, Map<String, Object> optionalParams) throws IOException {
        return str;
    }
}
