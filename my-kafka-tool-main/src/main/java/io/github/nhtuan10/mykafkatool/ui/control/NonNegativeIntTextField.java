package io.github.nhtuan10.mykafkatool.ui.control;

import javafx.beans.NamedArg;
import javafx.scene.control.TextField;
import org.apache.commons.lang3.StringUtils;

public class NonNegativeIntTextField extends TextField {
    public NonNegativeIntTextField() {
        this(0, true);
    }

    public NonNegativeIntTextField(@NamedArg(value = "default", defaultValue = "0") int defaultInt, @NamedArg(value = "isZeroAllowed", defaultValue = "true") boolean isZeroAllowed) {
        super();
        if (isValidDefaultValue(defaultInt, isZeroAllowed)) {
            setText(String.valueOf(defaultInt));
            textProperty().addListener((observable, oldValue, newValue) -> {
                nonNegIntChangeListener(defaultInt, newValue);
            });
        } else {
            throw new IllegalArgumentException("defaultInt must be >= 0");
        }
    }

    private boolean isValidDefaultValue(int defaultInt, boolean isZeroAllowed) {
        return (isZeroAllowed && defaultInt >= 0) || (!isZeroAllowed && defaultInt > 0);
    }

    private void nonNegIntChangeListener(int defaultInt, String newValue) {
        String text = newValue;
        if (!newValue.matches("\\d+")) {
            text = newValue.replaceAll("\\D", "");
        }
        if (StringUtils.isNotBlank(text) && Integer.parseInt(text) == 0) {
            text = String.valueOf(defaultInt);
        }
        setText(text);
    }
}
