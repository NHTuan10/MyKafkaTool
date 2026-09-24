package io.github.nhtuan10.mykafkatool.ui.control;


import javafx.beans.NamedArg;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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

        searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.setMaxWidth(200);
        searchField.setVisible(false);

        Label matchLabel = new Label("");
        matchLabel.setVisible(false);
        matchLabel.getStyleClass().add("search-result-label");

        Button prevButton = new Button("▲");
        prevButton.setVisible(false);

        Button nextButton = new Button("▼");
        nextButton.setVisible(false);

        CheckBox selectAllCheckBox = new CheckBox("Select All");
        selectAllCheckBox.setVisible(false);

        HBox hBox = new HBox(searchField, prevButton, nextButton, matchLabel, selectAllCheckBox);
        hBox.setSpacing(5);
        hBox.setMaxHeight(30);
        hBox.setPrefHeight(30);
        hBox.setAlignment(Pos.TOP_RIGHT);
        hBox.setVisible(false);
        StackPane.setAlignment(hBox, Pos.TOP_RIGHT);
        StackPane.setMargin(hBox, new javafx.geometry.Insets(10));

        List<int[]> matchPositions = new ArrayList<>();
        List<Selection> extraSelections = new ArrayList<>();
        AtomicInteger currentIndex = new AtomicInteger(-1);

        Runnable clearExtraSelections = () -> {
            extraSelections.forEach(s -> {
                s.deselect();
                codeArea.removeSelection(s);
            });
            extraSelections.clear();
        };

        Runnable updateExtraSelections = () -> {
            clearExtraSelections.run();
            if (selectAllCheckBox.isSelected()) {
                String query = searchField.getText();
                for (int i = 0; i < matchPositions.size(); i++) {
                    if (i == currentIndex.get()) continue;
                    int[] pos = matchPositions.get(i);
                    Selection<Collection<String>, String, Collection<String>> sel = new SelectionImpl<>(
                            query + "@" + pos[0], codeArea
                    );
                    extraSelections.add(sel);
                    codeArea.addSelection(sel);
                    sel.selectRange(pos[0], pos[1]);
                }
            }
        };

        Runnable navigateToCurrent = () -> {
            int idx = currentIndex.get();
            if (idx >= 0 && idx < matchPositions.size()) {
                int[] pos = matchPositions.get(idx);
                codeArea.selectRange(pos[0], pos[1]);
                codeArea.requestFollowCaret();
                matchLabel.setText((idx + 1) + "/" + matchPositions.size());
                updateExtraSelections.run();
            }
        };

        Runnable showSearchBar = () -> {
            searchField.setVisible(true);
            matchLabel.setVisible(true);
            prevButton.setVisible(true);
            nextButton.setVisible(true);
            selectAllCheckBox.setVisible(true);
            hBox.setVisible(true);
        };

        Runnable hideSearchBar = () -> {
            searchField.setVisible(false);
            matchLabel.setVisible(false);
            prevButton.setVisible(false);
            nextButton.setVisible(false);
            selectAllCheckBox.setVisible(false);
            hBox.setVisible(false);
        };

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            clearExtraSelections.run();
            codeArea.deselect();
            matchPositions.clear();
            currentIndex.set(-1);
            matchLabel.setText("");

            if (newVal != null && !newVal.isEmpty()) {
                String text = codeArea.getText().toLowerCase();
                String query = newVal.toLowerCase();
                int index = text.indexOf(query);
                while (index >= 0) {
                    matchPositions.add(new int[]{index, index + newVal.length()});
                    index = text.indexOf(query, index + newVal.length());
                }

                if (matchPositions.isEmpty()) {
                    matchLabel.setText("0/0");
                } else {
                    currentIndex.set(0);
                    navigateToCurrent.run();
                }
            }
        });

        selectAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> updateExtraSelections.run());

        nextButton.setOnAction(e -> {
            if (!matchPositions.isEmpty()) {
                currentIndex.set((currentIndex.get() + 1) % matchPositions.size());
                navigateToCurrent.run();
            }
        });

        prevButton.setOnAction(e -> {
            if (!matchPositions.isEmpty()) {
                int idx = currentIndex.get() - 1;
                if (idx < 0) idx = matchPositions.size() - 1;
                currentIndex.set(idx);
                navigateToCurrent.run();
            }
        });

        AtomicInteger noEscapePressed = new AtomicInteger();

        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hideSearchBar.run();
                noEscapePressed.incrementAndGet();
                codeArea.requestFocus();
            } else if (event.getCode() == KeyCode.ENTER) {
                if (!matchPositions.isEmpty()) {
                    currentIndex.set((currentIndex.get() + 1) % matchPositions.size());
                    navigateToCurrent.run();
                }
            }
        });

        KeyCombination ctrlF = new KeyCodeCombination(KeyCode.F, KeyCombination.META_DOWN);
        codeArea.setOnKeyPressed(event -> {
            if (ctrlF.match(event)) {
                noEscapePressed.set(0);
                if (searchField.isVisible()) {
                    hideSearchBar.run();
                    codeArea.requestFocus();
                } else {
                    showSearchBar.run();
                    searchField.requestFocus();
                    searchField.selectAll();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                int n = noEscapePressed.incrementAndGet();
                hideSearchBar.run();
                if (n >= 2) {
                    noEscapePressed.set(0);
                    clearExtraSelections.run();
                    codeArea.deselect();
                    matchPositions.clear();
                    currentIndex.set(-1);
                }
            }
        });

        this.getChildren().addAll(new VirtualizedScrollPane<>(codeArea), hBox);
    }
}
