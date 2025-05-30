package io.github.nhtuan10.mykafkatool.ui;

import io.github.nhtuan10.mykafkatool.ui.control.DragSelectionCell;
import io.github.nhtuan10.mykafkatool.ui.control.EditingTableCell;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.input.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
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

        List<String> fieldNames = getPropertyFieldNamesFromTableItem(clazz);
        IntStream.range(0, fieldNames.size()).forEach(i -> {
            TableColumn<S, ?> tableColumn = tableView.getColumns().get(i + 1);
            tableColumn.setId(fieldNames.get(i) + "Col");

//            Platform.runLater(() -> {

//            });
//            tableColumn.setText(null);
//            Label label = new Label();
//            label.setTooltip(new Tooltip(columnHeader));
//            tableColumn.setGraphic(label);
            tableColumn.setCellValueFactory(new PropertyValueFactory<>(fieldNames.get(i)));
            tableColumn.setCellFactory((column) -> new DragSelectionCell<>());
        });
        // Enable copy by Ctrl + C or by right click -> Copy
        enableCopyAndExportDataFromTable(tableView, SelectionMode.MULTIPLE, stageHolder);
        AtomicReference<Boolean> tooltipConfigured = new AtomicReference<>(false);
        //Set the auto-resize policy
        tableView.itemsProperty().addListener((observable, oldValue, newValue) -> {
            autoResizeColumns(tableView);
            // Add tooltip into header, we configure it here because we use lookup methods of TableView and need to wait for the table rendered
            if (!tooltipConfigured.get()) {
                configureTableViewHeaderTooltip(tableView);
                tooltipConfigured.set(true);
            }
        });
        tableView.getItems().addListener((ListChangeListener<? super S>) (change) -> {
            autoResizeColumns(tableView);
        });
        return tableView;
    }

    public static <S> void configureTableViewHeaderTooltip(TableView<S> tableView) {
        tableView.getColumns().forEach((tableColumn) -> {
            TableColumnHeader header = (TableColumnHeader) tableView.lookup("#" + tableColumn.getId());
            Label label = (Label) header.lookup(".label");
            label.setTooltip(new Tooltip(tableColumn.getText()));
        });
    }

    private static <S> void autoResizeColumns(TableView<S> tableView) {
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tableView.getColumns().subList(1, tableView.getColumns().size() - 1).forEach((column) ->
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
            column.setPrefWidth(Math.min(500, max) + 50.0d);
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
        numberCol.setId("indexCol");
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
        List<Field> fields = getPropertyFieldFromTableItem(tableItemClass);
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

    public static void enableCopyAndExportDataFromTable(TableView<?> tableView, SelectionMode selectionMode, StageHolder stage) {
//        tableView.getSelectionModel().setCellSelectionEnabled(isCellSelectionEnabled);
        tableView.getSelectionModel().setSelectionMode(selectionMode);

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(event -> copySelectedInTableViewToClipboard(tableView, false));

        MenuItem exportTableItem = new MenuItem("Export Table");
        exportTableItem.setOnAction(event -> {
            String data = getTableDataInCSV(tableView);
            try {
                ViewUtil.saveDataToFile(data, stage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        MenuItem exportSelectedItem = new MenuItem("Export Selected");
        exportSelectedItem.setOnAction(event -> {
            String selectedData = getSelectedRowsData(tableView, true);
            try {
                ViewUtil.saveDataToFile(selectedData, stage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        ContextMenu menu = new ContextMenu();
        menu.getItems().add(copyItem);
        menu.getItems().add(exportTableItem);
        menu.getItems().add(exportSelectedItem);
        tableView.setContextMenu(menu);

        final KeyCodeCombination keyCodeCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.META_DOWN);
        tableView.setOnKeyPressed(event -> {
            if (keyCodeCopy.match(event)) {
                copySelectedInTableViewToClipboard(tableView, false);
            }
        });
    }

    private static void copySelectedInTableViewToClipboard(TableView<?> tableView, boolean isCellSelectionEnabled) {
        if (isCellSelectionEnabled) {
            copySelectedCellsToClipboard(tableView);
        } else {
            copySelectedRowsToClipboard(tableView);
        }
    }

    private static String getTableDataInCSV(TableView<?> tableView) {
        StringBuilder strb = new StringBuilder();
        strb.append(getHeaderText(tableView, ViewUtil.COLUMN_SEPERATOR)).append(ViewUtil.LINE_SEPARATOR);
        Set<Integer> rows = IntStream.range(0, tableView.getItems().size()).boxed().collect(Collectors.toSet());
        getRowData(tableView, rows, strb);
        return strb.toString();
    }

    public static void copySelectedCellsToClipboard(TableView<?> tableView) {
        String selectedData = getSelectedCellsData(tableView);
        final ClipboardContent content = new ClipboardContent();
        content.putString(selectedData);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private static String getSelectedCellsData(TableView<?> tableView) {
        ObservableList<TablePosition> posList = tableView.getSelectionModel().getSelectedCells();
        int old_r = -1;
        StringBuilder selectedString = new StringBuilder();
        for (TablePosition<?, ?> p : posList) {
            int r = p.getRow();
            int c = p.getColumn();
            Object cell = tableView.getColumns().get(c).getCellData(r);
            if (cell == null)
                cell = "";
            if (old_r == r)
                selectedString.append('\t');
            else if (old_r != -1)
                selectedString.append(System.lineSeparator());
            selectedString.append(cell);
            old_r = r;
        }
        return selectedString.toString();
    }

    public static void copySelectedRowsToClipboard(final TableView<?> table) {
        final String data = getSelectedRowsData(table, true);
        final ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(data);
        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }

    private static String getSelectedRowsData(TableView<?> table, boolean isHeaderIncluded) {
        final StringBuilder strb = new StringBuilder();
        // get table header
        if (isHeaderIncluded) {
            String header = getHeaderText(table, ViewUtil.COLUMN_SEPERATOR);
            strb.append(header).append(ViewUtil.LINE_SEPARATOR);
        }

        final Set<Integer> rows = new TreeSet<>();
        for (final TablePosition tablePosition : table.getSelectionModel().getSelectedCells()) {
            rows.add(tablePosition.getRow());
        }
        getRowData(table, rows, strb);
        return strb.toString();
    }

    private static void getRowData(TableView<?> table, Set<Integer> rows, StringBuilder strb) {
        boolean firstRow = true;
        for (final Integer row : rows) {
            if (!firstRow) {
                strb.append(ViewUtil.LINE_SEPARATOR);
            }
            firstRow = false;
            boolean firstCol = true;
            // exclude first column which is index column
            var columns = table.getColumns().subList(1, table.getColumns().size());
            for (final TableColumn<?, ?> column : columns) {
                if (!firstCol) {
                    strb.append(ViewUtil.COLUMN_SEPERATOR);
                }
                firstCol = false;
                final Object cellData = column.getCellData(row);
                strb.append(cellData == null ? "" : cellData.toString());
            }
        }
    }

    private static String getHeaderText(TableView<?> table, String columnSeperator) {
        String header = table.getColumns().subList(1, table.getColumns().size()).stream().map(TableColumn::getText).collect(Collectors.joining(columnSeperator));
        return header;
    }

    public static List<String> getPropertyFieldNamesFromTableItem(Class<?> tableIemClass) {
        List<String> fieldNames = getPropertyFieldFromTableItem(tableIemClass).stream()
                .map(Field::getName)
                .toList();
        return fieldNames;
    }

    public static List<Field> getPropertyFieldFromTableItem(Class<?> tableIemClass) {
        return Arrays.stream(tableIemClass.getDeclaredFields())
                .filter(f -> Property.class.isAssignableFrom(f.getType()) && f.isAnnotationPresent(io.github.nhtuan10.mykafkatool.annotation.TableColumn.class))
                .toList();
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
