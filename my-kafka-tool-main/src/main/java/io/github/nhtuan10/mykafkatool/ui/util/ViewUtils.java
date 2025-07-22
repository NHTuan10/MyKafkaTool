package io.github.nhtuan10.mykafkatool.ui.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.control.DateTimePicker;
import javafx.concurrent.Task;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

@Slf4j
public final class ViewUtils {

    public static void setValueAndHighlightJsonInCodeArea(String inValue, CodeArea codeArea, boolean prettyPrint, ObjectMapper objectMapper, JsonHighlighter jsonHighlighter) {
        if (StringUtils.isNotBlank(inValue)) {
            String value = inValue;
            try {
                value = prettyPrint ?
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(inValue)) :
                        inValue;
            } catch (JsonProcessingException e) {
            }
            codeArea.replaceText(value);
            try {
                codeArea.setStyleSpans(0, jsonHighlighter.highlight(value));
            } catch (Exception e) {
                log.error("Error highlighting json in code area", e);
            }
        } else codeArea.replaceText(Objects.requireNonNullElse(inValue, ""));
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
//        CompletableFuture.runAsync(task);
        new Thread(task).start();
        return task;
    }

    public static void saveDataToFile(String title, String data, StageHolder parentStageHolder, FileChooser.ExtensionFilter... extensionFilters) throws IOException {
        assert parentStageHolder != null && parentStageHolder.getStage() != null;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
//        fileChooser.setInitialFileName();
        fileChooser.getExtensionFilters().addAll(
                extensionFilters);
        File selectedFile = fileChooser.showSaveDialog(parentStageHolder.getStage());
        if (selectedFile != null) {
            Files.writeString(selectedFile.toPath(), data);
        }
    }

    public static Path openFile(String title, String initFileName, StageHolder parentStageHolder, FileChooser.ExtensionFilter... extensionFilters) throws IOException {
        assert parentStageHolder != null && parentStageHolder.getStage() != null;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialFileName(initFileName);
        fileChooser.getExtensionFilters().addAll(
                extensionFilters);
        File selectedFile = fileChooser.showOpenDialog(parentStageHolder.getStage());
        if (selectedFile != null) {
            return selectedFile.toPath();
        }
        return null;
    }

    public static void copyTextToClipboard(String text) {
        final ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    public static Long getTimestamp(DateTimePicker dateTimePicker) {
        return (dateTimePicker.getValue() != null && dateTimePicker.getDateTimeValue() != null) ? ZonedDateTime.of(dateTimePicker.getDateTimeValue(), ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }
}
