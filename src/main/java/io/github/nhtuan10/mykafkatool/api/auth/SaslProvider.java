package io.github.nhtuan10.mykafkatool.api.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.configuration.annotation.SharedObjectMapper;
import jakarta.inject.Inject;

import java.util.Map;

@AppScoped
public class SaslProvider implements AuthProvider {
    public static final String SASL = "SASL";
    protected final ObjectMapper objectMapper;

    @Inject
    public SaslProvider(@SharedObjectMapper ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return SASL;
    }

    @Override
    public AuthConfig fromConfigText(String configText) throws JsonProcessingException {
        Map<String, Object> properties = objectMapper.readValue(configText, new TypeReference<>() {
        });
        return new AuthConfig(getName(), properties, null);
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
