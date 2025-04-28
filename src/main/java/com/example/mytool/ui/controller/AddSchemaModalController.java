package com.example.mytool.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;

public class AddSchemaModalController extends ModalController {

    @FXML
    private TextArea schemaTextArea;

    @FXML
    private Button okBtn;

    @FXML
    private Button cancelBtn;


    @FXML
    protected void ok() throws IOException {
        modelRef.set(schemaTextArea.getText());
        Stage stage = (Stage) okBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void cancel() throws IOException {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}
