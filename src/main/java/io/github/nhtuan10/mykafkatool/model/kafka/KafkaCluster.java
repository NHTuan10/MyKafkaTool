package io.github.nhtuan10.mykafkatool.model.kafka;

import com.fasterxml.jackson.annotation.*;
import io.github.nhtuan10.mykafkatool.api.auth.AuthConfig;
import io.github.nhtuan10.mykafkatool.api.auth.AuthProvider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.*;

import java.util.HashMap;
import java.util.Optional;

@Data
//@RequiredArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "")
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(exclude = "status")
public class KafkaCluster {
    @NonNull
    private final StringProperty name;
    @NonNull
    private final String bootstrapServer;
    private String schemaRegistryUrl;
    @Builder.Default
    private boolean isOnlySubjectLoaded = false;
    @Builder.Default
    @JsonSetter(nulls = Nulls.SKIP)
    private AuthConfig authConfig = new AuthConfig(AuthProvider.NO_AUTH, new HashMap<>(), null);
    @Builder.Default
    @JsonIgnore
    @JsonSetter(nulls = Nulls.SKIP)
    private final ObjectProperty<ClusterStatus> status = new SimpleObjectProperty<>(ClusterStatus.DISCONNECTED);

    public KafkaCluster(@NonNull String name, @NonNull String bootstrapServer) {
        this.name = new SimpleStringProperty(name);
        this.bootstrapServer = bootstrapServer;
        status = new SimpleObjectProperty<>(ClusterStatus.DISCONNECTED);
    }

    public String getName() {
        return name.get();
    }

    public @NonNull StringProperty nameProperty() {
        return name;
    }


    public void setName(String name) {
        this.name.set(name);
    }

    public void setStatus(ClusterStatus status) {
        this.status.set(status);
    }

    public ClusterStatus getStatus() {
        return status.get();
    }

    public ObjectProperty<ClusterStatus> statusProperty() {
        return status;
    }

    @Override
    public String toString() {
        String name = Optional.ofNullable(this.name.get()).orElse("");
        return status.get() == ClusterStatus.CONNECTED ?
                "\uD83D\uDFE2 " + name :
                "\uD83D\uDD34 " + name;
    }

    @JsonCreator
    public static KafkaCluster createKafkaClusterFromJSON(
            @JsonProperty("name") String name,
            @JsonProperty("bootstrapServer") String bootstrapServer) {

        return new KafkaCluster(name, bootstrapServer);
    }

    public static KafkaCluster.KafkaClusterBuilder builder(String name, String bootstrapServer) {
        return new KafkaClusterBuilder().name(new SimpleStringProperty(name)).bootstrapServer(bootstrapServer);
    }

    public static enum ClusterStatus {
        CONNECTED, DISCONNECTED
    }
}
