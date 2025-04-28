package com.example.mytool;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;

public class AddMessageModalController extends ModalController {

    @FXML
    private TextArea key;
    @FXML
    private TextArea value;
    @FXML
    private TextArea schema;
    @FXML
    private Button okBtn;
    @FXML
    private Button cancelBtn;

    @FXML
    protected void ok() throws IOException {
        modelRef.set(Triple.of(key.getText(), value.getText(), schema.getText()));
        Stage stage = (Stage) okBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void cancel() throws IOException {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}
