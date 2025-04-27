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
    private TextArea avroSchema;
    @FXML
    private Button addBtn;
    @FXML
    private Button cancelBtn;

    @FXML
    protected void add() throws IOException {
        modelRef.set(Triple.of(key.getText(), value.getText(), avroSchema.getText()));
        Stage stage = (Stage) addBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void cancel() throws IOException {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}
