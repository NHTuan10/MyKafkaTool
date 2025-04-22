package com.example.mytool;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;

public class AddConnectionModalController extends ModalController {
//
//    public Tuple2<String, String> getNewMsg() {
//        return newMsg;
//    }

    @FXML
    private TextField clusterNameTextField;
    @FXML
    private TextField bootstrapServerTextField;
    @FXML
    private Button addBtn;
    @FXML
    private Button cancelBtn;


    @FXML
    protected void add() throws IOException {
        mainController.setNewConnection(Pair.of(clusterNameTextField.getText(), bootstrapServerTextField.getText()));
        Stage stage = (Stage) addBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void cancel() throws IOException {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}
