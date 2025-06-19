package io.github.nhtuan10.mykafkatool.ui.control;

import javafx.beans.NamedArg;
import javafx.scene.control.TextField;

public class PositiveIntTextField extends TextField {

    public PositiveIntTextField(@NamedArg("default") int defaultInt) {
        super();
        if (defaultInt > 0) {
            setText(String.valueOf(defaultInt));
            textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d+")) {
                    setText(newValue.replaceAll("\\D", ""));
                    if (newValue.isEmpty() || Integer.parseInt(newValue) == 0) {
                        setText(String.valueOf(defaultInt));
                    }
                }
            });
        }

    }
}
