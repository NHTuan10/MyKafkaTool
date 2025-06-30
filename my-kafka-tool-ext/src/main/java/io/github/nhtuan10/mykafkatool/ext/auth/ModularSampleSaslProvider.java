package io.github.nhtuan10.mykafkatool.ext.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.mykafkatool.api.Config;
import io.github.nhtuan10.mykafkatool.api.auth.AuthConfig;
import io.github.nhtuan10.mykafkatool.api.auth.AuthProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@ModularService
public class ModularSampleSaslProvider implements AuthProvider {
    public static final String SASL = "Modular SASL";
    protected final ObjectMapper objectMapper;

    public ModularSampleSaslProvider() {
        this.objectMapper = Config.constructPrettyPrintObjectMapper();
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

    @Override
    public List<SampleAuthConfig> getSampleConfig() {
        try {
            return List.of(new SampleAuthConfig("PLAIN", new String(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("sasl-plain.json")).readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            log.error("Failed to load sasl-plain.json", e);
            return List.of(new SampleAuthConfig("", ""));
        }
    }
}
