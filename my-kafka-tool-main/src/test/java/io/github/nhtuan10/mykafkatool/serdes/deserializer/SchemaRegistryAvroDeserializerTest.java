package io.github.nhtuan10.mykafkatool.serdes.deserializer;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.github.nhtuan10.mykafkatool.api.Config;
import io.github.nhtuan10.mykafkatool.api.model.DisplayType;
import io.github.nhtuan10.mykafkatool.api.serdes.AvroUtil;
import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemaRegistryAvroDeserializerTest {

    @Mock
    private KafkaAvroDeserializer mockKafkaAvroDeserializer;

    private SchemaRegistryAvroDeserializer deserializer;
    private Map<String, Object> consumerProps;
    private Map<String, byte[]> headerMap;
    private Map<String, String> others;

    @BeforeEach
    void setUp() {
        deserializer = new SchemaRegistryAvroDeserializer();
        consumerProps = new HashMap<>();
        consumerProps.put("bootstrap.servers", "localhost:9092");
        consumerProps.put("schema.registry.url", "http://localhost:8081");

        headerMap = new HashMap<>();
        headerMap.put("content-type", "application/avro".getBytes());

        others = new HashMap<>();
    }

    @Test
    void testGetName() {
        assertEquals("Schema Registry Avro", deserializer.getName());
    }

    @Test
    void testIsCustomDeserializeMethodUsed() {
        assertTrue(deserializer.isCustomDeserializeMethodUsed());
    }

    @Test
    void testGetDisplayType() {
        assertEquals(DisplayType.JSON, deserializer.getDisplayType());
    }

    @Test
    void testDeserialize_WithValidPayload() throws Exception {
        // Prepare test data
        String topic = "test-topic";
        Integer partition = 0;
        byte[] payload = createMockAvroPayload(123); // Schema ID 123
        Object mockDeserializedObject = Map.of("field1", "value1", "field2", 42);
        String expectedJson = "{\"field1\":\"value1\",\"field2\":42}";

        // Mock static AvroUtil
        try (MockedStatic<AvroUtil> avroUtilMock = mockStatic(AvroUtil.class)) {
            avroUtilMock.when(() -> AvroUtil.convertObjectToJsonString(mockDeserializedObject))
                    .thenReturn(expectedJson);

            // Create a spy to mock the KafkaAvroDeserializer creation
            SchemaRegistryAvroDeserializer spyDeserializer = spy(deserializer);
            doReturn(mockKafkaAvroDeserializer).when(spyDeserializer).createKafkaAvroDeserializer(consumerProps, false);

            // Mock KafkaAvroDeserializer behavior
            when(mockKafkaAvroDeserializer.deserialize(eq(topic), any(Headers.class), eq(payload)))
                    .thenReturn(mockDeserializedObject);

            // Execute test
            String result = spyDeserializer.deserialize(topic, partition, payload, headerMap, consumerProps, others);

            // Verify results
            assertEquals(expectedJson, result);
            assertEquals("123", others.get(Config.SCHEMA_ID_PROP));

        }
    }

    @Test
    void testDeserialize_WithKeyDeserializer() throws Exception {
        // Set up for key deserialization
        others.put(Config.IS_KEY_PROP, "true");

        String topic = "test-topic";
        Integer partition = 0;
        byte[] payload = createMockAvroPayload(456);
        Object mockDeserializedObject = "test-key";
        String expectedJson = "\"test-key\"";

        try (MockedStatic<AvroUtil> avroUtilMock = mockStatic(AvroUtil.class)) {
            avroUtilMock.when(() -> AvroUtil.convertObjectToJsonString(mockDeserializedObject))
                    .thenReturn(expectedJson);

            SchemaRegistryAvroDeserializer spyDeserializer = spy(deserializer);
            doReturn(mockKafkaAvroDeserializer).when(spyDeserializer).createKafkaAvroDeserializer(consumerProps, true);

            when(mockKafkaAvroDeserializer.deserialize(eq(topic), any(Headers.class), eq(payload)))
                    .thenReturn(mockDeserializedObject);

            String result = spyDeserializer.deserialize(topic, partition, payload, headerMap, consumerProps, others);

            assertEquals(expectedJson, result);
            assertEquals("456", others.get(Config.SCHEMA_ID_PROP));
        }
    }

    @Test
    void testDeserialize_CachesDeserializer() throws Exception {
        String topic = "test-topic";
        Integer partition = 0;
        byte[] payload1 = createMockAvroPayload(123);
        byte[] payload2 = createMockAvroPayload(456);
        Object mockObject = Map.of("test", "value");

        try (MockedStatic<AvroUtil> avroUtilMock = mockStatic(AvroUtil.class)) {
            avroUtilMock.when(() -> AvroUtil.convertObjectToJsonString(any()))
                    .thenReturn("{}");

            SchemaRegistryAvroDeserializer spyDeserializer = spy(deserializer);
            doReturn(mockKafkaAvroDeserializer).when(spyDeserializer).createKafkaAvroDeserializer(consumerProps, false);

            when(mockKafkaAvroDeserializer.deserialize(anyString(), any(Headers.class), any(byte[].class)))
                    .thenReturn(mockObject);

            // First call
            spyDeserializer.deserialize(topic, partition, payload1, headerMap, consumerProps, others);

            // Second call with same configuration
            spyDeserializer.deserialize(topic, partition, payload2, headerMap, consumerProps, others);

            // Verify deserializer was created only once
            verify(spyDeserializer, times(1)).createKafkaAvroDeserializer(consumerProps, false);
        }
    }

    @Test
    void testDeserialize_ThrowsException() throws Exception {
        String topic = "test-topic";
        Integer partition = 0;
        byte[] payload = createMockAvroPayload(123);

        SchemaRegistryAvroDeserializer spyDeserializer = spy(deserializer);
        doReturn(mockKafkaAvroDeserializer).when(spyDeserializer).createKafkaAvroDeserializer(consumerProps, false);

        when(mockKafkaAvroDeserializer.deserialize(anyString(), any(Headers.class), any(byte[].class)))
                .thenThrow(new RuntimeException("Deserialization failed"));

        assertThrows(RuntimeException.class, () ->
                spyDeserializer.deserialize(topic, partition, payload, headerMap, consumerProps, others)
        );
    }

    @Test
    void testExtractSchemaId_ValidPayload() {
        // Create payload with schema ID 12345
        byte[] payload = createMockAvroPayload(12345);

        int schemaId = deserializer.extractSchemaId(payload);

        assertEquals(12345, schemaId);
    }

    @Test
    void testExtractSchemaId_InvalidMagicByte() {
        // Create payload without magic byte (0x0)
        byte[] payload = new byte[]{0x1, 0x0, 0x0, 0x30, 0x39}; // Wrong magic byte

        int schemaId = deserializer.extractSchemaId(payload);

        assertEquals(-1, schemaId);
    }

    @Test
    void testExtractSchemaId_PayloadTooShort() {
        // Create payload that's too short
        byte[] payload = new byte[]{0x0, 0x0, 0x30}; // Only 3 bytes after magic byte

        int schemaId = deserializer.extractSchemaId(payload);

        assertEquals(-1, schemaId);
    }

    @Test
    void testExtractSchemaId_EmptyPayload() {
        byte[] payload = new byte[0];

        int schemaId = deserializer.extractSchemaId(payload);

        assertEquals(-1, schemaId);
    }

    @Test
    void testExtractSchemaId_NegativeSchemaId() {
        // Create payload with negative schema ID
        byte[] payload = createMockAvroPayload(-1);

        int schemaId = deserializer.extractSchemaId(payload);

        assertEquals(-1, schemaId);
    }

    @Test
    void testExtractSchemaId_LargeSchemaId() {
        int largeSchemaId = Integer.MAX_VALUE;
        byte[] payload = createMockAvroPayload(largeSchemaId);

        int schemaId = deserializer.extractSchemaId(payload);

        assertEquals(largeSchemaId, schemaId);
    }

    @Test
    void testDeserialize_WithEmptyHeaderMap() throws Exception {
        String topic = "test-topic";
        Integer partition = 0;
        byte[] payload = createMockAvroPayload(789);
        Map<String, byte[]> emptyHeaders = new HashMap<>();
        Object mockObject = Map.of("key", "value");

        try (MockedStatic<AvroUtil> avroUtilMock = mockStatic(AvroUtil.class)) {
            avroUtilMock.when(() -> AvroUtil.convertObjectToJsonString(mockObject))
                    .thenReturn("{\"key\":\"value\"}");

            SchemaRegistryAvroDeserializer spyDeserializer = spy(deserializer);
            doReturn(mockKafkaAvroDeserializer).when(spyDeserializer).createKafkaAvroDeserializer(consumerProps, false);

            when(mockKafkaAvroDeserializer.deserialize(eq(topic), any(Headers.class), eq(payload)))
                    .thenReturn(mockObject);

            String result = spyDeserializer.deserialize(topic, partition, payload, emptyHeaders, consumerProps, others);

            assertNotNull(result);
            assertEquals("{\"key\":\"value\"}", result);
            assertEquals("789", others.get(Config.SCHEMA_ID_PROP));
        }
    }

    /**
     * Helper method to create a mock Avro payload with schema ID
     */
    private byte[] createMockAvroPayload(int schemaId) {
        ByteBuffer buffer = ByteBuffer.allocate(9); // Magic byte (1) + Schema ID (4) + Some data (4)
        buffer.put((byte) 0x0); // Magic byte
        buffer.putInt(schemaId); // Schema ID
        buffer.putInt(0x12345678); // Mock data
        return buffer.array();
    }

    // Note: This method would need to be added to the SchemaRegistryAvroDeserializer class
    // to make it more testable, or we could use PowerMock to mock the constructor
    private KafkaAvroDeserializer createKafkaAvroDeserializer() {
        return new KafkaAvroDeserializer();
    }
}