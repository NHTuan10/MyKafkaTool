package com.example.mytool.manager;

import com.example.mytool.constant.AppConstant;
import com.example.mytool.exception.ClusterNameExistedException;
import com.example.mytool.model.kafka.KafkaCluster;
import com.example.mytool.model.kafka.SchemaMetadataFromRegistry;
import io.confluent.kafka.schemaregistry.CompatibilityLevel;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class SchemaRegistryManager {

    public static final String DEFAULT_SCHEMA_COMPATIBILITY_LEVEL = CompatibilityLevel.BACKWARD.toString();

    private static class InstanceHolder {
        private static final SchemaRegistryManager INSTANCE = new SchemaRegistryManager();
    }

    public static SchemaRegistryManager getInstance() {
        return SchemaRegistryManager.InstanceHolder.INSTANCE;
    }

    private final Map<String, SchemaRegistryClient> schemaRegistryClientMap = new ConcurrentHashMap<>();

    public Collection<String> getAllSubjects(String clusterName) throws RestClientException, IOException {
        return schemaRegistryClientMap.get(clusterName).getAllSubjects();
    }

    public SchemaMetadata getSubject(String clusterName, String subjectName) throws RestClientException, IOException {
        return schemaRegistryClientMap.get(clusterName).getLatestSchemaMetadata(subjectName);
    }

    public String getCompatibility(String clusterName, String subjectName) throws RestClientException, IOException {
        return schemaRegistryClientMap.get(clusterName).getCompatibility(subjectName);
    }


    public List<SchemaMetadataFromRegistry> getAllSubjectMetadata(String clusterName) throws RestClientException, IOException {
        Collection<String> subjects = getAllSubjects(clusterName);
        List<SchemaMetadataFromRegistry> result = new ArrayList<>();
        for (String subject : subjects) {
            String compatibility = DEFAULT_SCHEMA_COMPATIBILITY_LEVEL;
            try {
                compatibility = getCompatibility(clusterName, subject);
            } catch (Exception e) {
                log.warn("Error when get compatibility level", e);
            }
            result.add(new SchemaMetadataFromRegistry(getSubject(clusterName, subject), compatibility));
        }
        return result;
    }

    public List<Integer> deleteSubject(String clusterName, String subject) throws RestClientException, IOException {
        return schemaRegistryClientMap.get(clusterName).deleteSubject(subject);
    }

    public void connectToSchemaRegistry(KafkaCluster cluster) throws ClusterNameExistedException {
        String clusterName = cluster.getName();
        if (schemaRegistryClientMap.containsKey(clusterName)) {
            throw new ClusterNameExistedException(clusterName, "Cluster already exists");
        }
        SchemaRegistryClient client = new CachedSchemaRegistryClient(cluster.getSchemaRegistryUrl(), AppConstant.MAX_SCHEMA_CACHED_SIZE);
        schemaRegistryClientMap.put(clusterName, client);
    }

    public static void main(String[] args) {
        String schemaRegistryUrl = "http://localhost:8081"; // Replace with your Schema Registry URL
        // Maximum number of schemas to cache

        SchemaRegistryClient client = new CachedSchemaRegistryClient(schemaRegistryUrl, AppConstant.MAX_SCHEMA_CACHED_SIZE);

        try {
            // Example: Register a schema
            String subject = "my-topic-value";
            String avroSchema = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}";
            Schema schema = new Schema.Parser().parse(avroSchema);
            int schemaId = client.register(subject, schema);
            SchemaMetadata l = client.getLatestSchemaMetadata("customer");
            System.out.println("Registered schema with ID: " + schemaId);

            // Example: Get schema by ID
            Schema retrievedSchema = client.getById(schemaId);
            System.out.println("Retrieved schema: " + retrievedSchema.toString());

            // Example: Get latest schema version for a subject
            int latestVersion = client.getVersion(subject, schema);
            System.out.println("Latest schema version for subject '" + subject + "': " + latestVersion);

        } catch (IOException | RestClientException e) {
            e.printStackTrace();
        }
    }
}