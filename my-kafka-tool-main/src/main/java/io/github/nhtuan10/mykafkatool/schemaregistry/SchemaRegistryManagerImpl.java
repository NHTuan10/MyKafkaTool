package io.github.nhtuan10.mykafkatool.schemaregistry;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.entities.SubjectVersion;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.github.nhtuan10.mykafkatool.api.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.api.exception.ClusterNameExistedException;
import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;
import io.github.nhtuan10.mykafkatool.api.model.SchemaMetadataFromRegistry;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
@AppScoped
public class SchemaRegistryManagerImpl implements SchemaRegistryManager {

    //    private static class InstanceHolder {
//        private static final SchemaRegistryManager INSTANCE = new SchemaRegistryManager();
//    }
//
//    public static SchemaRegistryManager getInstance() {
//        return SchemaRegistryManager.InstanceHolder.INSTANCE;
//    }

    private final Map<String, SchemaRegistryClient> schemaRegistryClientMap = new ConcurrentHashMap<>();

    private final Map<String, List<SchemaMetadataFromRegistry>> schemaStore = new ConcurrentHashMap<>();

    @Override
    public Collection<String> getAllSubjects(String clusterName) throws RestClientException, IOException {
        return schemaRegistryClientMap.get(clusterName).getAllSubjects();
    }

    @Override
    public SchemaMetadata getSubjectMetadata(String clusterName, String subjectName) throws RestClientException, IOException {
        return schemaRegistryClientMap.get(clusterName).getLatestSchemaMetadata(subjectName);
    }

    @Override
    public String getCompatibility(String clusterName, String subjectName) {
        String compatibility = DEFAULT_SCHEMA_COMPATIBILITY_LEVEL;
        try {
            compatibility = schemaRegistryClientMap.get(clusterName).getCompatibility(subjectName);
        } catch (RestClientException | IOException e) {
            log.warn("Error when get compatibility level", e);
        }
        return compatibility;
    }

    @Override
    public List<Integer> getAllVersions(String clusterName, String subject) throws RestClientException, IOException {
        return schemaRegistryClientMap.get(clusterName).getAllVersions(subject).stream().sorted(Collections.reverseOrder()).toList();
    }

    @Override
    public SchemaMetadata getSubjectMetadata(String clusterName, String subject, Integer version) throws RestClientException, IOException {
        return schemaRegistryClientMap.get(clusterName).getSchemaMetadata(subject, version);
    }

    @Override
    public SchemaMetadataFromRegistry getSubjectMetadataFromRegistry(String clusterName, String subject, Integer version) throws RestClientException, IOException {
        SchemaMetadata schemaMetadata = getSubjectMetadata(clusterName, subject, version);
        String compatibility = getCompatibility(clusterName, subject);
        List<Integer> allVersions;
        if (version < 0){
            allVersions = List.of(schemaMetadata.getVersion());
        }
        else {
            allVersions = List.of(version);
        }
        return new SchemaMetadataFromRegistry(subject, schemaMetadata, compatibility, allVersions);
    }

    @Override
    public List<SchemaMetadataFromRegistry> getAllSubjectMetadata(String clusterName, boolean isOnlySubjectLoaded, boolean useCache) throws RestClientException, IOException {
        if (useCache) {
            return schemaStore.computeIfAbsent(clusterName, cluster -> {
                try {
                    return getSchemaMetadataFromRegistry(clusterName, isOnlySubjectLoaded);
                } catch (RestClientException | IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            List<SchemaMetadataFromRegistry> result = getSchemaMetadataFromRegistry(clusterName, isOnlySubjectLoaded);
            schemaStore.put(clusterName, result);
            return result;
        }
    }

    @Override
    public boolean isSchemaCachedForCluster(String clusterName) {
        return schemaStore.containsKey(clusterName);
    }

    private List<SchemaMetadataFromRegistry> getSchemaMetadataFromRegistry(String clusterName, boolean isOnlySubjectLoaded) throws RestClientException, IOException {
        Collection<String> subjects = getAllSubjects(clusterName);
        List<SchemaMetadataFromRegistry> result = subjects.parallelStream().map((subject) -> {
            SchemaMetadata schemaMetadata = null;
            String compatibility = null;
            List<Integer> allVersions = List.of();

            try {
                if (!isOnlySubjectLoaded) {
                    schemaMetadata = getSubjectMetadata(clusterName, subject);
                    compatibility = getCompatibility(clusterName, subject);
                    allVersions = getAllVersions(clusterName, subject);
                }
            } catch (Exception e) {
                log.warn("Error when get compatibility level", e);
            }
            return new SchemaMetadataFromRegistry(subject, schemaMetadata, compatibility, allVersions);
        }).toList();
        return result;
    }

    @Override
    public List<Integer> deleteSubject(String clusterName, String subject) throws RestClientException, IOException {
        return schemaRegistryClientMap.get(clusterName).deleteSubject(subject);
    }

    @Override
    public void connectToSchemaRegistry(KafkaCluster cluster) throws ClusterNameExistedException {
        String clusterName = cluster.getName();
        if (schemaRegistryClientMap.containsKey(clusterName)) {
            throw new ClusterNameExistedException(clusterName, "Cluster already exists");
        }
        SchemaRegistryClient client = new CachedSchemaRegistryClient(cluster.getSchemaRegistryUrl(), AppConstant.MAX_SCHEMA_CACHED_SIZE);
        schemaRegistryClientMap.put(clusterName, client);
    }

    @Override
    public void disconnectFromSchemaRegistry(String clusterName) {
        SchemaRegistryClient client = schemaRegistryClientMap.remove(clusterName);
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                log.warn("Error when close schema registry client", e);
            }
        }
    }

    @Override
    public List<SchemaMetadataFromRegistry> getSchemasById(String clusterName, int id) throws RestClientException, IOException {
//        Collection<String> subjects = schemaRegistryClientMap.get(clusterName).getAllSubjectsById(id);
        Collection<SubjectVersion> subjectVersions = schemaRegistryClientMap.get(clusterName).getAllVersionsById(id);
        List<SchemaMetadataFromRegistry> schemaTableItems = new ArrayList<>();
        for (SubjectVersion subjectVersion : subjectVersions) {
//            String schema = schemaRegistryClientMap.get(clusterName).getSchemaBySubjectAndId(subject, id).toString();
//            ParsedSchema parsedSchema = schemaRegistryClientMap.get(clusterName).getSchemaById(id);
//            SchemaMetadata schemaMetadata = new SchemaMetadata(id, parsedSchema.version(), parsedSchema.toString());
            String subject = subjectVersion.getSubject();
            Optional<SchemaMetadataFromRegistry> optional = schemaTableItems.stream().filter(s -> subject.equals(s.subjectName())).findFirst();
            if (optional.isPresent()) {
                optional.get().allVersions().add(subjectVersion.getVersion());
            } else {
                schemaTableItems.add(getSubjectMetadataFromRegistry(clusterName, subjectVersion.getSubject(), subjectVersion.getVersion()));
            }
        }
        return schemaTableItems;
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