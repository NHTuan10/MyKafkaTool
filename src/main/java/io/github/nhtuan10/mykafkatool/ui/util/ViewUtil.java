package io.github.nhtuan10.mykafkatool.ui.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.controller.ModalController;
import io.github.nhtuan10.mykafkatool.ui.partition.KafkaPartitionsTableItem;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
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

    public static void showPopUpModal(final String modalFxml, final String title, final AtomicReference<Object> modelRef, final Map<String, Object> inputVarMap, final boolean editable, final boolean resizable, Window parentWindow) throws IOException {
        Stage stage = new Stage();

        FXMLLoader modalLoader = new FXMLLoader(
                MyKafkaToolApplication.class.getResource(modalFxml));
        Scene scene = new Scene(modalLoader.load());
        MyKafkaToolApplication.applyThemeFromCurrentUserPreference(scene);

        ModalController modalController = modalLoader.getController();
        modalController.setModelRef(modelRef);
        modalController.setFields(modalController, stage, inputVarMap);
        modalController.launch(editable);
        stage.setTitle(title);
        if (editable) {
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(parentWindow);
        }
        stage.setResizable(resizable);
        stage.setScene(scene);
//        URL cssResource = MyKafkaToolApplication.class.getResource(UIStyleConstant.LIGHT_STYLE_CSS_FILE);
//        scene.getStylesheets().add(cssResource.toExternalForm());

        stage.showAndWait();
    }


    public static void showAlertDialog(Alert.AlertType alertType, String text, String title, ButtonType... buttonTypes) {
        Alert alert = new Alert(alertType, text, buttonTypes);
        if (title != null) {
            alert.setTitle(title);
        }

        alert.showAndWait();
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

    public static <T> Task<T> runBackgroundTask(Callable<T> callable, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return callable.call();
            }
        };
        task.setOnSucceeded(workerStateEvent -> onSuccess.accept((T) workerStateEvent.getSource().getValue()));
        task.setOnFailed(workerStateEvent -> onError.accept(workerStateEvent.getSource().getException()));
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

    public static void copyTextToClipboard(String text) {
        final ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
