package io.github.nhtuan10.mykafkatool.api.serdes;

import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.mykafkatool.api.model.DisplayType;

import java.util.Map;

@ModularService
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

    // TODO: return object with value, schema and extra info if needed
    default String deserialize(String topic, Integer partition, byte[] payload, Map<String, byte[]> headerMap, Map<String, Object> consumerProps, Map<String, String> others) throws Exception {
        return new String(payload);
    }

    default DisplayType getDisplayType() {
        return DisplayType.TEXT;
    }
}
