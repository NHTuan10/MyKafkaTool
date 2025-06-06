package io.github.nhtuan10.mykafkatool.model.kafka;

import io.github.nhtuan10.mykafkatool.api.AuthConfig;
import io.github.nhtuan10.mykafkatool.model.common.Connection;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaCluster extends Connection {
    private String name;
    private String bootstrapServer;
    private String schemaRegistryUrl;
    @Builder.Default
    private boolean isOnlySubjectLoaded = false;
    private AuthConfig authConfig;
    @Override
    public String toString() {
        return name;
    }
}
