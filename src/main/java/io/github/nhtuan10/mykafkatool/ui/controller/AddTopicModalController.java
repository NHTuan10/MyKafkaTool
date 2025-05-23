package io.github.nhtuan10.mykafkatool.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.kafka.clients.admin.NewTopic;

import java.io.IOException;

public class AddTopicModalController extends ModalController {
    @FXML
    private TextField topicNameTextField;
    @FXML
    private TextField partitionCountTextField;
    @FXML
    private TextField replicationFactorTextField;
    @FXML
    private Button addBtn;
    @FXML
    private Button cancelBtn;


    @FXML
    protected void add() throws IOException {
        modelRef.set(new NewTopic(topicNameTextField.getText(), Integer.parseInt(partitionCountTextField.getText()), Short.parseShort(replicationFactorTextField.getText())));
        Stage stage = (Stage) addBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void cancel() throws IOException {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}
