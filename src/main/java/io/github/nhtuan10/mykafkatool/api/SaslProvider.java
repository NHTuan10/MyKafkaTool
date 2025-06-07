package io.github.nhtuan10.mykafkatool.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.ui.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class SaslProvider implements AuthProvider {
    ObjectMapper objectMapper = Utils.contructObjectMapper();

    @Override
    public String getName() {
        return "SASL";
    }

    @Override
    public AuthConfig fromConfigText(String configText) throws JsonProcessingException {
        Map<String, Object> properties = objectMapper.readValue(configText, new TypeReference<>() {
        });
        return new AuthConfig(getName(), properties, new HashMap<>());
    }

    @Override
    public String toConfigText(AuthConfig authConfig) throws JsonProcessingException {
        return objectMapper.writeValueAsString(authConfig.properties());
    }

    @Override
    public String toString() {
        return getName();
    }
}
