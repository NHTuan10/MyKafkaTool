package io.github.nhtuan10.mykafkatool.serdes.serializer;

import io.github.nhtuan10.mykafkatool.api.Config;
import io.github.nhtuan10.mykafkatool.api.model.DisplayType;
import io.github.nhtuan10.mykafkatool.api.serdes.AvroUtil;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchemaRegistryAvroSerializerTest {

    private SchemaRegistryAvroSerializer serializer;
    private String testSchemaString;
    private Schema testSchema;

    @BeforeEach
    void setUp() {
        serializer = new SchemaRegistryAvroSerializer();

        testSchemaString = """
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

        testSchema = new Schema.Parser().parse(testSchemaString);
    }

    @Test
    void testGetName() {
        // Act
        String result = serializer.getName();

        // Assert
        assertEquals("Schema Registry Avro", result);
    }

    @Test
    void testGetSerializerClass() {
        // Act
        String result = serializer.getSerializerClass();

        // Assert
        assertEquals("io.confluent.kafka.serializers.KafkaAvroSerializer", result);
    }

    @Test
    void testMayNeedUserInputForSchema() {
        // Act
        boolean result = serializer.mayNeedUserInputForSchema();

        // Assert
        assertTrue(result);
    }

    @Test
    void testGetDisplayType() {
        // Act
        DisplayType result = serializer.getDisplayType();

        // Assert
        assertEquals(DisplayType.JSON, result);
    }

    @Test
    void testParseSchema() {
        try (MockedStatic<AvroUtil> mockedAvroUtil = Mockito.mockStatic(AvroUtil.class)) {
            // Arrange
            mockedAvroUtil.when(() -> AvroUtil.parseSchema(testSchemaString)).thenReturn(testSchema);

            // Act
            Object result = serializer.parseSchema(testSchemaString);

            // Assert
            assertEquals(testSchema, result);
            mockedAvroUtil.verify(() -> AvroUtil.parseSchema(testSchemaString));
        }
    }

    @Test
    void testParseSchema_WithNullSchema() {
        try (MockedStatic<AvroUtil> mockedAvroUtil = Mockito.mockStatic(AvroUtil.class)) {
            // Arrange
            mockedAvroUtil.when(() -> AvroUtil.parseSchema(null)).thenReturn(null);

            // Act
            Object result = serializer.parseSchema(null);

            // Assert
            assertNull(result);
            mockedAvroUtil.verify(() -> AvroUtil.parseSchema(null));
        }
    }

    @Test
    void testParseSchema_WithEmptySchema() {
        try (MockedStatic<AvroUtil> mockedAvroUtil = Mockito.mockStatic(AvroUtil.class)) {
            // Arrange
            String emptySchema = "";
            mockedAvroUtil.when(() -> AvroUtil.parseSchema(emptySchema)).thenReturn(null);

            // Act
            Object result = serializer.parseSchema(emptySchema);

            // Assert
            assertNull(result);
            mockedAvroUtil.verify(() -> AvroUtil.parseSchema(emptySchema));
        }
    }

    @Test
    void testConvertStringToObject_WithValidJsonAndSchema() throws IOException {
        try (MockedStatic<AvroUtil> mockedAvroUtil = Mockito.mockStatic(AvroUtil.class)) {
            // Arrange
            String jsonString = "{\"id\": 123, \"name\": \"John Doe\", \"email\": \"john@example.com\"}";
            Map<String, Object> optionalParams = new HashMap<>();
            optionalParams.put(Config.SCHEMA_PROP, testSchemaString);

            GenericRecord expectedRecord = new GenericData.Record(testSchema);
            expectedRecord.put("id", 123L);
            expectedRecord.put("name", "John Doe");
            expectedRecord.put("email", "john@example.com");

            mockedAvroUtil.when(() -> AvroUtil.convertJsonToAvro(jsonString, testSchemaString))
                    .thenReturn(expectedRecord);

            // Act
            Object result = serializer.convertStringToObject(jsonString, optionalParams);

            // Assert
            assertEquals(expectedRecord, result);
            mockedAvroUtil.verify(() -> AvroUtil.convertJsonToAvro(jsonString, testSchemaString));
        }
    }

    @Test
    void testConvertStringToObject_WithNullJson() throws IOException {
        try (MockedStatic<AvroUtil> mockedAvroUtil = Mockito.mockStatic(AvroUtil.class)) {
            // Arrange
            Map<String, Object> optionalParams = new HashMap<>();
            optionalParams.put(Config.SCHEMA_PROP, testSchemaString);

            mockedAvroUtil.when(() -> AvroUtil.convertJsonToAvro(null, testSchemaString))
                    .thenReturn(null);

            // Act
            Object result = serializer.convertStringToObject(null, optionalParams);

            // Assert
            assertNull(result);
            mockedAvroUtil.verify(() -> AvroUtil.convertJsonToAvro(null, testSchemaString));
        }
    }

    @Test
    void testConvertStringToObject_WithEmptyJson() throws IOException {

        String emptyJson = "";
        Map<String, Object> optionalParams = new HashMap<>();
        optionalParams.put(Config.SCHEMA_PROP, testSchemaString);

        assertThrows(Exception.class, () -> serializer.convertStringToObject(emptyJson, optionalParams));


    }

    @Test
    void testConvertStringToObject_WithMissingSchemaParam() throws IOException {
        // Arrange
        String jsonString = "{\"id\": 123, \"name\": \"John Doe\"}";
        Map<String, Object> optionalParams = new HashMap<>();
        // Missing SCHEMA_PROP

        // Act & Assert

        assertEquals(jsonString, serializer.convertStringToObject(jsonString, optionalParams));


    }

    @Test
    void testConvertStringToObject_WithNullSchemaParam() throws IOException {
        try (MockedStatic<AvroUtil> mockedAvroUtil = Mockito.mockStatic(AvroUtil.class)) {
            // Arrange
            String jsonString = "{\"id\": 123, \"name\": \"John Doe\"}";
            Map<String, Object> optionalParams = new HashMap<>();
            optionalParams.put(Config.SCHEMA_PROP, null);

            mockedAvroUtil.when(() -> AvroUtil.convertJsonToAvro(jsonString, null))
                    .thenReturn(jsonString);

            // Act
            Object result = serializer.convertStringToObject(jsonString, optionalParams);

            // Assert
            assertEquals(jsonString, result);
            mockedAvroUtil.verify(() -> AvroUtil.convertJsonToAvro(jsonString, null));
        }
    }

    @Test
    void testConvertStringToObject_WithEmptyOptionalParams() throws IOException {
        try (MockedStatic<AvroUtil> mockedAvroUtil = Mockito.mockStatic(AvroUtil.class)) {
            // Arrange
            String jsonString = "{\"id\": 123, \"name\": \"John Doe\"}";
            Map<String, Object> emptyParams = new HashMap<>();

            mockedAvroUtil.when(() -> AvroUtil.convertJsonToAvro(jsonString, null))
                    .thenReturn(jsonString);
            // Act & Assert

            assertEquals(jsonString, serializer.convertStringToObject(jsonString, emptyParams));

        }
    }

    @Test
    void testConvertStringToObject_AvroUtilThrowsIOException() throws IOException {
        try (MockedStatic<AvroUtil> mockedAvroUtil = Mockito.mockStatic(AvroUtil.class)) {
            // Arrange
            String jsonString = "{\"invalid\": \"json\"}";
            Map<String, Object> optionalParams = new HashMap<>();
            optionalParams.put(Config.SCHEMA_PROP, testSchemaString);

            IOException expectedException = new IOException("Failed to convert JSON to Avro");
            mockedAvroUtil.when(() -> AvroUtil.convertJsonToAvro(jsonString, testSchemaString))
                    .thenThrow(expectedException);

            // Act & Assert
            IOException thrownException = assertThrows(IOException.class, () -> {
                serializer.convertStringToObject(jsonString, optionalParams);
            });

            assertEquals(expectedException, thrownException);
            mockedAvroUtil.verify(() -> AvroUtil.convertJsonToAvro(jsonString, testSchemaString));
        }
    }

    @Test
    void testConvertStringToObject_WithComplexJson() throws IOException {
        try (MockedStatic<AvroUtil> mockedAvroUtil = Mockito.mockStatic(AvroUtil.class)) {
            // Arrange
            String complexJson = """
                    {
                      "id": 456,
                      "name": "Jane Smith",
                      "email": null
                    }
                    """;
            Map<String, Object> optionalParams = new HashMap<>();
            optionalParams.put(Config.SCHEMA_PROP, testSchemaString);

            GenericRecord expectedRecord = new GenericData.Record(testSchema);
            expectedRecord.put("id", 456L);
            expectedRecord.put("name", "Jane Smith");
            expectedRecord.put("email", null);

            mockedAvroUtil.when(() -> AvroUtil.convertJsonToAvro(complexJson, testSchemaString))
                    .thenReturn(expectedRecord);

            // Act
            Object result = serializer.convertStringToObject(complexJson, optionalParams);

            // Assert
            assertEquals(expectedRecord, result);
            mockedAvroUtil.verify(() -> AvroUtil.convertJsonToAvro(complexJson, testSchemaString));
        }
    }

    @Test
    void testConvertStringToObject_WithNonStringSchemaParam() throws IOException {
        try (MockedStatic<AvroUtil> mockedAvroUtil = Mockito.mockStatic(AvroUtil.class)) {
            // Arrange
            String jsonString = "{\"id\": 123, \"name\": \"John Doe\"}";
            Map<String, Object> optionalParams = new HashMap<>();
            optionalParams.put(Config.SCHEMA_PROP, testSchema); // Schema object instead of string

            String schemaString = testSchema.toString();
            mockedAvroUtil.when(() -> AvroUtil.convertJsonToAvro(jsonString, schemaString))
                    .thenReturn(jsonString);

            // Act
            Object result = serializer.convertStringToObject(jsonString, optionalParams);

            // Assert
            assertEquals(jsonString, result);
            mockedAvroUtil.verify(() -> AvroUtil.convertJsonToAvro(jsonString, schemaString));
        }
    }

    @Test
    void testIntegration_AllMethodsWork() throws IOException {
        try (MockedStatic<AvroUtil> mockedAvroUtil = Mockito.mockStatic(AvroUtil.class)) {
            // Arrange
            String jsonString = "{\"id\": 789, \"name\": \"Alice\", \"email\": \"alice@example.com\"}";
            Map<String, Object> optionalParams = new HashMap<>();
            optionalParams.put(Config.SCHEMA_PROP, testSchemaString);

            GenericRecord expectedRecord = new GenericData.Record(testSchema);
            expectedRecord.put("id", 789L);
            expectedRecord.put("name", "Alice");
            expectedRecord.put("email", "alice@example.com");

            mockedAvroUtil.when(() -> AvroUtil.parseSchema(testSchemaString)).thenReturn(testSchema);
            mockedAvroUtil.when(() -> AvroUtil.convertJsonToAvro(jsonString, testSchemaString))
                    .thenReturn(expectedRecord);

            // Act
            String name = serializer.getName();
            String serializerClass = serializer.getSerializerClass();
            boolean needsUserInput = serializer.mayNeedUserInputForSchema();
            DisplayType displayType = serializer.getDisplayType();
            Object parsedSchema = serializer.parseSchema(testSchemaString);
            Object convertedObject = serializer.convertStringToObject(jsonString, optionalParams);

            // Assert
            assertEquals("Schema Registry Avro", name);
            assertEquals("io.confluent.kafka.serializers.KafkaAvroSerializer", serializerClass);
            assertTrue(needsUserInput);
            assertEquals(DisplayType.JSON, displayType);
            assertEquals(testSchema, parsedSchema);
            assertEquals(expectedRecord, convertedObject);

            // Verify mock interactions
            mockedAvroUtil.verify(() -> AvroUtil.parseSchema(testSchemaString));
            mockedAvroUtil.verify(() -> AvroUtil.convertJsonToAvro(jsonString, testSchemaString));
        }
    }
}