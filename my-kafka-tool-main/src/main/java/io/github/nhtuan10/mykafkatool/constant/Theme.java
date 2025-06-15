package io.github.nhtuan10.mykafkatool.constant;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public enum Theme {
    LIGHT(new PrimerLight().getUserAgentStylesheet(), List.of(UIStyleConstant.LIGHT_STYLE_CSS_FILE)),
    DARK(new PrimerDark().getUserAgentStylesheet(), List.of(UIStyleConstant.DARK_STYLE_CSS_FILE));

    @Getter
    private final String userAgentStyleSheet;

    @Getter
    private final List<String> styleSheets;

    Theme(String userAgentStyleSheet) {
        this(userAgentStyleSheet, new ArrayList<>());
    }

}
