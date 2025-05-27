package io.github.nhtuan10.mykafkatool.ui;

import io.github.nhtuan10.mykafkatool.ui.control.EditingTableCell;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
public class TableViewConfigurer {

    public static <S> TableView<S> configureTableView(Class<S> clazz, String fxId, Stage stage, StageHolder stageHolder) {
        TableView<S> tableView = (TableView<S>) stage.getScene().lookup("#" + fxId);
        return configureTableView(clazz, tableView, stageHolder);
    }

//    public static <S> TableView<S> configureTableView(Class<S> clazz, TableView<S> tableView) {
//        return configureTableView(clazz, tableView);
//    }


    public static <S> TableView<S> configureTableView(Class<S> clazz, TableView<S> tableView, @NonNull StageHolder stageHolder) {
        TableColumn<S, S> numberCol = buildIndexTableColumn();
        tableView.getColumns().addFirst(numberCol);

        List<String> fieldNames = ViewUtil.getPropertyFieldNamesFromTableItem(clazz);
        IntStream.range(0, fieldNames.size()).forEach(i -> {
            TableColumn<S, ?> tableColumn = tableView.getColumns().get(i + 1);
            tableColumn.setCellValueFactory(new PropertyValueFactory<>(fieldNames.get(i)));
            tableColumn.setCellFactory((column) -> new ViewUtil.DragSelectionCell<>());
        });
        // Enable copy by Ctrl + C or by right click -> Copy
        ViewUtil.enableCopyAndExportDataFromTable(tableView, SelectionMode.MULTIPLE, stageHolder);

        //Set the auto-resize policy
//        tableView.itemsProperty().addListener((observable, oldValue, newValue) -> {
//            autoResizeColumns(tableView);
//
//        });
//        tableView.getItems().addListener((ListChangeListener<? super S>) (change) -> {
//            autoResizeColumns(tableView);
//        });
        return tableView;
    }

    private static <S> void autoResizeColumns(TableView<S> tableView) {
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tableView.getColumns().forEach((column) ->
        {
            //Minimal width = columnheader
            Text t = new Text(column.getText());
            double max = t.getLayoutBounds().getWidth();
            for (int i = 0; i < tableView.getItems().size(); i++) {
                //cell must not be empty
                if (column.getCellData(i) != null) {
                    t = new Text(column.getCellData(i).toString());
                    double calcwidth = t.getLayoutBounds().getWidth();
                    //remember new max-width
                    if (calcwidth > max) {
                        max = calcwidth;
                    }
                }
            }
            //set the new max-widht with some extra space
            column.setPrefWidth(Math.min(500, max) + 10.0d);
        });
    }

    private static <S> TableColumn<S, S> buildIndexTableColumn() {
        TableColumn<S, S> numberCol = new TableColumn<>("#");
        numberCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper(p.getValue()));

        numberCol.setCellFactory((column) -> new TableCell<S, S>() {
            @Override
            protected void updateItem(S item, boolean empty) {
                super.updateItem(item, empty);

                if (this.getTableRow() != null && item != null) {
                    setText(String.valueOf(this.getTableRow().getIndex() + 1));
                    setStyle("-fx-text-fill: darkgray");
                } else {
                    setText("");
                }
            }
        });
        numberCol.setSortable(false);
        return numberCol;
    }


//    public static void configureEditableTableCell(TableView<UIPropertyTableItem> headerTable) {
//        Callback<TableColumn<UIPropertyTableItem, String>,
//                TableCell<UIPropertyTableItem, String>> cellFactory
//                = (TableColumn<UIPropertyTableItem, String> p) -> new EditingTableCell<>();
//
//        TableColumn<UIPropertyTableItem, String> nameColumn = (TableColumn<UIPropertyTableItem, String>) headerTable.getColumns().getFirst();
////            nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
////            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
////            nameColumn.setCellFactory((tableColumn)-> new EditingTableCell()); // Use TextField for editing
//        nameColumn.setCellFactory(cellFactory); // Use TextField for editing
//        nameColumn.setOnEditCommit(event -> {
//            // Update the model when editing is committed
////                UIPropertyItem row = event.getRowValue();
////                row.setName(event.getNewValue());
//            event.getTableView().getItems().get(
//                    event.getTablePosition().getRow()).setName(event.getNewValue());
//        });

    /// /            nameColumn.setOnEditCancel(event -> {
    /// /                event.getRowValue();
    /// /            });
//
//        TableColumn<UIPropertyTableItem, String> valueColumn = (TableColumn<UIPropertyTableItem, String>) headerTable.getColumns().get(1);
//
//        valueColumn.setCellFactory(cellFactory);
//        valueColumn.setOnEditCommit(event -> {
//            // Update the model when editing is committed
//            event.getTableView().getItems().get(
//                    event.getTablePosition().getRow()).setValue(event.getNewValue());
//        });
//    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <S> void configureEditableTableCell(TableView<S> tableView, Class<S> tableItemClass) {
        Callback<TableColumn<S, String>,
                TableCell<S, String>> cellFactory
                = (TableColumn<S, String> p) -> new EditingTableCell<>();
        List<Field> fields = ViewUtil.getPropertyFieldFromTableItem(tableItemClass);
        IntStream.range(0, fields.size()).forEach(i -> {
            Field field = fields.get(i);
            TableColumn<S, String> tableColumn = (TableColumn<S, String>) tableView.getColumns().get(i + 1);
            tableColumn.setCellFactory(cellFactory);
            tableColumn.setOnEditCommit(event -> {
                // Update the model when editing is committed
//                UIPropertyItem row = event.getRowValue();
//                row.setName(event.getNewValue());
                S row = event.getTableView().getItems().get(
                        event.getTablePosition().getRow());
                field.setAccessible(true);
                try {
                    Property property = (Property) field.get(row);
                    property.setValue(event.getNewValue());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
        });
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
