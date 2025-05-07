package com.example.mytool.serdes.deserializer;

import com.example.mytool.api.PluggableDeserializer;

public class SchemaRegistryAvroDeserializer implements PluggableDeserializer {
    @Override
    public String getName() {
        return "Schema Registry Avro";
    }

    @Override
    public String getDeserializerClass() {
        return "io.confluent.kafka.serializers.KafkaAvroDeserializer";
    }
}
