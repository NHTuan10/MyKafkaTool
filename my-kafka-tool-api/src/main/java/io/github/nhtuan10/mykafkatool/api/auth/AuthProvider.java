package io.github.nhtuan10.mykafkatool.api.auth;

import io.github.nhtuan10.modular.api.annotation.ModularService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ModularService
public interface AuthProvider {
    String NO_AUTH = "No Auth";

    String getName();

    AuthConfig fromConfigText(String configText) throws Exception;

    String toConfigText(AuthConfig authConfig) throws Exception;

    String toString();

    default Map<String, Object> getKafkaProperties(AuthConfig authConfig) throws Exception {
        return authConfig.properties();
    }

    default List<SampleAuthConfig> getSampleConfig() {
        return new ArrayList<>();
    }

    public record SampleAuthConfig(String name, String sampleValue) {
    }
}
