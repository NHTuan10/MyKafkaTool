package io.github.nhtuan10.mykafkatool.api.serdes;

import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.mykafkatool.api.model.DisplayType;
import io.github.nhtuan10.mykafkatool.api.model.KafkaMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@ModularService
public interface PluggableSerializer {
    String getName();

    default String getSerializerClass() {
        return "org.apache.kafka.common.serialization.ByteArraySerializer";
    }

    default boolean isCustomSerializeMethodUsed() {
        return false;
    }

    default boolean mayNeedUserInputForSchema() {
        return false;
    }

    default Object parseSchema(String schema) {
        return schema;
    }
//     default ProducerRecord<Object,Object> serialize(String topic, Integer partition, KafkaMessage kafkaMessage, Map<String, byte[]> headerMap, Map<String, String> others) {
//          List<Header> headers = kafkaMessage.headers().entrySet().stream().map(entry -> new RecordHeader(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8))).collect(Collectors.toList());
//          return new ProducerRecord<>(topic, partition, kafkaMessage.key(), kafkaMessage.value(), headers);
//     }

    default byte[] serialize(String topic, Integer partition, KafkaMessage kafkaMessage, Map<String, byte[]> headerMap, Map<String, Object> others) throws Exception {
        return kafkaMessage.value().getBytes(StandardCharsets.UTF_8);
    }

    default Object convertStringToObject(String str, Map<String, Object> optionalParams) throws IOException {
        return str;
    }

    default DisplayType getDisplayType() {
        return DisplayType.TEXT;
    }
}
