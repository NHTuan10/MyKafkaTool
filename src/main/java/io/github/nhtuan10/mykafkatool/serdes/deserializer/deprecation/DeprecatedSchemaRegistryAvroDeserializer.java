package io.github.nhtuan10.mykafkatool.serdes.deserializer.deprecation;

import io.github.nhtuan10.mykafkatool.api.serdes.PluggableDeserializer;

public class DeprecatedSchemaRegistryAvroDeserializer implements PluggableDeserializer {
    @Override
    public String getName() {
        return "Deprecated Schema Registry Avro";
    }

    @Override
    public String getDeserializerClass() {
        return "io.confluent.kafka.serializers.KafkaAvroDeserializer";
    }

    @Override
    public boolean mayNeedUserInputForSchema() {
        return true;
    }
}
