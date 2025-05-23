package io.github.nhtuan10.mykafkatool.api;

import java.util.Map;

public interface PluggableDeserializer {
    String getName();

    default String getDeserializerClass() {
        return "org.apache.kafka.common.serialization.ByteArrayDeserializer";
    }

    default boolean isCustomDeserializeMethodUsed() {
        return false;
    }

    default boolean mayNeedUserInputForSchema() {
        return false;
    }

    default String deserialize(String topic, Integer partition, byte[] payload, Map<String, byte[]> headerMap, Map<String, Object> consumerProps, Map<String, String> others) throws Exception {
        return new String(payload);
    }

    default DisplayType getDisplayType() {
        return DisplayType.TEXT;
    }
}
