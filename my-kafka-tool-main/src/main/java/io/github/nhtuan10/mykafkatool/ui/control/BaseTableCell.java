package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.util.TableViewConfigurer;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;

public class BaseTableCell<S, T> extends TableCell<S, T> {
    private StageHolder stageHolder;

    public BaseTableCell(StageHolder stageHolder, TableViewConfigurer.TableViewConfiguration<S> tableViewConfiguration) {
        super();
        this.stageHolder = stageHolder;
        setOnDragDetected(event -> {
            startFullDrag();
            getTableColumn().getTableView().getSelectionModel().select(getIndex(), getTableColumn());
        });

        setOnMouseDragEntered(event -> getTableColumn().getTableView().getSelectionModel().select(getIndex(), getTableColumn()));
//        ContextMenu menu = new ContextMenu(new MenuItem());
//        menu.setOnShowing(e -> {
//            String cellText = String.valueOf(getTableColumn().getCellData(getIndex()));
//            menu.getItems().setAll(TableViewConfigurer.getTableContextMenuItems(getTableView(), cellText, this.stageHolder, tableViewConfiguration));
////                getTableView().setContextMenu(menu);
////            menu.show(this, e.getScreenX(), e.getScreenY());
//        });
//        getTableView().setContextMenu(menu);
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                String cellText = String.valueOf(getTableColumn().getCellData(getIndex()));

                ContextMenu menu = new ContextMenu();
                menu.getItems().addAll(tableViewConfiguration.extraContextMenuItems());
                menu.getItems().addAll(TableViewConfigurer.getTableContextMenuItems(getTableView(), cellText, this.stageHolder, tableViewConfiguration));
//                getTableView().setContextMenu(menu);
                menu.show(this, e.getScreenX(), e.getScreenY());
            }

        });

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
