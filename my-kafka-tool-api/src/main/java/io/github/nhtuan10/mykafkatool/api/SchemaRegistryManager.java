package io.github.nhtuan10.mykafkatool.api;

import io.confluent.kafka.schemaregistry.CompatibilityLevel;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.github.nhtuan10.mykafkatool.api.exception.ClusterNameExistedException;
import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;
import io.github.nhtuan10.mykafkatool.api.model.SchemaMetadataFromRegistry;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface SchemaRegistryManager {
    String DEFAULT_SCHEMA_COMPATIBILITY_LEVEL = CompatibilityLevel.BACKWARD.toString();

    Collection<String> getAllSubjects(String clusterName) throws RestClientException, IOException;

    SchemaMetadata getSubjectMetadata(String clusterName, String subjectName) throws RestClientException, IOException;

    String getCompatibility(String clusterName, String subjectName);

    List<Integer> getAllVersions(String clusterName, String subject) throws RestClientException, IOException;

    SchemaMetadata getSubjectMetadata(String clusterName, String subject, Integer version) throws RestClientException, IOException;

    SchemaMetadataFromRegistry getSubjectMetadataFromRegistry(String clusterName, String subject, Integer version) throws RestClientException, IOException;

    List<SchemaMetadataFromRegistry> getAllSubjectMetadata(String clusterName, boolean isOnlySubjectLoaded, boolean useCache) throws RestClientException, IOException;

    boolean isSchemaCachedForCluster(String clusterName);

    List<Integer> deleteSubject(String clusterName, String subject) throws RestClientException, IOException;

    void connectToSchemaRegistry(KafkaCluster cluster) throws ClusterNameExistedException;

    void disconnectFromSchemaRegistry(String clusterName);

    List<SchemaMetadataFromRegistry> getSchemasById(String clusterName, int id) throws RestClientException, IOException;
}
