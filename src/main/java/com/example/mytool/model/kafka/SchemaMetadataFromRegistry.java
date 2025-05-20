package com.example.mytool.model.kafka;

import io.confluent.kafka.schemaregistry.client.SchemaMetadata;

public record SchemaMetadataFromRegistry(String subjectName, SchemaMetadata schemaMetadata, String compatibility) {
}
