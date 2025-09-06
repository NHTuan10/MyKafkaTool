package io.github.nhtuan10.mykafkatool.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.api.auth.AuthConfig;
import io.github.nhtuan10.mykafkatool.api.auth.AuthProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class SaslProvider implements AuthProvider {
    protected final ObjectMapper objectMapper;
    public static final String SASL_PLAIN_JSON_FILE = "auth-sample/sasl-plain.json";
    public static final String SASL_KERBEROS_JSON_FILE = "auth-sample/sasl-kerberos.json";

    public SaslProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return getSecurityProtocol();
    }

    @Override
    public AuthConfig fromConfigText(String configText) throws JsonProcessingException {
        Map<String, Object> properties = Map.of(SECURITY_PROTOCOL, getSecurityProtocol());
        Map<String, Object> extraProperties = objectMapper.readValue(configText, new TypeReference<>() {
        });
        return new AuthConfig(getName(), properties, extraProperties);
    }

    @Override
    public String toConfigText(AuthConfig authConfig) throws JsonProcessingException {
        return objectMapper.writeValueAsString(authConfig.extraConfig());
    }

    @Override
    public String toString() {
        return getName();
    }

    protected abstract String getSecurityProtocol();

    @Override
    public Map<String, Object> getKafkaProperties(AuthConfig authConfig) throws Exception {
        return Stream.concat(authConfig.properties().entrySet().stream(), ((Map<String, Object>) authConfig.extraConfig()).entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> newValue
                ));
    }

    @Override
    public List<SampleAuthConfig> getSampleConfig() {
        try {
            return List.of(new SampleAuthConfig("PLAIN", IOUtils.toString(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(SASL_PLAIN_JSON_FILE)), StandardCharsets.UTF_8)),
                    new SampleAuthConfig("GSSAPI(Kerberos)", new String(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(SASL_KERBEROS_JSON_FILE)).readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            log.error("Failed to load SASL sample configurations", e);
            return List.of(new SampleAuthConfig("", ""));
        }
    }
}
