package io.github.nhtuan10.mykafkatool.ui.codehighlighting;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import jakarta.inject.Inject;
import org.fxmisc.richtext.model.StyleSpans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@AppScoped
public class JsonHighlighter implements Highlighter {

    private final JsonFactory jsonFactory;

    @Inject
    public JsonHighlighter() {
        jsonFactory = new JsonFactory();
    }

    @Override
    public StyleSpans<Collection<String>> highlight(String code) {
        List<Match> matches = new ArrayList<>();

        try {
            JsonParser parser = jsonFactory.createParser(code);
            while (!parser.isClosed()) {
                JsonToken jsonToken = parser.nextToken();
                int start = (int) parser.getTokenLocation().getCharOffset();
                int end = start + parser.getTextLength();

                // Because getTextLength() does contain the surrounding ""
                if (jsonToken == JsonToken.VALUE_STRING || jsonToken == JsonToken.FIELD_NAME) {
                    end += 2;
                }

                String className = jsonTokenToClassName(jsonToken);
                if (!className.isEmpty()) {
                    Match m = new Match(className, start, end);
                    matches.add(m);
                }
            }
        } catch (IOException e) {
        }

        return Highlighter.buildStyleSpans(code, matches);
    }

    public static String jsonTokenToClassName(JsonToken jsonToken) {
        if (jsonToken == null) {
            return "";
        }
        switch (jsonToken) {
            case FIELD_NAME:
                return "json-property";
            case START_OBJECT:
            case END_OBJECT:
                return "json-start-or-end-object";
            case START_ARRAY:
            case END_ARRAY:
                return "json-start-or-end-array";
            case VALUE_STRING:
                return CODE_STRING;
            case VALUE_NUMBER_FLOAT:
            case VALUE_NUMBER_INT:
                return CODE_NUMBER;
            default:
                return NORMAL_TEXT;
        }
    }

}
