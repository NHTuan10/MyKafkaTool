package io.github.nhtuan10.mykafkatool.model.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.github.nhtuan10.mykafkatool.api.AuthConfig;
import io.github.nhtuan10.mykafkatool.api.AuthProvider;
import lombok.*;

import java.util.HashMap;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "")
public class KafkaCluster {
    @NonNull
    private final String name;
    @NonNull
    private final String bootstrapServer;
    private String schemaRegistryUrl;
    @Builder.Default
    private boolean isOnlySubjectLoaded = false;
    @Builder.Default
    @JsonSetter(nulls = Nulls.SKIP)
    private AuthConfig authConfig = new AuthConfig(AuthProvider.NO_AUTH, new HashMap<>(), new HashMap<>());
    @Override
    public String toString() {
        return name;
    }

    @JsonCreator
    public static KafkaCluster createKafkaClusterFromJSON(
            @JsonProperty("name") String name,
            @JsonProperty("bootstrapServer") String bootstrapServer) {

        return new KafkaCluster(name, bootstrapServer);
    }

    public static KafkaCluster.KafkaClusterBuilder builder(String name, String bootstrapServer) {
        return new KafkaClusterBuilder().name(name).bootstrapServer(bootstrapServer);
    }
}
