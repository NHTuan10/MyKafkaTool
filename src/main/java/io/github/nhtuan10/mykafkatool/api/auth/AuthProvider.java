package io.github.nhtuan10.mykafkatool.api.auth;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface AuthProvider {
    String NO_AUTH = "No Auth";

    String getName();

    AuthConfig fromConfigText(String configText) throws Exception;

    String toConfigText(AuthConfig authConfig) throws Exception;

    String toString();

    default Map<String, Object> getKafkaProperties(AuthConfig authConfig) throws Exception {
        return authConfig.properties();
    }

    default List<Pair<String, String>> getSampleConfig() {
        return new ArrayList<>();
    }
}
