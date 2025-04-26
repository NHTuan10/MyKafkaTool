package com.example.mytool.ui;

import com.example.mytool.ErrorController;
import com.example.mytool.MyApplication;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

@Slf4j
public class UIErrorHandler {
    public static void showError(Thread t, Throwable e) {
        log.error("***Default exception handler***", e);
        if (Platform.isFxApplicationThread()) {
            showErrorDialog(e);
        } else {
            log.error("An unexpected error occurred in " + t);

        }
    }

    private static void showErrorDialog(Throwable e) {
        StringWriter errorMsg = new StringWriter();
        e.printStackTrace(new PrintWriter(errorMsg));
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        FXMLLoader loader = new FXMLLoader(MyApplication.class.getResource("error.fxml"));
        try {
            Parent root = loader.load();
            ((ErrorController) loader.getController()).setErrorText(errorMsg.toString());
            dialog.setScene(new Scene(root, 450, 600));
            dialog.show();
        } catch (IOException exc) {
            log.error("Error loading error dialog", exc);
        }
    }
}
