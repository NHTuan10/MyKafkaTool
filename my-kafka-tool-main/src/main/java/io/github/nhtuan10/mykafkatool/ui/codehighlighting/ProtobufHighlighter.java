package io.github.nhtuan10.mykafkatool.ui.codehighlighting;

import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.model.StyleSpans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@AppScoped
public class ProtobufHighlighter implements Highlighter {

    public static final String KEYWORD = "code-keyword";
    public static final String COMMENT = "code-comment";
    public static final String STRING = "code-string";
    public static final String NUMBER = "code-number";
    public static final String TYPE = "code-type";
    public static final String FIELD_NAME = "protobuf-field-name";
    public static final String OPTION_NAME = "protobuf-option-name";

    // Protobuf keywords based on the official specification
    private static final Set<String> KEYWORDS = Set.of(
            "syntax", "import", "weak", "public", "package", "option",
            "message", "group", "oneof", "optional", "required", "repeated",
            "enum", "service", "rpc", "stream", "returns",
            "extend", "extensions", "to", "max", "reserved",
            "true", "false", "inf", "nan"
    );

    // Built-in scalar types
    private static final Set<String> TYPES = Set.of(
            "double", "float", "int32", "int64", "uint32", "uint64",
            "sint32", "sint64", "fixed32", "fixed64", "sfixed32", "sfixed64",
            "bool", "string", "bytes"
    );

    // Well-known types
    private static final Set<String> WELL_KNOWN_TYPES = Set.of(
            "google.protobuf.Any", "google.protobuf.Timestamp", "google.protobuf.Duration",
            "google.protobuf.Empty", "google.protobuf.FieldMask", "google.protobuf.Struct",
            "google.protobuf.Value", "google.protobuf.ListValue", "google.protobuf.NullValue",
            "google.protobuf.BoolValue", "google.protobuf.Int32Value", "google.protobuf.UInt32Value",
            "google.protobuf.Int64Value", "google.protobuf.UInt64Value", "google.protobuf.FloatValue",
            "google.protobuf.DoubleValue", "google.protobuf.StringValue", "google.protobuf.BytesValue"
    );

    // Regex patterns for different protobuf elements
    private static final Pattern COMMENT_PATTERN = Pattern.compile("//[^\r\n]*|/\\*[\\s\\S]*?\\*/");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b|\\b0[xX][0-9a-fA-F]+\\b|\\b0[0-7]+\\b");
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("\\b(" + String.join("|", KEYWORDS) + ")\\b");
    private static final Pattern TYPE_PATTERN = Pattern.compile("\\b(" + String.join("|", TYPES) + ")\\b");
    private static final Pattern WELL_KNOWN_TYPE_PATTERN = Pattern.compile("\\b(" + String.join("|", WELL_KNOWN_TYPES.stream().map(Pattern::quote).toArray(String[]::new)) + ")\\b");
    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*\\d+");
    private static final Pattern OPTION_NAME_PATTERN = Pattern.compile("option\\s+([a-zA-Z_][a-zA-Z0-9_.]*(?:\\([a-zA-Z_][a-zA-Z0-9_.]*\\))?)");

    @Inject
    public ProtobufHighlighter() {
        log.debug("ProtobufHighlighter initialized");
    }

    public StyleSpans<Collection<String>> highlight(String code) {
        List<Match> matches = new ArrayList<>();

        try {
            // Find comments
            addMatches(matches, COMMENT_PATTERN, code, COMMENT);

            // Find strings
            addMatches(matches, STRING_PATTERN, code, STRING);

            // Find numbers
            addMatches(matches, NUMBER_PATTERN, code, NUMBER);

            // Find keywords
            addMatches(matches, KEYWORD_PATTERN, code, KEYWORD);

            // Find built-in types
            addMatches(matches, TYPE_PATTERN, code, TYPE);

            // Find well-known types
            addMatches(matches, WELL_KNOWN_TYPE_PATTERN, code, TYPE);

            // Find field names (identifier = number pattern)
            Matcher fieldMatcher = FIELD_NAME_PATTERN.matcher(code);
            while (fieldMatcher.find()) {
                String fieldName = fieldMatcher.group(1);
                int start = fieldMatcher.start(1);
                int end = fieldMatcher.end(1);
                matches.add(new Match(FIELD_NAME, start, end));
            }

            // Find option names
            Matcher optionMatcher = OPTION_NAME_PATTERN.matcher(code);
            while (optionMatcher.find()) {
                String optionName = fieldMatcher.group(1);
                int start = optionMatcher.start(1);
                int end = optionMatcher.end(1);
                matches.add(new Match(OPTION_NAME, start, end));
            }

        } catch (Exception e) {
            log.warn("Error during protobuf syntax highlighting", e);
        }

        // Sort matches by start position to handle overlapping matches
        matches.sort(Match::compareTo);

        // Remove overlapping matches (prefer earlier matches)
        List<Match> filteredMatches = new ArrayList<>();
        int lastEnd = 0;
        for (Match match : matches) {
            if (match.start >= lastEnd) {
                filteredMatches.add(match);
                lastEnd = match.end;
            }
        }
        return Highlighter.buildStyleSpans(code, filteredMatches);
    }

    private void addMatches(List<Match> matches, Pattern pattern, String code, String styleClass) {
        Matcher matcher = pattern.matcher(code);
        while (matcher.find()) {
            matches.add(new Match(styleClass, matcher.start(), matcher.end()));
        }
    }
}