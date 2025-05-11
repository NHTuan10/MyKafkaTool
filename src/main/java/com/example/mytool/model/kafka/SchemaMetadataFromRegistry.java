package com.example.mytool.model.kafka;

import io.confluent.kafka.schemaregistry.client.SchemaMetadata;

public record SchemaMetadataFromRegistry(SchemaMetadata schemaMetadata, String compatibility) {
}
