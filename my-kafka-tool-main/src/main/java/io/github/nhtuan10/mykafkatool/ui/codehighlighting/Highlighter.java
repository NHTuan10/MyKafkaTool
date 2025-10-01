package io.github.nhtuan10.mykafkatool.ui.codehighlighting;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface Highlighter {
    String NORMAL_TEXT = "normal-text";
    String CODE_STRING = "code-string";
    String CODE_NUMBER = "code-number";

    static StyleSpans<Collection<String>> buildStyleSpans(String code, List<Match> matches) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastPos = 0;
        for (Match match : matches) {
            // Fill the gaps, since Style Spans need to be contiguous.
            if (match.start > lastPos) {
                int length = match.start - lastPos;
                spansBuilder.add(List.of(NORMAL_TEXT), length);
            }

            int length = match.end - match.start;
            spansBuilder.add(Collections.singleton(match.kind), length);
            lastPos = match.end;
        }

        // Add remaining normal text
        if (lastPos < code.length()) {
            spansBuilder.add(List.of(NORMAL_TEXT), code.length() - lastPos);
        }

        return spansBuilder.create();
    }

    StyleSpans<Collection<String>> highlight(String code);
}
