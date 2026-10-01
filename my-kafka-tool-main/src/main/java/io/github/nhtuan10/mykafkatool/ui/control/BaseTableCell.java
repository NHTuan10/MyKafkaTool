package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.util.TableViewConfigurer;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;

public class BaseTableCell<S, T> extends TableCell<S, T> {
    private StageHolder stageHolder;
    private final Button copyButton;
    private final Label textLabel;
    private final StackPane cellContent;

    public BaseTableCell(StageHolder stageHolder, TableViewConfigurer.TableViewConfiguration<S> tableViewConfiguration) {
        super();
        copyButton = new Button("⧉");
        copyButton.setStyle("-fx-padding: 0 3 0 3; -fx-font-size: 10;");
        copyButton.setTooltip(new Tooltip("Copy"));
        copyButton.setOnAction(e -> {
            T item = getItem();
            ViewUtils.copyTextToClipboard(item != null ? item.toString() : "");
        });

        textLabel = new Label();
        textLabel.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(textLabel, Pos.TOP_LEFT);
        StackPane.setAlignment(copyButton, Pos.TOP_RIGHT);
        cellContent = new StackPane(textLabel, copyButton);
        cellContent.prefWidthProperty().bind(widthProperty());
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
            setGraphic(null);
        } else {
            String text = item != null ? item.toString() : null;
            textLabel.setText(text);
            setTooltip(new Tooltip(text));
            setText(null);
            setGraphic(cellContent);
        }
    }
}
