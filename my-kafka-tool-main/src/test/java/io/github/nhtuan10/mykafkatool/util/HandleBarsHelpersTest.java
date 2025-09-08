package io.github.nhtuan10.mykafkatool.util;

import com.github.jknack.handlebars.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class HandleBarsHelpersTest {

    private Options options;

    @BeforeEach
    void setUp() {
//        when(options.param(0)).thenReturn(new Object[0]);
//        options.params = new Object[0];
        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{}).build();
    }

    // randomInt Tests
    @Test
    void testRandomInt_NoContext_NoParams() throws IOException {
        // Act

        Object result = HandleBarsHelpers.randomInt.apply(10, options);

        // Assert
        assertInstanceOf(Integer.class, result);
        int value = (Integer) result;
        // Random int can be any value, just verify it's an integer
        assertTrue(value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE);
    }

    @Test
    void testRandomInt_WithIntegerContext_NoParams() throws IOException {
        // Arrange
        Integer context = 10;

        // Act
        Object result = HandleBarsHelpers.randomInt.apply(context, options);

        // Assert
        assertInstanceOf(Integer.class, result);
        int value = (Integer) result;
        assertTrue(value >= 0 && value < 10);
    }

    @Test
    void testRandomInt_WithIntegerContext_WithParam() throws IOException {
        // Arrange
        Integer context = 5;

        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{15}).build();

        // Act
        Object result = HandleBarsHelpers.randomInt.apply(context, options);

        // Assert
        assertInstanceOf(Integer.class, result);
        int value = (Integer) result;
        assertTrue(value >= 5 && value < 15);
    }

    @Test
    void testRandomInt_WithDoubleContext() throws IOException {
        // Arrange
        Double context = 7.5;

        // Act
        Object result = HandleBarsHelpers.randomInt.apply(context, options);

        // Assert
        assertInstanceOf(Integer.class, result);
        int value = (Integer) result;
        assertTrue(value >= 0 && value < 7);
    }

    // randomDouble Tests
    @Test
    void testRandomDouble_NoContext_NoParams() throws IOException {
        // Act
        Object result = HandleBarsHelpers.randomDouble.apply(null, options);

        // Assert
        assertInstanceOf(Double.class, result);
        double value = (Double) result;
        assertTrue(value >= 0.0 && value < 1.0);
    }

    @Test
    void testRandomDouble_WithDoubleContext_NoParams() throws IOException {
        // Arrange
        Double context = 10.5;

        // Act
        Object result = HandleBarsHelpers.randomDouble.apply(context, options);

        // Assert
        assertInstanceOf(Double.class, result);
        double value = (Double) result;
        assertTrue(value >= 0.0 && value < 10.5);
    }

    @Test
    void testRandomDouble_WithDoubleContext_WithParam() throws IOException {
        // Arrange
        Double context = 5.5;
        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{15.7}).build();

        // Act
        Object result = HandleBarsHelpers.randomDouble.apply(context, options);

        // Assert
        assertInstanceOf(Double.class, result);
        double value = (Double) result;
        assertTrue(value >= 5.5 && value < 15.7);
    }

    @Test
    void testRandomDouble_WithIntegerContext() throws IOException {
        // Arrange
        Integer context = 8;

        // Act
        Object result = HandleBarsHelpers.randomDouble.apply(context, options);

        // Assert
        assertInstanceOf(Double.class, result);
        double value = (Double) result;
        assertTrue(value >= 0.0 && value < 8.0);
    }

    // uuid Tests
    @Test
    void testUuid() throws IOException {
        // Act
        Object result = HandleBarsHelpers.uuid.apply(null, options);

        // Assert
        assertInstanceOf(String.class, result);
        String uuid = (String) result;

        // Verify it's a valid UUID format
        assertDoesNotThrow(() -> UUID.fromString(uuid));

        // UUID should be 36 characters with hyphens
        assertEquals(36, uuid.length());
        assertTrue(Pattern.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", uuid));
    }

    @Test
    void testUuid_MultipleCalls_ReturnDifferentValues() throws IOException {
        // Act
        Object result1 = HandleBarsHelpers.uuid.apply(null, options);
        Object result2 = HandleBarsHelpers.uuid.apply(null, options);

        // Assert
        assertNotEquals(result1, result2);
    }

    // randomString Tests
    @Test
    void testRandomString_NoContext() throws IOException {
        // Act
        Object result = HandleBarsHelpers.randomString.apply(null, options);

        // Assert
        assertInstanceOf(String.class, result);
        String randomStr = (String) result;
        assertTrue(randomStr.length() >= 0 && randomStr.length() <= 36);
        assertTrue(randomStr.matches("[A-Za-z0-9]*"));
    }

    @Test
    void testRandomString_WithIntegerContext() throws IOException {
        // Arrange
        Integer context = 10;

        // Act
        Object result = HandleBarsHelpers.randomString.apply(context, options);

        // Assert
        assertInstanceOf(String.class, result);
        String randomStr = (String) result;
        assertEquals(10, randomStr.length());
        assertTrue(randomStr.matches("[A-Za-z0-9]*"));
    }

    @Test
    void testRandomString_WithIntegerContext_WithParam() throws IOException {
        // Arrange
        Integer context = 5;
        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{null, 15}).build();

        // Act
        Object result = HandleBarsHelpers.randomString.apply(context, options);

        // Assert
        assertInstanceOf(String.class, result);
        String randomStr = (String) result;
        assertTrue(randomStr.length() >= 5 && randomStr.length() <= 15);
        assertTrue(randomStr.matches("[A-Za-z0-9]*"));
    }

    @Test
    void testRandomString_WithZeroLength() throws IOException {
        // Arrange
        Integer context = 0;

        // Act
        Object result = HandleBarsHelpers.randomString.apply(context, options);

        // Assert
        assertInstanceOf(String.class, result);
        String randomStr = (String) result;
        assertEquals(0, randomStr.length());
    }

    // currentDate Tests
    @Test
    void testCurrentDate_NoContext() throws IOException {
        // Act
        Object result = HandleBarsHelpers.currentDate.apply(null, options);

        // Assert
        assertInstanceOf(LocalDate.class, result);
        LocalDate date = (LocalDate) result;
        assertEquals(LocalDate.now(), date);
    }

    @Test
    void testCurrentDate_WithFormat() throws IOException {
        // Arrange
        String format = "yyyy-MM-dd";

        // Act
        Object result = HandleBarsHelpers.currentDate.apply(format, options);

        // Assert
        assertInstanceOf(String.class, result);
        String dateStr = (String) result;

        // Verify it matches expected format
        assertDoesNotThrow(() -> LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format)));
        assertTrue(dateStr.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yyyy/MM/dd", "dd-MM-yyyy", "MMM dd, yyyy"})
    void testCurrentDate_WithDifferentFormats(String format) throws IOException {
        // Act
        Object result = HandleBarsHelpers.currentDate.apply(format, options);

        // Assert
        assertInstanceOf(String.class, result);
        String dateStr = (String) result;
        assertDoesNotThrow(() -> LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format)));
    }

    @Test
    void testCurrentDate_WithInvalidFormat() {
        // Arrange
        String invalidFormat = "invalid-format";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            HandleBarsHelpers.currentDate.apply(invalidFormat, options);
        });
    }

    // currentTime Tests
    @Test
    void testCurrentTime_NoContext() throws IOException {
        // Act
        Object result = HandleBarsHelpers.currentTime.apply(null, options);

        // Assert
        assertInstanceOf(LocalTime.class, result);
    }

    @Test
    void testCurrentTime_WithFormat() throws IOException {
        // Arrange
        String format = "HH:mm:ss";

        // Act
        Object result = HandleBarsHelpers.currentTime.apply(format, options);

        // Assert
        assertInstanceOf(String.class, result);
        String timeStr = (String) result;
        assertDoesNotThrow(() -> LocalTime.parse(timeStr, DateTimeFormatter.ofPattern(format)));
        assertTrue(timeStr.matches("\\d{2}:\\d{2}:\\d{2}"));
    }

    // currentDateTime Tests
    @Test
    void testCurrentDateTime_NoContext() throws IOException {
        // Act
        Object result = HandleBarsHelpers.currentDateTime.apply(null, options);

        // Assert
        assertInstanceOf(LocalDateTime.class, result);
    }

    @Test
    void testCurrentDateTime_WithFormat() throws IOException {
        // Arrange
        String format = "yyyy-MM-dd HH:mm:ss";

        // Act
        Object result = HandleBarsHelpers.currentDateTime.apply(format, options);

        // Assert
        assertInstanceOf(String.class, result);
        String dateTimeStr = (String) result;
        assertDoesNotThrow(() -> LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(format)));
    }

    // epochMillis Tests
    @Test
    void testEpochMillis() throws IOException {
        // Act
        Object result = HandleBarsHelpers.epochMillis.apply(null, options);

        // Assert
        assertInstanceOf(Long.class, result);
        long epochMillis = (Long) result;
        assertTrue(epochMillis > 0);

        // Should be close to current time (within 1 second)
        long currentTime = System.currentTimeMillis();
        assertTrue(Math.abs(currentTime - epochMillis) < 1000);
    }

    // epochSeconds Tests
    @Test
    void testEpochSeconds() throws IOException {
        // Act
        Object result = HandleBarsHelpers.epochSeconds.apply(null, options);

        // Assert
        assertInstanceOf(Long.class, result);
        long epochSeconds = (Long) result;
        assertTrue(epochSeconds > 0);

        // Should be close to current time (within 1 second)
        long currentSeconds = System.currentTimeMillis() / 1000;
        assertTrue(Math.abs(currentSeconds - epochSeconds) <= 1);
    }

    // round Tests
    @Test
    void testRound_WithDouble_NoScale() throws IOException {
        // Arrange
        Double context = 3.7;

        // Act
        Object result = HandleBarsHelpers.round.apply(context, options);

        // Assert
        assertInstanceOf(BigDecimal.class, result);
        BigDecimal rounded = (BigDecimal) result;
        assertEquals(BigDecimal.valueOf(4), rounded);
    }

    @Test
    void testRound_WithDouble_WithScale() throws IOException {
        // Arrange
        Double context = 3.14159;
//        when(options.params).thenReturn(new Object[]{2});
        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Integer[]{2}).build();

        // Act
        Object result = HandleBarsHelpers.round.apply(context, options);

        // Assert
        assertInstanceOf(BigDecimal.class, result);
        BigDecimal rounded = (BigDecimal) result;
        assertEquals(BigDecimal.valueOf(3.14), rounded);
    }

    @Test
    void testRound_WithNonNumber_ThrowsException() {
        // Arrange
        String context = "not a number";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            HandleBarsHelpers.round.apply(context, options);
        });
    }

    // ceil Tests
    @Test
    void testCeil_WithDouble() throws IOException {
        // Arrange
        Double context = 3.1;

        // Act
        Object result = HandleBarsHelpers.ceil.apply(context, options);

        // Assert
        assertInstanceOf(BigDecimal.class, result);
        BigDecimal ceiled = (BigDecimal) result;
        assertEquals(BigDecimal.valueOf(4), ceiled);
    }

    @Test
    void testCeil_WithDouble_WithScale() throws IOException {
        // Arrange
        Double context = 3.141;
        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{1}).build();

        // Act
        Object result = HandleBarsHelpers.ceil.apply(context, options);

        // Assert
        assertInstanceOf(BigDecimal.class, result);
        BigDecimal ceiled = (BigDecimal) result;
        assertEquals(BigDecimal.valueOf(3.2), ceiled);
    }

    // floor Tests
    @Test
    void testFloor_WithDouble() throws IOException {
        // Arrange
        Double context = 3.9;

        // Act
        Object result = HandleBarsHelpers.floor.apply(context, options);

        // Assert
        assertInstanceOf(BigDecimal.class, result);
        BigDecimal floored = (BigDecimal) result;
        assertEquals(BigDecimal.valueOf(3), floored);
    }

    @Test
    void testFloor_WithDouble_WithScale() throws IOException {
        // Arrange
        Double context = 3.159;
        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{1}).build();

        // Act
        Object result = HandleBarsHelpers.floor.apply(context, options);

        // Assert
        assertInstanceOf(BigDecimal.class, result);
        BigDecimal floored = (BigDecimal) result;
        assertEquals(BigDecimal.valueOf(3.1), floored);
    }

    // math Tests - Addition
    @Test
    void testMath_Addition() throws IOException {
        // Arrange
        Double context = 5.0;

        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{"+", 3.0}).build();

        // Act
        Object result = HandleBarsHelpers.math.apply(context, options);

        // Assert
        assertInstanceOf(BigDecimal.class, result);
        BigDecimal mathResult = (BigDecimal) result;
        assertEquals(BigDecimal.valueOf(8.0), mathResult);
    }

    // math Tests - Subtraction
    @Test
    void testMath_Subtraction() throws IOException {
        // Arrange
        Double context = 10.0;
        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{"-", 4.0}).build();

        // Act
        Object result = HandleBarsHelpers.math.apply(context, options);

        // Assert
        assertInstanceOf(BigDecimal.class, result);
        BigDecimal mathResult = (BigDecimal) result;
        assertEquals(BigDecimal.valueOf(6.0), mathResult);
    }

    // math Tests - Multiplication
    @Test
    void testMath_Multiplication() throws IOException {
        // Arrange
        Double context = 6.0;
        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{"*", 7.0}).build();

        // Act
        Object result = HandleBarsHelpers.math.apply(context, options);

        // Assert
        assertInstanceOf(BigDecimal.class, result);
        BigDecimal mathResult = (BigDecimal) result;
        assertEquals(0, BigDecimal.valueOf(42.0).compareTo(mathResult));
    }

    // math Tests - Division
    @Test
    void testMath_Division() throws IOException {
        // Arrange
        Double context = 15.0;
        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{"/", 3.0}).build();

        // Act
        Object result = HandleBarsHelpers.math.apply(context, options);

        // Assert
        assertInstanceOf(BigDecimal.class, result);
        BigDecimal mathResult = (BigDecimal) result;
        assertEquals(0, BigDecimal.valueOf(5.0).compareTo(mathResult));
    }

    // math Tests - Division with precision
    @Test
    void testMath_DivisionWithPrecision() throws IOException {
        // Arrange
        Double context = 10.0;
        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{"/", 3.0, 2}).build();

        // Act
        Object result = HandleBarsHelpers.math.apply(context, options);

        // Assert
        assertInstanceOf(BigDecimal.class, result);
        BigDecimal mathResult = (BigDecimal) result;
        assertEquals(0, mathResult.compareTo(BigDecimal.valueOf(3.33)));
    }

    // math Tests - Modulo
    @Test
    void testMath_Modulo() throws IOException {
        // Arrange
        Double context = 17.0;

        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{"%", 5.0}).build();

        // Act
        Object result = HandleBarsHelpers.math.apply(context, options);

        // Assert
        assertInstanceOf(Long.class, result);
        Long mathResult = (Long) result;
        assertEquals(Long.valueOf(2), mathResult);
    }

    // math Tests - Invalid operation
    @Test
    void testMath_InvalidOperation() {
        // Arrange
        Double context = 5.0;
        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{"^", 2.0}).build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            HandleBarsHelpers.math.apply(context, options);
        });
    }

    // math Tests - Non-number context
    @Test
    void testMath_NonNumberContext() {
        // Arrange
        String context = "not a number";
        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{"+", 2.0}).build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            HandleBarsHelpers.math.apply(context, options);
        });
    }

    // math Tests - Insufficient parameters
    @Test
    void testMath_InsufficientParameters() {
        // Arrange
        Double context = 5.0;
        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{"+"}).build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            HandleBarsHelpers.math.apply(context, options);
        });
    }

    // Utility method tests
    @Test
    void testGenerator_WithValidInputs() {
        // Act
        String result = HandleBarsHelpers.generator("ABC", 3);

        // Assert
        assertEquals(3, result.length());
        assertTrue(result.matches("[ABC]*"));
    }

    @Test
    void testGenerator_WithZeroLength() {
        // Act
        String result = HandleBarsHelpers.generator("ABC", 0);

        // Assert
        assertEquals(0, result.length());
        assertEquals("", result);
    }

    @Test
    void testAlphaNumericConstant() {
        // Assert
        String alphaNumeric = HandleBarsHelpers.ALPHA_NUMERICS;
        assertEquals(62, alphaNumeric.length()); // 26 uppercase + 26 lowercase + 10 digits
        assertTrue(alphaNumeric.contains("A"));
        assertTrue(alphaNumeric.contains("Z"));
        assertTrue(alphaNumeric.contains("a"));
        assertTrue(alphaNumeric.contains("z"));
        assertTrue(alphaNumeric.contains("0"));
        assertTrue(alphaNumeric.contains("9"));
    }

    // Edge case tests
    @Test
    void testRandomString_WithLargeNumber() throws IOException {
        // Arrange
        Integer context = 1000;

        // Act
        Object result = HandleBarsHelpers.randomString.apply(context, options);

        // Assert
        assertInstanceOf(String.class, result);
        String randomStr = (String) result;
        assertEquals(1000, randomStr.length());
        assertTrue(randomStr.matches("[A-Za-z0-9]*"));
    }

    @Test
    void testRoundNumber_StaticMethod_WithValidInputs() {
        // Arrange
        Double context = 3.14159;

        options = new Options.Builder(mock(Handlebars.class), "test", mock(TagType.class), Context.newContext(1), mock(Template.class)).setParams(new Object[]{2}).build();

        // Act
        Object result = HandleBarsHelpers.roundNumber(context, options, RoundingMode.HALF_UP);

        // Assert
        assertInstanceOf(BigDecimal.class, result);
        BigDecimal rounded = (BigDecimal) result;
        assertEquals(BigDecimal.valueOf(3.14), rounded);
    }

    @Test
    void testRoundNumber_StaticMethod_WithNonNumber() {
        // Arrange
        String context = "not a number";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            HandleBarsHelpers.roundNumber(context, options, RoundingMode.HALF_UP);
        });
    }

    // BiOp enum tests
    @Test
    void testBiOp_FromSymbol_ValidSymbols() {
        assertEquals(HandleBarsHelpers.BiOp.add, HandleBarsHelpers.BiOp.fromSymbol("+"));
        assertEquals(HandleBarsHelpers.BiOp.sub, HandleBarsHelpers.BiOp.fromSymbol("-"));
        assertEquals(HandleBarsHelpers.BiOp.mul, HandleBarsHelpers.BiOp.fromSymbol("*"));
        assertEquals(HandleBarsHelpers.BiOp.div, HandleBarsHelpers.BiOp.fromSymbol("/"));
        assertEquals(HandleBarsHelpers.BiOp.mod, HandleBarsHelpers.BiOp.fromSymbol("%"));
    }

    @Test
    void testBiOp_FromSymbol_InvalidSymbol() {
        assertNull(HandleBarsHelpers.BiOp.fromSymbol("^"));
    }

    // TriOp enum tests
    @Test
    void testTriOp_FromSymbol_ValidSymbol() {
        assertEquals(HandleBarsHelpers.TriOp.div, HandleBarsHelpers.TriOp.fromSymbol("/"));
    }

    @Test
    void testTriOp_FromSymbol_InvalidSymbol() {
        assertNull(HandleBarsHelpers.TriOp.fromSymbol("*"));
    }
}