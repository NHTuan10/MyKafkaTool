package io.github.nhtuan10.mykafkatool.ui.control;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;

public class CopyTextMenuItem extends MenuItem {
    public CopyTextMenuItem() {
    }

    public CopyTextMenuItem(String text) {
        super(text);
    }

    public CopyTextMenuItem(String text, Node graphic) {
        super(text, graphic);
    }
}
