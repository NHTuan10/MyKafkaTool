package io.github.nhtuan10.mykafkatool.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorController {
    @FXML
    private Label errorMessage;

    @FXML
    private Label errorStackTrace;

    public void setErrorText(Throwable e) throws FileNotFoundException {
        String errorMsg = e.getMessage();
        errorMessage.setText("Error Message: " + errorMsg);
        StringWriter stackTraceWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTraceWriter));
        errorStackTrace.setText(stackTraceWriter.toString());

    }

    @FXML
    private void close() {
        errorStackTrace.getScene().getWindow().hide();
    }

    public void initUI() {
//        errorMessage.lookup(".scroll-bar:horizontal").setStyle("-fx-opacity: 0;");
    }
}