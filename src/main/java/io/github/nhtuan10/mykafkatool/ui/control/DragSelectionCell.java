package io.github.nhtuan10.mykafkatool.ui.control;

import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;

public class DragSelectionCell<S, T> extends TableCell<S, T> {

    public DragSelectionCell() {
        setOnDragDetected(event -> {
            startFullDrag();
            getTableColumn().getTableView().getSelectionModel().select(getIndex(), getTableColumn());
        });

        setOnMouseDragEntered(event -> getTableColumn().getTableView().getSelectionModel().select(getIndex(), getTableColumn()));
    }

    @Override
    public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
        } else {
            setText(item != null ? item.toString() : null);
            setTooltip(new Tooltip(item != null ? item.toString() : null));
        }
    }
}
