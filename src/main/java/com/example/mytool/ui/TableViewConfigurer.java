package com.example.mytool.ui;

import com.example.mytool.ui.util.EditingTableCell;
import com.example.mytool.ui.util.ViewUtil;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
public class TableViewConfigurer {

    public static <T> TableView<T> configureTableView(Class<T> clazz, String fxId, Stage stage) {
        TableView<T> tableView = (TableView<T>) stage.getScene().lookup("#" + fxId);
        return configureTableView(clazz, tableView);
    }


    public static <T> TableView<T> configureTableView(Class<T> clazz, TableView<T> tableView) {
        List<String> fieldNames = ViewUtil.getPropertyFieldNamesFromTableItem(clazz);
        IntStream.range(0, fieldNames.size()).forEach(i -> {
            tableView.getColumns().get(i).setCellValueFactory(new PropertyValueFactory<>(fieldNames.get(i)));
        });
        return tableView;
//        return kafkaPartitionsTableView;
    }

    public static void configureEditableKeyValueTable(TableView<UIPropertyItem> headerTable) {
        Callback<TableColumn<UIPropertyItem, String>,
                TableCell<UIPropertyItem, String>> cellFactory
                = (TableColumn<UIPropertyItem, String> p) -> new EditingTableCell();

        TableColumn<UIPropertyItem, String> nameColumn = (TableColumn<UIPropertyItem, String>) headerTable.getColumns().get(0);
//            nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
//            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
//            nameColumn.setCellFactory((tableColumn)-> new EditingTableCell()); // Use TextField for editing
        nameColumn.setCellFactory(cellFactory); // Use TextField for editing
        nameColumn.setOnEditCommit(event -> {
            // Update the model when editing is committed
//                UIPropertyItem row = event.getRowValue();
//                row.setName(event.getNewValue());
            event.getTableView().getItems().get(
                    event.getTablePosition().getRow()).setName(event.getNewValue());
        });
//            nameColumn.setOnEditCancel(event -> {
//                event.getRowValue();
//            });

        TableColumn<UIPropertyItem, String> valueColumn = (TableColumn<UIPropertyItem, String>) headerTable.getColumns().get(1);

//            valueColumn.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
//            valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
//            valueColumn.setCellFactory(TextFieldTableCell.forTableColumn()); // Use TextField for editing
//            valueColumn.setCellFactory((tableColumn)-> new EditingTableCell());
        valueColumn.setCellFactory(cellFactory);
        valueColumn.setOnEditCommit(event -> {
            // Update the model when editing is committed
//                UIPropertyItem row = event.getRowValue();
//                row.setValue(event.getNewValue());
            event.getTableView().getItems().get(
                    event.getTablePosition().getRow()).setValue(event.getNewValue());
        });
    }

//    public static void configureTopicConfigTableView(Stage stage) {
//        TableView<KafkaMessageTableItem> topicConfigTable = (TableView<KafkaMessageTableItem>) stage.getScene().lookup("#topicConfigTable");
//        TableColumn<KafkaMessageTableItem, Long> partition = (TableColumn<KafkaMessageTableItem, Long>) topicConfigTable.getColumns().get(0);
//        partition.setCellValueFactory(new PropertyValueFactory<>("name"));
//
//        TableColumn<KafkaMessageTableItem, Long> offset = (TableColumn<KafkaMessageTableItem, Long>) topicConfigTable.getColumns().get(1);
//        offset.setCellValueFactory(new PropertyValueFactory<>("value"));
//
//    }

//    public static void initTableView(Stage stage) {
//        TableView<KafkaMessageTableItem> kafkaMsgTable = TableViewConfigurer.configureTableView(KafkaMessageTableItem.class, "messageTable", stage);
//        TableViewConfigurer.configureTableView(ConsumerGroupOffsetTableItem.class, "consumerGroupOffsetTable", stage);
//        TableViewConfigurer.configureTableView(KafkaPartitionsTableItem.class, "kafkaPartitionsTable", stage);
//        TableViewConfigurer.configureTableView(UIPropertyItem.class, "topicConfigTable", stage);
//        // Use a change listener to respond to a selection within
//        // a tree view
////        clusterTree.getSelectionModel().selectedItemProperty().addListener((ChangeListener<TreeItem<String>>) (changed, oldVal, newVal) -> {
////
////
////        });
//
//
////        TreeView<String> tree = new TreeView<String> (rootItem);
//
////        clusterTree.setEditable(true);
////        clusterTree.setCellFactory((Callback<TreeView<String>, TreeCell<String>>) p -> new TextFieldTreeCellImpl());
//    }
}
