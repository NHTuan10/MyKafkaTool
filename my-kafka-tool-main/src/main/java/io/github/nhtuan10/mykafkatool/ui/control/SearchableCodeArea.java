package io.github.nhtuan10.mykafkatool.ui.control;


import javafx.beans.NamedArg;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.Selection;
import org.fxmisc.richtext.SelectionImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SearchableCodeArea extends StackPane {
    @Getter
    CodeArea codeArea;

    TextField searchField;

    public SearchableCodeArea(@NamedArg(value = "editable", defaultValue = "true") Boolean editable, @NamedArg(value = "wrapText", defaultValue = "true") Boolean wrapText) {
        codeArea = new CodeArea();
        codeArea.setEditable(editable);
        codeArea.setWrapText(wrapText);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.replaceText(0, 0, "Hello world!\nThis is a RichTextFX CodeArea search example.");

        // Create search text field
        searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.setMaxWidth(200);
        searchField.setVisible(false); // Hidden by default

        // Position it inside the StackPane at the top right
        StackPane.setAlignment(searchField, Pos.TOP_RIGHT);
        StackPane.setMargin(searchField, new javafx.geometry.Insets(10));
        List<Selection> selectionList = new ArrayList<>();
        // Search logic on text change
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectionList.forEach(s -> {
                    s.deselect();
                    codeArea.removeSelection(s);
                });
                selectionList.clear();
                if (!newVal.isEmpty()) {
                    String text = codeArea.getText().toLowerCase();
                    int index = text.indexOf(newVal.toLowerCase());
                    while (index >= 0) {
                        int endIndex = index + newVal.length();
                        //                    codeArea.selectRange(index, endIndex);
                        Selection<Collection<String>, String, Collection<String>> selection = new SelectionImpl<>(newVal + "@" + index, codeArea
                                //                            ,path -> {
                                //                        // make rendered selection path look like a yellow highlighter
                                //                        path.setStrokeWidth(0);
                                //                        path.setFill(Color.YELLOW);
                                //                    }
                        );
                        selectionList.add(selection);
                        codeArea.addSelection(selection);
                        selection.selectRange(index, endIndex);
                        codeArea.requestFollowCaret();
                        index = text.indexOf(newVal.toLowerCase(), endIndex);
                    }
                }
            }
        });

        // Hide search box on ESC, or go to next on ENTER
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                searchField.setVisible(false);
                codeArea.requestFocus();
            }
        });

        // Toggle search box with Ctrl+F shortcut on the CodeArea
        KeyCombination ctrlF = new KeyCodeCombination(KeyCode.F, KeyCombination.META_DOWN);
        codeArea.setOnKeyPressed(event -> {
            if (ctrlF.match(event)) {
                searchField.setVisible(!searchField.isVisible());
                if (searchField.isVisible()) {
                    searchField.requestFocus();
                    searchField.selectAll();
                } else {
                    codeArea.requestFocus();
                }
                event.consume();
            }
        });

        this.getChildren().addAll(new VirtualizedScrollPane<>(codeArea), searchField);
        // Combine into a StackPane root
//        StackPane root = new StackPane(codeArea, searchField);
    }
}
