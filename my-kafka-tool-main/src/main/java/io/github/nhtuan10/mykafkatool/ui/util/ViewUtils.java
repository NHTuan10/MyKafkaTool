package io.github.nhtuan10.mykafkatool.ui.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import javafx.concurrent.Task;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public final class ViewUtils {

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
