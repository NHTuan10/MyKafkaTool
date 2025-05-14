package com.example.mytool.ui;

import com.example.mytool.constant.AppConstant;
import com.example.mytool.serdes.SerDesHelper;
import com.example.mytool.ui.control.EditingTableCell;
import com.example.mytool.ui.util.ViewUtil;
import javafx.collections.FXCollections;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

@Slf4j
public class TableViewConfigurer {

    public static <S> TableView<S> configureTableView(Class<S> clazz, String fxId, Stage stage) {
        TableView<S> tableView = (TableView<S>) stage.getScene().lookup("#" + fxId);
        return configureTableView(clazz, tableView);
    }


    public static <S> TableView<S> configureTableView(Class<S> clazz, TableView<S> tableView) {
        List<String> fieldNames = ViewUtil.getPropertyFieldNamesFromTableItem(clazz);
        IntStream.range(0, fieldNames.size()).forEach(i -> {
            TableColumn<S, ?> tableColumn = tableView.getColumns().get(i);
            tableColumn.setCellValueFactory(new PropertyValueFactory<>(fieldNames.get(i)));
            tableColumn.setCellFactory((callback) -> new ViewUtil.DragSelectionCell<>());
        });
        // Enable copy by Ctrl + C or by right click -> Copy
        ViewUtil.enableCopyDataFromTableToClipboard(tableView);

        return tableView;
    }

    public static void configureEditableKeyValueTable(TableView<UIPropertyTableItem> headerTable) {
        Callback<TableColumn<UIPropertyTableItem, String>,
                TableCell<UIPropertyTableItem, String>> cellFactory
                = (TableColumn<UIPropertyTableItem, String> p) -> new EditingTableCell<>();

        TableColumn<UIPropertyTableItem, String> nameColumn = (TableColumn<UIPropertyTableItem, String>) headerTable.getColumns().getFirst();
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

        TableColumn<UIPropertyTableItem, String> valueColumn = (TableColumn<UIPropertyTableItem, String>) headerTable.getColumns().get(1);

        valueColumn.setCellFactory(cellFactory);
        valueColumn.setOnEditCommit(event -> {
            // Update the model when editing is committed
            event.getTableView().getItems().get(
                    event.getTablePosition().getRow()).setValue(event.getNewValue());
        });
    }

    public static void configureMessageTable(TableView<KafkaMessageTableItem> messageTable, SerDesHelper serDesHelper) {
        TableViewConfigurer.configureTableView(KafkaMessageTableItem.class, messageTable);
        messageTable.setRowFactory(tv -> {
            TableRow<KafkaMessageTableItem> row = new TableRow<>() {
                @Override
                protected void updateItem(KafkaMessageTableItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty && item != null) {
                        if (item.isErrorItem()) {
                            setStyle("-fx-background-color: lightcoral; -fx-border-color: transparent transparent lightgray transparent;");
                        } else {
                            setStyle("-fx-background-color: transparent; -fx-border-color: transparent transparent lightgray transparent;");
                        }
                    } else {
                        setStyle("");
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    KafkaMessageTableItem rowData = row.getItem();
                    log.debug("Double click on: {}", rowData.getKey());
                    Map<String, Object> msgModalFieldMap = Map.of(
                            "serDesHelper", serDesHelper,
                            "keyTextArea", rowData.getKey(),
                            "valueTextArea", rowData.getValue(),
                            "valueContentTypeComboBox", FXCollections.observableArrayList(rowData.getValueContentType()),
                            "headerTable",
                            FXCollections.observableArrayList(
                                    Arrays.stream(rowData.getHeaders().toArray()).map(header -> new UIPropertyTableItem(header.key(), new String(header.value()))).toList()));
                    try {
                        ViewUtil.showPopUpModal(AppConstant.ADD_MESSAGE_MODAL_FXML, "View Message", new AtomicReference<>(), msgModalFieldMap, false);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

//                    System.out.println("Double click on: "+rowData.getKey());
                }
            });
            return row;
        });
//        configureErrorMessageRow((TableColumn<KafkaMessageTableItem, Object>) messageTable.getColumns().get(3));
    }

//    public static void initTableView(Stage stage) {
//        TableView<KafkaMessageTableItem> kafkaMsgTable = TableViewConfigurer.configureTableView(KafkaMessageTableItem.class, "messageTable", stage);
//        TableViewConfigurer.configureTableView(ConsumerGroupOffsetTableItem.class, "consumerGroupOffsetTable", stage);
//        TableViewConfigurer.configureTableView(KafkaPartitionsTableItem.class, "kafkaPartitionsTable", stage);
//        TableViewConfigurer.configureTableView(UIPropertyItem.class, "topicConfigTable", stage);
//        // Use a change listener to respond to a selection within
//        // a tree view
//        clusterTree.getSelectionModel().selectedItemProperty().addListener((ChangeListener<TreeItem<String>>) (changed, oldVal, newVal) -> {
//
//
//        });


//        TreeView<String> tree = new TreeView<String> (rootItem);

//        clusterTree.setEditable(true);
//        clusterTree.setCellFactory((Callback<TreeView<String>, TreeCell<String>>) p -> new TextFieldTreeCellImpl());
//    }
}
