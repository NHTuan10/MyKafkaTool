package io.github.nhtuan10.mykafkatool.model.kafka;

import io.confluent.kafka.schemaregistry.client.SchemaMetadata;

public record SchemaMetadataFromRegistry(String subjectName, SchemaMetadata schemaMetadata, String compatibility) {
}
