package io.github.nhtuan10.mykafkatool.model.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.nhtuan10.mykafkatool.api.AuthConfig;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.model.common.Connection;
import lombok.*;

import java.util.Properties;

@EqualsAndHashCode(callSuper = true)
@Data
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaCluster extends Connection {
    @NonNull
    private final String name;
    @NonNull
    private final String bootstrapServer;
    private String schemaRegistryUrl;
    @Builder.Default
    private boolean isOnlySubjectLoaded = false;
    @Builder.Default
    private AuthConfig authConfig = new AuthConfig(AppConstant.NO_AUTH, new Properties(), new Properties());
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
}
