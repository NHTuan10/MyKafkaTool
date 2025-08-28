package io.github.nhtuan10.mykafkatool.model.kafka;

import io.confluent.kafka.schemaregistry.client.SchemaMetadata;

import java.util.List;

public record SchemaMetadataFromRegistry(String subjectName, SchemaMetadata schemaMetadata, String compatibility,
                                         List<Integer> allVersions) {
    @Override
    public String toString() {
        return subjectName;
    }
}
