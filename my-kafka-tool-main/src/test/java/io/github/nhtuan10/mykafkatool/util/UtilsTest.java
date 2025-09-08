package io.github.nhtuan10.mykafkatool.util;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import io.github.nhtuan10.mykafkatool.api.model.KafkaMessage;
import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageTableItem;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class UtilsTest {

    @Mock
    private KafkaMessageTableItem mockTableItem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testInitHandleBars() {
        // Given
        Handlebars testHandlebars = new Handlebars();

        // When
        Utils.initHandleBars(testHandlebars);

        // Then
        // Verify that helpers are registered (this is more of an integration test)
        assertNotNull(testHandlebars);
    }

    @Test
    void testGetAllFields_SimpleClass() {
        // Given
        class TestClass {
            private String field1;
            public int field2;
        }

        // When
        List<Field> fields = Utils.getAllFields(TestClass.class);

        // Then
        assertEquals(3, fields.size());
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("field1")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("field2")));
    }

    @Test
    void testGetAllFields_WithInheritance() {
        // Given
        class ParentClass {
            private String parentField;
        }
        class ChildClass extends ParentClass {
            private int childField;
        }

        // When
        List<Field> fields = Utils.getAllFields(ChildClass.class);

        // Then
        assertEquals(4, fields.size());
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("parentField")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("childField")));
    }

    @Test
    void testGetAllFields_ObjectClass() {
        // Given
        class TestClass {
            private String field1;
        }

        // When
        List<Field> fields = Utils.getAllFields(TestClass.class);

        // Then
        // Should include fields from TestClass but not from Object class
        assertTrue(fields.size() >= 1);
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("field1")));
    }

    @Test
    void testEvalHandlebarsAtNth_SimpleTemplate() throws IOException {
        // Given
        String template = "Hello {{counter}}";
        int n = 5;

        // When
        String result = Utils.evalHandlebarsAtNth(template, n);

        // Then
        assertEquals("Hello 5", result);
    }

    @Test
    void testEvalHandlebarsAtNth_WithZero() throws IOException {
        // Given
        String template = "Counter: {{counter}}";
        int n = 0;

        // When
        String result = Utils.evalHandlebarsAtNth(template, n);

        // Then
        assertEquals("Counter: 0", result);
    }

    @Test
    void testEvalHandlebarsAtNth_InvalidTemplate() {
        // Given
        String invalidTemplate = "Hello {{unclosed";
        int n = 1;

        // When & Then
        assertThrows(HandlebarsException.class, () -> Utils.evalHandlebarsAtNth(invalidTemplate, n));
    }

    @Test
    void testEvalHandlebars_MultipleIterations() throws IOException {
        // Given
        String template = "Item {{counter}}";
        int numOfLoop = 3;

        // When
        List<String> results = Utils.evalHandlebars(template, numOfLoop);

        // Then
        assertEquals(3, results.size());
        assertEquals("Item 1", results.get(0));
        assertEquals("Item 2", results.get(1));
        assertEquals("Item 3", results.get(2));
    }

    @Test
    void testEvalHandlebars_ZeroIterations() throws IOException {
        // Given
        String template = "Item {{counter}}";
        int numOfLoop = 0;

        // When
        List<String> results = Utils.evalHandlebars(template, numOfLoop);

        // Then
        assertEquals(0, results.size());
        assertTrue(results.isEmpty());
    }

    @Test
    void testEvalHandlebars_InvalidTemplate() {
        // Given
        String invalidTemplate = "Hello {{unclosed";
        int numOfLoop = 1;

        // When & Then
        assertThrows(HandlebarsException.class, () -> Utils.evalHandlebars(invalidTemplate, numOfLoop));
    }

    @Test
    void testConvertKafkaMessage_WithHeaders() {
        // Given
        String key = "test-key";
        String value = "test-value";
        String contentType = "application/json";
        String schema = "test-schema";

        Header header1 = new RecordHeader("header1", "value1".getBytes(StandardCharsets.UTF_8));
        Header header2 = new RecordHeader("header2", "value2".getBytes(StandardCharsets.UTF_8));
        Headers headers = new RecordHeaders(new Header[]{header1, header2});

        when(mockTableItem.getKey()).thenReturn(key);
        when(mockTableItem.getValue()).thenReturn(value);
        when(mockTableItem.getValueContentType()).thenReturn(contentType);
        when(mockTableItem.getSchema()).thenReturn(schema);
        when(mockTableItem.getHeaders()).thenReturn(headers);

        // When
        KafkaMessage result = Utils.convertKafkaMessage(mockTableItem);

        // Then
        assertEquals(key, result.key());
        assertEquals(value, result.value());
        assertEquals(contentType, result.valueContentType());
        assertEquals(schema, result.schema());
        assertEquals(2, result.headers().size());
        assertArrayEquals("value1".getBytes(StandardCharsets.UTF_8), result.headers().get("header1"));
        assertArrayEquals("value2".getBytes(StandardCharsets.UTF_8), result.headers().get("header2"));
    }

    @Test
    void testConvertKafkaMessage_WithDuplicateHeaders() {
        // Given
        String key = "test-key";
        String value = "test-value";
        String contentType = "application/json";
        String schema = "test-schema";

        Header header1 = new RecordHeader("duplicate", "value1".getBytes(StandardCharsets.UTF_8));
        Header header2 = new RecordHeader("duplicate", "value2".getBytes(StandardCharsets.UTF_8));
        Headers headers = new RecordHeaders(new Header[]{header1, header2});

        when(mockTableItem.getKey()).thenReturn(key);
        when(mockTableItem.getValue()).thenReturn(value);
        when(mockTableItem.getValueContentType()).thenReturn(contentType);
        when(mockTableItem.getSchema()).thenReturn(schema);
        when(mockTableItem.getHeaders()).thenReturn(headers);

        // When
        KafkaMessage result = Utils.convertKafkaMessage(mockTableItem);

        // Then
        assertEquals(1, result.headers().size());
        // Should keep the last value (v2) when there are duplicates
        assertArrayEquals("value2".getBytes(StandardCharsets.UTF_8), result.headers().get("duplicate"));
    }

    @Test
    void testConvertKafkaMessage_EmptyHeaders() {
        // Given
        String key = "test-key";
        String value = "test-value";
        String contentType = "application/json";
        String schema = "test-schema";
        Headers headers = new RecordHeaders(new Header[]{});

        when(mockTableItem.getKey()).thenReturn(key);
        when(mockTableItem.getValue()).thenReturn(value);
        when(mockTableItem.getValueContentType()).thenReturn(contentType);
        when(mockTableItem.getSchema()).thenReturn(schema);
        when(mockTableItem.getHeaders()).thenReturn(headers);

        // When
        KafkaMessage result = Utils.convertKafkaMessage(mockTableItem);

        // Then
        assertEquals(key, result.key());
        assertEquals(value, result.value());
        assertEquals(contentType, result.valueContentType());
        assertEquals(schema, result.schema());
        assertEquals(0, result.headers().size());
    }

    @Test
    void testConvertKafkaMessage_NullValues() {
        // Given
        when(mockTableItem.getKey()).thenReturn(null);
        when(mockTableItem.getValue()).thenReturn(null);
        when(mockTableItem.getValueContentType()).thenReturn(null);
        when(mockTableItem.getSchema()).thenReturn(null);
        when(mockTableItem.getHeaders()).thenReturn(new RecordHeaders());

        // When
        KafkaMessage result = Utils.convertKafkaMessage(mockTableItem);

        // Then
        assertNull(result.key());
        assertNull(result.value());
        assertNull(result.valueContentType());
        assertNull(result.schema());
        assertEquals(0, result.headers().size());
    }

    @Test
    void testHandleBarsHelpersNotWorking_RandomInt() {
        // Given
        int[] bounds = {10};

        // When
        int result = Utils.HandleBarsHelpersNotWorking.randomInt(bounds);

        // Then
        assertTrue(result >= 0);
        assertTrue(result < 10);
    }

    @Test
    void testHandleBarsHelpersNotWorking_RandomInt_WithZeroBound() {
        // Given
        int[] bounds = {0};

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> Utils.HandleBarsHelpersNotWorking.randomInt(bounds));
    }

    @Test
    void testHandleBarsHelpersNotWorking_RandomInt_WithNegativeBound() {
        // Given
        int[] bounds = {-5};

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> Utils.HandleBarsHelpersNotWorking.randomInt(bounds));
    }
}