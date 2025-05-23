package io.github.nhtuan10.mykafkatool.ui.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.Application;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.fxmisc.richtext.CodeArea;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public final class ViewUtil {

    public static boolean confirmAlert(String title, String text, String okDoneText, String cancelCloseText) {
        ButtonType yes = new ButtonType(okDoneText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(cancelCloseText, ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, yes, cancel);
        alert.setTitle(title);
        Optional<ButtonType> result = alert.showAndWait();

        return result.orElse(cancel) == yes;
    }

    public static void enableCopyDataFromTableToClipboard(TableView<?> tableView, SelectionMode selectionMode, boolean isCellSelectionEnabled) {
        tableView.getSelectionModel().setCellSelectionEnabled(isCellSelectionEnabled);
        tableView.getSelectionModel().setSelectionMode(selectionMode);

        MenuItem item = new MenuItem("Copy");
        item.setOnAction(event -> copySelectedInTableViewToClipboard(tableView, isCellSelectionEnabled));
        ContextMenu menu = new ContextMenu();
        menu.getItems().add(item);
        tableView.setContextMenu(menu);

        final KeyCodeCombination keyCodeCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.META_DOWN);
        tableView.setOnKeyPressed(event -> {
            if (keyCodeCopy.match(event)) {
                copySelectedInTableViewToClipboard(tableView, isCellSelectionEnabled);
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

    public static void copySelectedCellsToClipboard(TableView<?> tableView) {
        ObservableList<TablePosition> posList = tableView.getSelectionModel().getSelectedCells();
        int old_r = -1;
        StringBuilder clipboardString = new StringBuilder();
        for (TablePosition<?, ?> p : posList) {
            int r = p.getRow();
            int c = p.getColumn();
            Object cell = tableView.getColumns().get(c).getCellData(r);
            if (cell == null)
                cell = "";
            if (old_r == r)
                clipboardString.append('\t');
            else if (old_r != -1)
                clipboardString.append('\n');
            clipboardString.append(cell);
            old_r = r;
        }
        final ClipboardContent content = new ClipboardContent();
        content.putString(clipboardString.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    public static void copySelectedRowsToClipboard(final TableView<?> table) {
        final Set<Integer> rows = new TreeSet<>();
        for (final TablePosition tablePosition : table.getSelectionModel().getSelectedCells()) {
            rows.add(tablePosition.getRow());
        }
        final StringBuilder strb = new StringBuilder();
        boolean firstRow = true;
        for (final Integer row : rows) {
            if (!firstRow) {
                strb.append('\n');
            }
            firstRow = false;
            boolean firstCol = true;
            for (final TableColumn<?, ?> column : table.getColumns()) {
                if (!firstCol) {
                    strb.append('\t');
                }
                firstCol = false;
                final Object cellData = column.getCellData(row);
                strb.append(cellData == null ? "" : cellData.toString());
            }
        }
        final ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(strb.toString());
        Clipboard.getSystemClipboard().setContent(clipboardContent);
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

    public static void showPopUpModal(String modalFxml, String title, AtomicReference<Object> modelRef, final Map<String, Object> inputVarMap) throws IOException {
        showPopUpModal(modalFxml, title, modelRef, inputVarMap, true);
    }

    //    private Tuple2<String, String> showAddMsgModalAndGetResult() throws IOException {
    public static void showPopUpModal(String modalFxml, String title, AtomicReference<Object> modelRef, final Map<String, Object> inputVarMap, final boolean editable) throws IOException {
        Stage stage = new Stage();
//        FXMLLoader addMsgModalLoader = new FXMLLoader(
//                AddMessageModalController.class.getResource("add-message-modal.fxml"));

        FXMLLoader modalLoader = new FXMLLoader(
                Application.class.getResource(modalFxml));
        Scene scene = new Scene(modalLoader.load());

//        AddMessageModalController addMessageModalController =  modalLoader.getController();
        ModalController modalController = modalLoader.getController();
//        modalController.setParentController(parentController);
        modalController.setModelRef(modelRef);
        modalController.setTextFieldOrAreaText(modalController, inputVarMap);
        modalController.launch(editable);
        stage.setTitle(title);
        stage.initModality(Modality.WINDOW_MODAL);

        stage.setResizable(false);
        if (editable) {
            stage.setAlwaysOnTop(true);
            stage.initModality(Modality.APPLICATION_MODAL);
        }
//        ActionEvent event
//        stage.initOwner(
//                ((Node)event.getSource()).getScene().getWindow() );
        stage.setScene(scene);
        URL cssResource = Application.class.getResource("style.css");
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
        new Thread(task).start();
        return task;
    }
}
