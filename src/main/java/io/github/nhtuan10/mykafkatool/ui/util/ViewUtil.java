package io.github.nhtuan10.mykafkatool.ui.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.controller.ModalController;
import io.github.nhtuan10.mykafkatool.ui.partition.KafkaPartitionsTableItem;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public final class ViewUtil {

    public static final String COLUMN_SEPERATOR = "\t";
    public static final String LINE_SEPARATOR = System.lineSeparator();

    public static boolean confirmAlert(String title, String text, String okDoneText, String cancelCloseText) {
        ButtonType yes = new ButtonType(okDoneText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(cancelCloseText, ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, yes, cancel);
        alert.setTitle(title);
        Optional<ButtonType> result = alert.showAndWait();

        return result.orElse(cancel) == yes;
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
        strb.append(getHeaderText(tableView, COLUMN_SEPERATOR)).append(LINE_SEPARATOR);
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
            String header = getHeaderText(table, COLUMN_SEPERATOR);
            strb.append(header).append(LINE_SEPARATOR);
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
                strb.append(LINE_SEPARATOR);
            }
            firstRow = false;
            boolean firstCol = true;
            // exclude first column which is index column
            var columns = table.getColumns().subList(1, table.getColumns().size());
            for (final TableColumn<?, ?> column : columns) {
                if (!firstCol) {
                    strb.append(COLUMN_SEPERATOR);
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

    public static KafkaPartitionsTableItem mapToUIPartitionTableItem(TopicPartitionInfo partitionInfo, Pair<Long, Long> partitionOffsetsInfo) {
        Node leader = partitionInfo.leader();
        return new KafkaPartitionsTableItem(
                partitionInfo.partition(),
                partitionOffsetsInfo.getLeft(),
                partitionOffsetsInfo.getRight(),
                partitionOffsetsInfo.getRight() - partitionOffsetsInfo.getLeft(),
                leader.host() + ":" + leader.port(),
                FXCollections.observableArrayList(partitionInfo.isr().stream().filter(r -> r != leader).map(replica -> replica.host() + ":" + replica.port()).toList()),
                FXCollections.observableArrayList(partitionInfo.replicas().stream().filter(r -> r != leader && !partitionInfo.isr().contains(r)).map(replica -> replica.host() + ":" + replica.port()).toList()));
    }

    public static void showPopUpModal(String modalFxml, String title, AtomicReference<Object> modelRef, final Map<String, Object> inputVarMap, Window parentWindow) throws IOException {
        showPopUpModal(modalFxml, title, modelRef, inputVarMap, true, false, parentWindow);
    }

    //    private Tuple2<String, String> showAddMsgModalAndGetResult() throws IOException {
    public static void showPopUpModal(final String modalFxml, final String title, final AtomicReference<Object> modelRef, final Map<String, Object> inputVarMap, final boolean editable, final boolean resizable, Window parentWindow) throws IOException {
        Stage stage = new Stage();
//        FXMLLoader addMsgModalLoader = new FXMLLoader(
//                AddMessageModalController.class.getResource("add-message-modal.fxml"));

        FXMLLoader modalLoader = new FXMLLoader(
                MyKafkaToolApplication.class.getResource(modalFxml));
        Scene scene = new Scene(modalLoader.load());

//        AddMessageModalController addMessageModalController =  modalLoader.getController();
        ModalController modalController = modalLoader.getController();
//        modalController.setParentController(parentController);
        modalController.setModelRef(modelRef);
        modalController.setFields(modalController, stage, inputVarMap);
        modalController.launch(editable);
        stage.setTitle(title);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentWindow);

        stage.setResizable(resizable);
//        if (editable) {
//            stage.setAlwaysOnTop(true);
//            stage.initModality(Modality.APPLICATION_MODAL);
//        }
//        ActionEvent event
//        stage.initOwner(
//                ((Node)event.getSource()).getScene().getWindow() );
        stage.setScene(scene);
        URL cssResource = MyKafkaToolApplication.class.getResource("style.css");
        scene.getStylesheets().add(cssResource.toExternalForm());
        stage.showAndWait();
//        return modelRef.get();
    }


    public static void showAlertDialog(Alert.AlertType alertType, String text, String title, ButtonType... buttonTypes) {
        Alert alert = new Alert(alertType, text, buttonTypes);
        if (title != null) {
            alert.setTitle(title);
        }

        alert.showAndWait();
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

    public static void highlightJsonInCodeArea(String inValue, CodeArea codeArea, boolean prettyPrint, ObjectMapper objectMapper, JsonHighlighter jsonHighlighter) {
        String value = inValue;
        try {
            value = prettyPrint ?
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(inValue)) :
                    codeArea.getText();
        } catch (JsonProcessingException e) {
        }
        codeArea.replaceText(value);
        try {
            codeArea.setStyleSpans(0, jsonHighlighter.highlight(value));
        } catch (Exception e) {
            log.error("Error highlighting json in code area", e);
        }
    }

    public static class DragSelectionCell<S, T> extends TableCell<S, T> {

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
            }
        }
    }

    public static <T> Task<T> runBackgroundTask(Callable<T> callable, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return callable.call();
            }
        };
        task.setOnSucceeded(workerStateEvent -> onSuccess.accept((T) workerStateEvent.getSource().getValue()));
        task.setOnFailed(workerStateEvent -> onError.accept(workerStateEvent.getSource().getException()));
//        Thread thread =  new Thread(task);
//        thread.setDaemon(true);
//        thread.start();
        CompletableFuture.runAsync(task);
        return task;
    }

    public static void saveDataToFile(String data, StageHolder parentStageHolder) throws IOException {
        assert parentStageHolder != null && parentStageHolder.getStage() != null;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export");
//        fileChooser.setInitialFileName();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showSaveDialog(parentStageHolder.getStage());
        if (selectedFile != null) {
            Files.writeString(selectedFile.toPath(), data);
        }

    }

}
