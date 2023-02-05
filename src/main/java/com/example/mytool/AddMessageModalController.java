package com.example.mytool;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import reactor.util.function.Tuples;

import java.io.IOException;

public class AddMessageModalController extends ModalController {
//
//    public Tuple2<String, String> getNewMsg() {
//        return newMsg;
//    }

    @FXML
    private TextArea key;
    @FXML
    private TextArea value;
    @FXML
    private Button addBtn;
    @FXML
    private Button cancelBtn;

//    private AtomicReference<Tuple2<String, String>> newMsg;
//
//    public void setNewMsg(Tuple2<String, String> newMsg) {
//        this.newMsg = newMsg;
//    }

    @FXML
    protected void add() throws IOException {
//        mainController.setNewMsg(Tuples.of(key.getText(),value.getText()));
        modelRef.set(Tuples.of(key.getText(), value.getText()));
        Stage stage = (Stage) addBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void cancel() throws IOException {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}
