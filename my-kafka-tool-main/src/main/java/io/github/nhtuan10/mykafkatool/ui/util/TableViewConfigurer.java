package io.github.nhtuan10.mykafkatool.ui.util;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.github.nhtuan10.mykafkatool.annotation.FilterableTableItemField;
import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import io.github.nhtuan10.mykafkatool.constant.UIStyleConstant;
import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.control.CopyTextMenuItem;
import io.github.nhtuan10.mykafkatool.ui.control.DragSelectionCell;
import io.github.nhtuan10.mykafkatool.ui.control.EditingTableCell;
import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageTableItem;
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
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class TableViewConfigurer {
    public static final char COLUMN_SEPERATOR = '\t';
    public static final String LINE_SEPARATOR = System.lineSeparator();
    private final static CsvMapper CSV_MAPPER = new CsvMapper();
    private final static CsvSchema schema = CsvSchema.builder()
            .setColumnSeparator(COLUMN_SEPERATOR)
            .setLineSeparator(LINE_SEPARATOR).build();

    public static final int MAX_TABLE_COLUMN_WIDTH = 400;
    public static final double TABLE_COLUMN_WIDTH_MARGIN = 40.0d;

    public static <S> TableView<S> configureTableView(Class<S> clazz, String fxId, Stage stage, StageHolder stageHolder, TableViewConfiguration<S> tableViewConfiguration) {
        TableView<S> tableView = (TableView<S>) stage.getScene().lookup("#" + fxId);
        return configureTableView(clazz, tableView, stageHolder, tableViewConfiguration);
    }

    public static <S> TableView<S> configureTableView(Class<S> clazz, TableView<S> tableView, @NonNull StageHolder stageHolder, TableViewConfiguration<S> tableViewConfiguration) {
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tableView.getSelectionModel().setSelectionMode(tableViewConfiguration.selectionMode());
        tableView.getSelectionModel().setCellSelectionEnabled(tableViewConfiguration.isCellSelectionEnabled());
        TableColumn<S, S> numberCol = buildIndexTableColumn();
        tableView.getColumns().addFirst(numberCol);

        List<String> fieldNames = getTableColumnNamesFromTableItem(clazz);
        IntStream.range(0, fieldNames.size()).forEach(i -> {
            TableColumn<S, ?> tableColumn = tableView.getColumns().get(i + 1);
            tableColumn.setId(fieldNames.get(i));
//            tableColumn.setText(null);
//            Label label = new Label();
//            label.setTooltip(new Tooltip(columnHeader));
//            tableColumn.setGraphic(label);
            tableColumn.setCellValueFactory(new PropertyValueFactory<>(fieldNames.get(i)));
            tableColumn.setCellFactory((column) -> new DragSelectionCell<>(stageHolder, tableViewConfiguration));
        });
        // Enable copy by Ctrl + C or by right click -> Copy
        enableCopyDataFromTableByShortcutKeys(tableView, tableViewConfiguration.isHeadersIncludedInRowCopy(), tableViewConfiguration);
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
            if (header != null) {
                Label label = (Label) header.lookup(".label");
                label.setTooltip(new Tooltip(tableColumn.getText()));
            }
        });
    }

    public static <S> void autoResizeColumns(TableView<S> tableView) {
        // ignore first column, which is index column
        tableView.getColumns().subList(1, tableView.getColumns().size()).forEach((column) ->
        {
            if (column.getWidth() >= MAX_TABLE_COLUMN_WIDTH + TABLE_COLUMN_WIDTH_MARGIN) {
                return;
            }
            //Minimal width = columnheader
            Text t = new Text(column.getText());
            double headerWidth = t.getLayoutBounds().getWidth();
            if (headerWidth >= MAX_TABLE_COLUMN_WIDTH) {
                column.setPrefWidth(MAX_TABLE_COLUMN_WIDTH + TABLE_COLUMN_WIDTH_MARGIN);
                return;
            }
            String maxStr = IntStream.range(0, tableView.getItems().size())
                    .mapToObj(i -> String.valueOf(column.getCellData(i)))
                    .max(Comparator.comparing(String::length)).orElse("");
            double max = Math.max(headerWidth, new Text(maxStr).getLayoutBounds().getWidth());
//            for (int i = 0; i < tableView.getItems().size(); i++) {
//                //cell must not be empty
//                if (column.getCellData(i) != null) {
//                    t = new Text(column.getCellData(i).toString());
//                    double calcwidth = t.getLayoutBounds().getWidth();
//                    //remember new max-width
//                    if (calcwidth > max) {
//                        max = calcwidth;
//                    }
//                    if (max >= MAX_TABLE_COLUMN_WIDTH)
//                        break;
//                }
//            }
            //set the new max-width with some extra space
            if (max + TABLE_COLUMN_WIDTH_MARGIN > column.getWidth() + 5) {
                column.setPrefWidth(Math.min(MAX_TABLE_COLUMN_WIDTH, max) + TABLE_COLUMN_WIDTH_MARGIN);
            }
        });
    }

    private static <S> TableColumn<S, S> buildIndexTableColumn() {
        TableColumn<S, S> numberCol = new TableColumn<>("#");
        numberCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper(p.getValue()));

        numberCol.setCellFactory((column) -> new TableCell<S, S>() {
            @Override
            protected void updateItem(S item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(UIStyleConstant.INDEX_COL_CELL_CLASS);
                if (this.getTableRow() != null && item != null) {
                    setText(String.valueOf(this.getTableRow().getIndex() + 1));
                    getStyleClass().add(UIStyleConstant.INDEX_COL_CELL_CLASS);
                } else {
                    setText("");
                    getStyleClass().remove(UIStyleConstant.INDEX_COL_CELL_CLASS);
                }
            }
        });
        numberCol.setSortable(false);
        numberCol.setId("indexCol");
        return numberCol;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <S> void configureEditableTableCell(TableView<S> tableView, Class<S> tableItemClass, @NonNull StageHolder stageHolder, TableViewConfigurer.TableViewConfiguration<S> tableViewConfiguration) {
        Callback<TableColumn<S, String>,
                TableCell<S, String>> cellFactory
                = (TableColumn<S, String> p) -> new EditingTableCell<>(stageHolder, tableViewConfiguration);
        List<Field> fields = getTableColumnFieldsFromTableItem(tableItemClass);
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

    private static <S> void enableCopyDataFromTableByShortcutKeys(TableView<S> tableView, boolean isHeadersIncludedInRowCopy, TableViewConfigurer.TableViewConfiguration<S> tableViewConfiguration) {
        final KeyCodeCombination keyCodeCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.META_DOWN);
        tableView.setOnKeyPressed(event -> {
            if (keyCodeCopy.match(event)) {
                copySelectedInTableViewToClipboard(tableView, false, isHeadersIncludedInRowCopy, tableViewConfiguration.extraFieldsToCopyAndExport());
            }
        });
    }

    public static <S> List<MenuItem> getTableContextMenuItems(TableView<S> tableView, String cellText, StageHolder stage, TableViewConfigurer.TableViewConfiguration<S> tableViewConfiguration) {
        MenuItem copyRowItem = new MenuItem("Copy Row");
        copyRowItem.setOnAction(event -> copySelectedInTableViewToClipboard(tableView, tableViewConfiguration.isCellSelectionEnabled(), tableViewConfiguration.isHeadersIncludedInRowCopy(), tableViewConfiguration.extraFieldsToCopyAndExport()));

        MenuItem exportTableItem = new MenuItem("Export Table");
        boolean isHeadersIncludedInRowExport = tableViewConfiguration.isHeadersIncludedInRowExport();
        exportTableItem.setOnAction(event -> {
            String data = getTableDataInCSV(tableView, isHeadersIncludedInRowExport, tableViewConfiguration.extraFieldsToCopyAndExport());
            try {
                ViewUtils.saveDataToFile(data, stage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        MenuItem exportSelectedItem = new MenuItem("Export Selected");
        exportSelectedItem.setOnAction(event -> {
            String selectedData = getSelectedRowsData(tableView, isHeadersIncludedInRowExport, tableViewConfiguration.extraFieldsToCopyAndExport());
            try {
                ViewUtils.saveDataToFile(selectedData, stage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        String truncatedText = cellText.length() > 21 ? cellText.substring(0, 21) + "..." : cellText;
        MenuItem copyHoverCell = new CopyTextMenuItem("Copy '%s'".formatted(truncatedText));
        copyHoverCell.setOnAction(event -> ViewUtils.copyTextToClipboard(cellText));

        return List.of(copyRowItem, exportTableItem, exportSelectedItem, copyHoverCell);
    }

    private static <S> void copySelectedInTableViewToClipboard(TableView<S> tableView, boolean isCellSelectionEnabled, boolean isHeaderIncluded, TableViewConfiguration.ExtraFieldsToCopyAndExport<S> extraFieldsToCopyAndExport) {
        if (isCellSelectionEnabled) {
            copySelectedCellsToClipboard(tableView);
        } else {
            copySelectedRowsToClipboard(tableView, isHeaderIncluded, extraFieldsToCopyAndExport);
        }
    }

    private static <S> String getTableDataInCSV(TableView<S> tableView, boolean isHeaderIncluded, TableViewConfiguration.ExtraFieldsToCopyAndExport<S> extraFieldsToCopyAndExport) {
        Set<Integer> rows = IntStream.range(0, tableView.getItems().size()).boxed().collect(Collectors.toSet());
        return getRowData(tableView, rows, true, extraFieldsToCopyAndExport);
    }

    // Doesn't have any use case for cell select or copy data from the table by cells.
    // Below code are legacy,

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

    public static <S> void copySelectedRowsToClipboard(final TableView<S> table, boolean isHeaderIncluded, TableViewConfiguration.ExtraFieldsToCopyAndExport<S> extraFieldsToCopyAndExport) {
        final String data = getSelectedRowsData(table, isHeaderIncluded, extraFieldsToCopyAndExport);
        ViewUtils.copyTextToClipboard(data);
    }

    private static <S> String getSelectedRowsData(TableView<S> table, boolean isHeaderIncluded, TableViewConfiguration.ExtraFieldsToCopyAndExport<S> extraFieldsToCopyAndExport) {
        final Set<Integer> rows = new TreeSet<>();
        for (final TablePosition tablePosition : table.getSelectionModel().getSelectedCells()) {
            rows.add(tablePosition.getRow());
        }
        return getRowData(table, rows, isHeaderIncluded, extraFieldsToCopyAndExport);
    }

    private static <S> String getRowData(TableView<S> table, Set<Integer> rows, boolean isHeaderIncluded, TableViewConfiguration.ExtraFieldsToCopyAndExport<S> extraFieldsToCopyAndExport) {
        try (StringWriter strWriter = new StringWriter()) {
            SequenceWriter seqWriter = CSV_MAPPER.writer(schema)
                    .writeValues(strWriter);
            if (isHeaderIncluded) {
                // get table header
                List<String> headers = Stream.concat(getHeaders(table).stream(), extraFieldsToCopyAndExport.extraFieldsHeadersToCopyAndExport().stream())
                        .toList();
                seqWriter.write(headers);
            }

            for (final Integer row : rows) {
                // exclude first column which is index column
                List<? extends TableColumn<?, ?>> columns = table.getColumns().subList(1, table.getColumns().size());
                List<String> csvRow = new ArrayList<>(columns.size());
                for (final TableColumn<?, ?> column : columns) {
                    final Object cellData = column.getCellData(row);
                    csvRow.add(cellData == null ? "" : cellData.toString());
                }
                csvRow.addAll(extraFieldsToCopyAndExport.extraFieldsToCopyAndExportFunc().apply(table.getItems().get(row)));
                seqWriter.write(csvRow);
            }
            seqWriter.flush();
            seqWriter.close();
            return strWriter.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static String getHeaderText(TableView<?> table, String columnSeperator) {
        return String.join(columnSeperator, getHeaders(table));
    }

    private static List<String> getHeaders(TableView<?> table) {
        return table.getColumns().subList(1, table.getColumns().size()).stream().map(TableColumn::getText).toList();
    }

    public static List<String> getTableColumnNamesFromTableItem(Class<?> tableIemClass) {
        return getTableColumnFieldsFromTableItem(tableIemClass).stream()
                .map(Field::getName)
                .toList();
    }

    public static List<Field> getTableColumnFieldsFromTableItem(Class<?> tableIemClass) {
        return Arrays.stream(tableIemClass.getDeclaredFields())
                .filter(f -> Property.class.isAssignableFrom(f.getType()) && f.isAnnotationPresent(TableViewColumn.class))
                .toList();
    }

    public static List<String> getFilterableFieldsFromTableItem(Class<?> tableIemClass) {
        return Arrays.stream(tableIemClass.getDeclaredFields())
                .filter(TableViewConfigurer::isFilterableField)
                .map(Field::getName)
                .toList();
    }

    public static boolean isFilterableField(Field field) {
        for (Annotation annotation : field.getAnnotations()) {
            return (annotation instanceof FilterableTableItemField || annotation.annotationType().isAnnotationPresent(FilterableTableItemField.class));
        }
        return false;
    }

    public static Optional<TableColumn<KafkaMessageTableItem, ?>> getTableColumnById(TableView<KafkaMessageTableItem> tableView, String tableColumnId) {
        return tableView.getColumns().stream().filter(c -> c.getId().equals(tableColumnId)).findFirst();
    }

    public static record TableViewConfiguration<T>(
            SelectionMode selectionMode
            , boolean isCellSelectionEnabled
            , boolean isHeadersIncludedInRowCopy
            , boolean isHeadersIncludedInRowExport
            , ExtraFieldsToCopyAndExport<T> extraFieldsToCopyAndExport
    ) {
        public static final TableViewConfiguration<?> DEFAULT = new TableViewConfiguration<>(SelectionMode.MULTIPLE, false, true, true, new ExtraFieldsToCopyAndExport(List.of(), (item) -> List.of()));

        public record ExtraFieldsToCopyAndExport<T>(List<String> extraFieldsHeadersToCopyAndExport,
                                                    Function<T, List<String>> extraFieldsToCopyAndExportFunc) {
        }
    }
}
