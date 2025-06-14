package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.util.TableViewConfigurer;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class EditingTableCell<T> extends DragSelectionCell<T, String> {

    private TextField textField;

    private String oldValue;

    public EditingTableCell(StageHolder stageHolder, TableViewConfigurer.TableViewConfiguration tableViewConfiguration) {
        super(stageHolder, tableViewConfiguration);
    }

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            super.startEdit();
            oldValue = getItem();
            createTextField();
            setText(null);
            setGraphic(textField);
            textField.selectAll();
        }
    }

    @Override
    public void cancelEdit() {
//        super.cancelEdit();

        setText(getItem());
        setGraphic(null);
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(getString());
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(getString());
                setGraphic(null);
            }
        }
    }


    private void createTextField() {
        textField = new TextField(getString());
        textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
        textField.focusedProperty().addListener((arg0, arg1, arg2) -> {
            if (!arg2) {
                commitEdit(textField.getText());
            }
        });

        textField.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                textField.setText(oldValue);
            } else if (e.getCode().equals(KeyCode.ENTER)) {
                commitEdit(textField.getText());
            }
        });
    }

    private String getString() {
        return getItem() == null ? "" : getItem();
    }
}