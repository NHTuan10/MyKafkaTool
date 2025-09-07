package io.github.nhtuan10.mykafkatool.api.serdes;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AvroUtilTest {

    private Schema testSchema;
    private String schemaString;
    private GenericRecord testRecord;
    private byte[] serializedData;

    @BeforeEach
    void setUp() throws IOException {
        // Define a test schema
        schemaString = """
                {
                  "type": "record",
                  "name": "User",
                  "fields": [
                    {"name": "id", "type": "long"},
                    {"name": "name", "type": "string"},
                    {"name": "email", "type": ["null", "string"], "default": null}
                  ]
                }
                """;

        testSchema = AvroUtil.parseSchema(schemaString);

        // Create a test record
        testRecord = new GenericData.Record(testSchema);
        testRecord.put("id", 123L);
        testRecord.put("name", "John Doe");
        testRecord.put("email", "john@example.com");

        // Serialize the test record to bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(testSchema);
        writer.write(testRecord, encoder);
        encoder.flush();
        serializedData = outputStream.toByteArray();
        outputStream.close();
    }


    /**
     * Tests for the parseSchema method.
     * These tests focus on verifying that the method correctly parses valid Avro schema strings
     * and handles invalid cases as expected.
     */

    @Test
    void parseSchema_validSchema() {
        // Arrange
        String validSchema = """
                {
                  "type": "record",
                  "name": "TestRecord",
                  "fields": [
                    {"name": "field1", "type": "string"}
                  ]
                }
                """;

        // Act
        Schema schema = AvroUtil.parseSchema(validSchema);

        // Assert
        assertNotNull(schema);
        assertEquals("TestRecord", schema.getName());
        assertEquals("string", schema.getField("field1").schema().getType().getName());
    }

    @Test
    void parseSchema_invalidSchema() {
        // Arrange
        String invalidSchema = """
                {
                  "type": "record",
                  "name": "TestRecord",
                  "fields": [
                    {"name": "field1", "type": "invalidType"}
                  ]
                }
                """;

        // Act & Assert
        assertThrows(RuntimeException.class, () -> AvroUtil.parseSchema(invalidSchema));
    }

    @Test
    void parseSchema_emptySchema() {
        // Arrange
        String emptySchema = "";

        // Act & Assert
        assertThrows(RuntimeException.class, () -> AvroUtil.parseSchema(emptySchema));
    }

    @Test
    void parseSchema_nullSchema() {
        // Arrange
        String nullSchema = null;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> AvroUtil.parseSchema(nullSchema));
    }

    @Test
    void testConvertJsonToAvro_WithValidJsonAndSchema() throws IOException {
        String json = "{\"id\": 123, \"name\": \"John Doe\", \"email\": {\"string\": \"john@example.com\"}}";

        Object result = AvroUtil.convertJsonToAvro(json, schemaString);

        assertNotNull(result);
        assertInstanceOf(GenericRecord.class, result);
        GenericRecord record = (GenericRecord) result;
        assertEquals(123L, record.get("id"));
        assertEquals("John Doe", record.get("name").toString());
        assertEquals("john@example.com", record.get("email").toString());
    }

    @Test
    void testConvertJsonToAvro_WithValidJsonAndNullSchema() throws IOException {
        String json = "{\"id\": 123, \"name\": \"John Doe\"}";

        Object result = AvroUtil.convertJsonToAvro(json, null);

        assertEquals(json, result);
    }

    @Test
    void testConvertJsonToAvro_WithValidJsonAndEmptySchema() throws IOException {
        String json = "{\"id\": 123, \"name\": \"John Doe\"}";

        Object result = AvroUtil.convertJsonToAvro(json, "");

        assertEquals(json, result);
    }

    @Test
    void testConvertJsonToAvro_WithValidJsonAndBlankSchema() throws IOException {
        String json = "{\"id\": 123, \"name\": \"John Doe\"}";

        Object result = AvroUtil.convertJsonToAvro(json, "   ");

        assertEquals(json, result);
    }

    @Test
    void testConvertJsonToAvro_WithInvalidSchema() throws IOException {
        String json = "{\"id\": 123, \"name\": \"John Doe\"}";
        String invalidSchema = "invalid schema";

        Object result = AvroUtil.convertJsonToAvro(json, invalidSchema);

        assertEquals(json, result);
    }

    @Test
    void testConvertJsonToAvro_WithNullJson() throws IOException {
        Object result = AvroUtil.convertJsonToAvro(null, schemaString);

        assertNull(result);
    }

    @Test
    void testConvertObjectToJsonString_WithGenericRecord() throws IOException {
        String result = AvroUtil.convertObjectToJsonString(testRecord);

        assertNotNull(result);
        assertTrue(result.contains("\"id\":123"));
        assertTrue(result.contains("\"name\":\"John Doe\""));
        assertTrue(result.contains("\"email\":{\"string\":\"john@example.com\"}"));
    }

    @Test
    void testConvertObjectToJsonString_WithString() throws IOException {
        String input = "test string";

        String result = AvroUtil.convertObjectToJsonString(input);

        assertEquals("test string", result);
    }

    @Test
    void testConvertObjectToJsonString_WithByteArray() throws IOException {
        byte[] input = "test bytes".getBytes();

        String result = AvroUtil.convertObjectToJsonString(input);

        assertEquals("test bytes", result);
    }

    @Test
    void testConvertObjectToJsonString_WithNull() throws IOException {
        String result = AvroUtil.convertObjectToJsonString(null);

        assertNull(result);
    }

    @Test
    void testConvertObjectToJsonString_WithOtherObject() throws IOException {
        Integer input = 42;

        String result = AvroUtil.convertObjectToJsonString(input);

        assertEquals("42", result);
    }

    @Test
    void testDeserialize_WithValidData() throws IOException {
        GenericRecord result = AvroUtil.deserialize(serializedData, testSchema);

        assertNotNull(result);
        assertEquals(123L, result.get("id"));
        assertEquals("John Doe", result.get("name").toString());
        assertEquals("john@example.com", result.get("email").toString());
    }

    @Test
    void testDeserialize_WithEmptyData() {
        byte[] emptyData = new byte[0];

        assertThrows(IOException.class, () -> {
            AvroUtil.deserialize(emptyData, testSchema);
        });
    }

    @Test
    void testDeserialize_WithNullData() {
        assertThrows(NullPointerException.class, () -> {
            AvroUtil.deserialize(null, testSchema);
        });
    }

    @Test
    void testDeserialize_WithNullSchema() {
        assertThrows(NullPointerException.class, () -> {
            AvroUtil.deserialize(serializedData, null);
        });
    }

    @Test
    void testDeserialize_WithInvalidData() {
        byte[] invalidData = "invalid data".getBytes();

        assertThrows(IOException.class, () -> {
            AvroUtil.deserialize(invalidData, testSchema);
        });
    }

    @Test
    void testDeserialize_WithMismatchedSchema() throws IOException {
        // Create a different schema
        String differentSchemaString = """
                {
                  "type": "record",
                  "name": "Product",
                  "fields": [
                    {"name": "id", "type": "string"},
                    {"name": "price", "type": "double"}
                  ]
                }
                """;
        Schema differentSchema = AvroUtil.parseSchema(differentSchemaString);

        assertThrows(IOException.class, () -> {
            AvroUtil.deserialize(serializedData, differentSchema);
        });
    }

    @Test
    void testIntegration_SerializeAndDeserialize() throws IOException {
        // Test the full cycle: create record -> serialize -> deserialize -> convert to JSON
        GenericRecord originalRecord = new GenericData.Record(testSchema);
        originalRecord.put("id", 456L);
        originalRecord.put("name", "Jane Smith");
        originalRecord.put("email", null); // Test null value

        // Serialize
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(testSchema);
        writer.write(originalRecord, encoder);
        encoder.flush();
        byte[] data = outputStream.toByteArray();
        outputStream.close();

        // Deserialize
        GenericRecord deserializedRecord = AvroUtil.deserialize(data, testSchema);

        // Convert to JSON
        String jsonResult = AvroUtil.convertObjectToJsonString(deserializedRecord);

        // Verify
        assertNotNull(jsonResult);
        assertTrue(jsonResult.contains("\"id\":456"));
        assertTrue(jsonResult.contains("\"name\":\"Jane Smith\""));
        assertTrue(jsonResult.contains("\"email\":null"));
    }

}