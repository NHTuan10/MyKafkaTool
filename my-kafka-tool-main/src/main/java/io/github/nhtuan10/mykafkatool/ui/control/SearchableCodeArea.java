package io.github.nhtuan10.mykafkatool.ui.control;


import javafx.beans.NamedArg;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
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
import java.util.concurrent.atomic.AtomicInteger;

public class SearchableCodeArea extends StackPane {
    @Getter
    CodeArea codeArea;

    TextField searchField;

    public SearchableCodeArea(@NamedArg(value = "editable", defaultValue = "true") Boolean editable, @NamedArg(value = "wrapText", defaultValue = "true") Boolean wrapText) {
        codeArea = new CodeArea();
        codeArea.setEditable(editable);
        codeArea.setWrapText(wrapText);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
//        codeArea.replaceText(0, 0, "Hello world!\nThis is a RichTextFX CodeArea search example.");

        // Create search text field
        searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.setMaxWidth(200);
        searchField.setVisible(false); // Hidden by default

        Label label = new Label("");
        label.setVisible(false); // Hidden by default
        label.getStyleClass().add("search-result-label");
//        label.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 5px;");
//        label.setBackground(new Background(new BackgroundFill(Color.WHITE, null, Insets.EMPTY)));

//        Background defaultSearchFieldBackground = searchField.get
//        Color color;
//        searchField.applyCss();
//
//        Paint textPaint = (Paint) searchField.queryAccessibleAttribute(AccessibleAttribute.TEXT);
//        if (textPaint instanceof Color) {
//            color = (Color) textPaint;
//        }
        HBox hBox = new HBox(searchField, label);
        hBox.setSpacing(10);
        hBox.setMaxHeight(30);
        hBox.setPrefHeight(30);
        hBox.setAlignment(Pos.TOP_RIGHT);
//        hBox.setPadding(new Insets(0, 5, 0, 0));
        // Position it inside the StackPane at the top right
//        StackPane.setAlignment(searchField, Pos.TOP_RIGHT);
//        StackPane.setMargin(searchField, new javafx.geometry.Insets(10));
        StackPane.setAlignment(hBox, Pos.TOP_RIGHT);
        StackPane.setMargin(hBox, new javafx.geometry.Insets(10));
        List<Selection> selectionList = new ArrayList<>();
        // Search logic on text change
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                clearAllSelections(selectionList);
                label.setText("");
//                searchField.setBackground(defaultSearchFieldBackground);
                if (!newVal.isEmpty()) {
                    String text = codeArea.getText().toLowerCase();
                    int index = text.indexOf(newVal.toLowerCase());
                    if (index < 0) {
                        label.setText("0 Match");
//                        searchField.setBackground(Background.fill(javafx.scene.paint.Color.RED));
                        return;
                    }
                    int noMatch = 0;
                    while (index >= 0) {
                        noMatch++;
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
                        if (selectionList.size() == 1) {
                            codeArea.requestFollowCaret();
                        }
                        index = text.indexOf(newVal.toLowerCase(), endIndex);
                    }
                    label.setText(noMatch + " Match");
                }
            }
        });

        AtomicInteger noEscapePressed = new AtomicInteger();

        // Hide search box on ESC, or go to next on ENTER
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                searchField.setVisible(false);
                label.setVisible(false);
                noEscapePressed.incrementAndGet();
                codeArea.requestFocus();
            }
        });

        // Toggle search box with Ctrl+F shortcut on the CodeArea
        KeyCombination ctrlF = new KeyCodeCombination(KeyCode.F, KeyCombination.META_DOWN);
        codeArea.setOnKeyPressed(event -> {
            if (ctrlF.match(event)) {
                noEscapePressed.set(0);
                searchField.setVisible(!searchField.isVisible());
                label.setVisible(!label.isVisible());
                if (searchField.isVisible()) {
                    searchField.requestFocus();
                    searchField.selectAll();
                } else {
                    codeArea.requestFocus();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                int n = noEscapePressed.incrementAndGet();
                searchField.setVisible(false);
                if (n >= 2) {
                    noEscapePressed.set(0);
                    clearAllSelections(selectionList);
                }
            }
        });

        this.getChildren().addAll(new VirtualizedScrollPane<>(codeArea), hBox);
        // Combine into a StackPane root
//        StackPane root = new StackPane(codeArea, searchField);
    }

    private void clearAllSelections(List<Selection> selectionList) {
        selectionList.forEach(s -> {
            s.deselect();
            codeArea.removeSelection(s);
        });
        selectionList.clear();
    }
}
