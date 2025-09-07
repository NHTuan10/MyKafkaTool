package io.github.nhtuan10.mykafkatool.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.api.auth.AuthConfig;
import io.github.nhtuan10.mykafkatool.api.auth.AuthProvider.SampleAuthConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaslProviderTest {

    @Mock
    private ObjectMapper objectMapper;

    private SaslProvider saslProvider;

    @BeforeEach
    void setUp() {
        saslProvider = new SaslSslProvider(objectMapper);
    }


    @Test
    void fromConfigText_ValidJson_ReturnsExpectedAuthConfig() throws JsonProcessingException {
        // Arrange
        ObjectMapper objectMapper = new ObjectMapper();
        SaslProvider provider = new SaslSslProvider(objectMapper);
        String configText = "{\"sasl.mechanism\":\"GSSAPI\"}";
        // Act
        AuthConfig result = provider.fromConfigText(configText);

        // Assert
        assertEquals("SASL_SSL", result.name());
        assertEquals(Map.of("sasl.mechanism", "GSSAPI"), result.extraConfig());
        assertEquals(Map.of("security.protocol", "SASL_SSL"), result.properties());
    }

    @Test
    void fromConfigText_InvalidJson_ThrowsJsonProcessingException() {
        // Arrange
        ObjectMapper objectMapper = new ObjectMapper();
        SaslProvider provider = new SaslSslProvider(objectMapper);
        String invalidConfigText = "{invalidJson}";

        // Act & Assert
        assertThrows(JsonProcessingException.class, () -> provider.fromConfigText(invalidConfigText));
    }

    @Test
    void fromConfigText_EmptyJson_ReturnsAuthConfigWithEmptyExtraConfig() throws JsonProcessingException {
        // Arrange
        ObjectMapper objectMapper = new ObjectMapper();
        SaslProvider provider = new SaslSslProvider(objectMapper);
        String emptyConfigText = "{}";

        // Act
        AuthConfig result = provider.fromConfigText(emptyConfigText);

        // Assert
        assertEquals("SASL_SSL", result.name());
        assertEquals(Map.of(), result.extraConfig());
        assertEquals(Map.of("security.protocol", "SASL_SSL"), result.properties());
    }

    @Test
    void testToConfigText_WithValidAuthConfig() throws JsonProcessingException {
        // Arrange
        Map<String, Object> extraConfig = new HashMap<>();
        extraConfig.put("sasl.mechanism", "PLAIN");
        extraConfig.put("sasl.login.callback.handler.class", "org.apache.kafka.common.security.authenticator.SaslClientCallbackHandler");
        extraConfig.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"user\" password=\"password\";");

        Map<String, Object> properties = Map.of("security.protocol", "SASL_SSL");
        AuthConfig authConfig = new AuthConfig("SASL_SSL", properties, extraConfig);

        String expectedJson = """
                {"sasl.mechanism":"PLAIN","sasl.login.callback.handler.class":"org.apache.kafka.common.security.authenticator.SaslClientCallbackHandler","sasl.password":"org.apache.kafka.common.security.plain.PlainLoginModule required username=\"user\" password=\"password\";"}
                """;
        when(objectMapper.writeValueAsString(extraConfig)).thenReturn(expectedJson);

        // Act
        String result = saslProvider.toConfigText(authConfig);

        // Assert
        assertEquals(expectedJson, result);
        verify(objectMapper).writeValueAsString(extraConfig);
    }

    @Test
    void testToConfigText_WithEmptyExtraConfig() throws JsonProcessingException {
        // Arrange
        Map<String, Object> emptyExtraConfig = new HashMap<>();
        Map<String, Object> properties = Map.of("security.protocol", "SASL_SSL");
        AuthConfig authConfig = new AuthConfig("SASL_SSL", properties, emptyExtraConfig);

        String expectedJson = "{}";
        when(objectMapper.writeValueAsString(emptyExtraConfig)).thenReturn(expectedJson);

        // Act
        String result = saslProvider.toConfigText(authConfig);

        // Assert
        assertEquals(expectedJson, result);
        verify(objectMapper).writeValueAsString(emptyExtraConfig);
    }

    @Test
    void testToConfigText_JsonProcessingExceptionThrown() throws JsonProcessingException {
        // Arrange
        Map<String, Object> extraConfig = Map.of("key", "value");
        Map<String, Object> properties = Map.of("security.protocol", "SASL_SSL");
        AuthConfig authConfig = new AuthConfig("SASL_SSL", properties, extraConfig);

        when(objectMapper.writeValueAsString(extraConfig)).thenThrow(new JsonProcessingException("Test exception") {
        });

        // Act & Assert
        assertThrows(JsonProcessingException.class, () -> saslProvider.toConfigText(authConfig));
        verify(objectMapper).writeValueAsString(extraConfig);
    }

    @Test
    void testToString_ReturnsSecurityProtocol() {
        // Act
        String result = saslProvider.toString();

        // Assert
        assertEquals("SASL_SSL", result);
    }

    @Test
    void testGetKafkaProperties_WithValidAuthConfig() throws Exception {
        // Arrange
        Map<String, Object> properties = new HashMap<>();
        properties.put("security.protocol", "SASL_SSL");

        Map<String, Object> extraConfig = new HashMap<>();
        extraConfig.put("sasl.mechanism", "PLAIN");
        extraConfig.put("sasl.login.callback.handler.class", "org.apache.kafka.common.security.authenticator.SaslClientCallbackHandler");


        AuthConfig authConfig = new AuthConfig("SASL_SSL", properties, extraConfig);

        // Act
        Map<String, Object> result = saslProvider.getKafkaProperties(authConfig);

        // Assert
        assertEquals(3, result.size());
        assertEquals("SASL_SSL", result.get("security.protocol"));
        assertEquals("PLAIN", result.get("sasl.mechanism"));
        assertEquals("org.apache.kafka.common.security.authenticator.SaslClientCallbackHandler", result.get("sasl.login.callback.handler.class"));
    }

    @Test
    void testGetKafkaProperties_WithOverlappingKeys() throws Exception {
        // Arrange
        Map<String, Object> properties = new HashMap<>();
        properties.put("security.protocol", "SASL_SSL");
        properties.put("sasl.mechanism", "SCRAM-SHA-256"); // This will be overridden

        Map<String, Object> extraConfig = new HashMap<>();
        extraConfig.put("sasl.mechanism", "PLAIN"); // This should override the properties value
        extraConfig.put("sasl.username", "user");

        AuthConfig authConfig = new AuthConfig("SASL_SSL", properties, extraConfig);

        // Act
        Map<String, Object> result = saslProvider.getKafkaProperties(authConfig);

        // Assert
        assertEquals(3, result.size());
        assertEquals("SASL_SSL", result.get("security.protocol"));
        assertEquals("PLAIN", result.get("sasl.mechanism")); // Should be the value from extraConfig
        assertEquals("user", result.get("sasl.username"));
    }

    @Test
    void testGetKafkaProperties_WithEmptyConfigs() throws Exception {
        // Arrange
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> extraConfig = new HashMap<>();
        AuthConfig authConfig = new AuthConfig("SASL_SSL", properties, extraConfig);

        // Act
        Map<String, Object> result = saslProvider.getKafkaProperties(authConfig);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetKafkaProperties_WithNullExtraConfig() throws Exception {
        // Arrange
        Map<String, Object> properties = Map.of("security.protocol", "SASL_SSL");
        AuthConfig authConfig = new AuthConfig("SASL_SSL", properties, null);

        // Act & Assert
        assertThrows(Exception.class, () -> saslProvider.getKafkaProperties(authConfig));
    }

    @Test
    void testGetSampleConfig_Success() {
        // Act
        List<SampleAuthConfig> result = saslProvider.getSampleConfig();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        SampleAuthConfig plainConfig = result.get(0);
        assertEquals("PLAIN", plainConfig.name());
        assertTrue(plainConfig.sampleValue().contains("sasl.mechanism"));
        assertTrue(plainConfig.sampleValue().contains("PLAIN"));

        SampleAuthConfig kerberosConfig = result.get(1);
        assertEquals("GSSAPI(Kerberos)", kerberosConfig.name());
        assertTrue(kerberosConfig.sampleValue().contains("sasl.mechanism"));
        assertTrue(kerberosConfig.sampleValue().contains("GSSAPI"));
    }

    @Test
    void testGetSampleConfig_FileNotFound() {
        // Create a provider with a different class loader that won't find the files
        SaslProvider providerWithNoResources = new SaslSslProvider(objectMapper);

        // Act
        List<SampleAuthConfig> result = providerWithNoResources.getSampleConfig();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        SampleAuthConfig emptyConfig = result.get(0);
        assertEquals("PLAIN", emptyConfig.name());
        assertNotNull(emptyConfig.sampleValue());
    }

    @Test
    void testGetSampleConfig_ResourceContent() {
        // Act
        List<SampleAuthConfig> result = saslProvider.getSampleConfig();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify PLAIN config content
        SampleAuthConfig plainConfig = result.get(0);
        assertEquals("PLAIN", plainConfig.name());
        String plainSample = plainConfig.sampleValue();
        assertFalse(plainSample.isEmpty());
        assertTrue(plainSample.contains("\"sasl.mechanism\": \"PLAIN\""));

        // Verify GSSAPI config content
        SampleAuthConfig kerberosConfig = result.get(1);
        assertEquals("GSSAPI(Kerberos)", kerberosConfig.name());
        String kerberosSample = kerberosConfig.sampleValue();
        assertFalse(kerberosSample.isEmpty());
        assertTrue(kerberosSample.contains("\"sasl.mechanism\": \"GSSAPI\""));
        assertTrue(kerberosSample.contains("sasl.kerberos.service.name"));
        assertTrue(kerberosSample.contains("sasl.jaas.config"));
    }

    @Test
    void testIntegration_FullWorkflow() throws Exception {
        // Arrange
        String configText = "{\"sasl.mechanism\":\"PLAIN\",\"sasl.username\":\"testuser\",\"sasl.password\":\"testpass\"}";
        Map<String, Object> parsedConfig = new HashMap<>();
        parsedConfig.put("sasl.mechanism", "PLAIN");
        parsedConfig.put("sasl.username", "testuser");
        parsedConfig.put("sasl.password", "testpass");

        when(objectMapper.readValue(eq(configText), any(TypeReference.class))).thenReturn(parsedConfig);
        when(objectMapper.writeValueAsString(parsedConfig)).thenReturn(configText);

        // Act - Parse config text to AuthConfig
        AuthConfig authConfig = saslProvider.fromConfigText(configText);

        // Act - Convert back to config text
        String resultConfigText = saslProvider.toConfigText(authConfig);

        // Act - Get Kafka properties
        Map<String, Object> kafkaProperties = saslProvider.getKafkaProperties(authConfig);

        // Act - Get string representation
        String stringRepresentation = saslProvider.toString();

        // Assert
        assertNotNull(authConfig);
        assertEquals("SASL_SSL", authConfig.name());
        assertEquals("SASL_SSL", authConfig.properties().get("security.protocol"));
        assertEquals(parsedConfig, authConfig.extraConfig());

        assertEquals(configText, resultConfigText);

        assertEquals(4, kafkaProperties.size());
        assertEquals("SASL_SSL", kafkaProperties.get("security.protocol"));
        assertEquals("PLAIN", kafkaProperties.get("sasl.mechanism"));
        assertEquals("testuser", kafkaProperties.get("sasl.username"));
        assertEquals("testpass", kafkaProperties.get("sasl.password"));

        assertEquals("SASL_SSL", stringRepresentation);

        // Verify mock interactions
        verify(objectMapper).readValue(eq(configText), any(TypeReference.class));
        verify(objectMapper).writeValueAsString(parsedConfig);
    }

}