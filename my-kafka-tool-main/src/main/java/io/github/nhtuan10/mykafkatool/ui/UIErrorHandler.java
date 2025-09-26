package io.github.nhtuan10.mykafkatool.ui;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.ui.controller.ErrorController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class UIErrorHandler {
    public static void showError(Thread t, Throwable e) {

        if (Platform.isFxApplicationThread()) {
            log.error("An error occurred in FX application thread", e);
            // TODO: fix the issue when undo changes in Filter text fields, ignore this issue dialog for now
            if (e instanceof NullPointerException && e.getMessage() != null && e.getMessage().contains("undoChange")) {
                return;
            }
            showErrorDialog(e);
        } else {
            log.error("An unexpected error occurred in {}", t, e);

        }
    }

    private static void showErrorDialog(Throwable e) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        FXMLLoader loader = new FXMLLoader(MyKafkaToolApplication.class.getResource("error.fxml"));
        try {
            Parent root = loader.load();
            ErrorController errorController = loader.getController();
            errorController.setErrorText(e);
            dialog.setScene(new Scene(root, 450, 600));
            dialog.show();
        } catch (IOException exc) {
            log.error("Error loading error dialog", exc);
        }
    }
}
