package com.example.mytool.serdes.deserializer.deprecation;

import com.example.mytool.api.PluggableDeserializer;

public class DeprecatedSchemaRegistryAvroDeserializer implements PluggableDeserializer {
    @Override
    public String getName() {
        return "Deprecated Schema Registry Avro";
    }

    @Override
    public String getDeserializerClass() {
        return "io.confluent.kafka.serializers.KafkaAvroDeserializer";
    }
}
