package com.example.mytool.ui.controller;

import com.example.mytool.model.kafka.KafkaCluster;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class AddConnectionModalController extends ModalController {

    @FXML
    private TextField clusterNameTextField;
    @FXML
    private TextField bootstrapServerTextField;
    @FXML
    private TextField schemaRegistryTextField;
    @FXML
    private CheckBox isOnlySubjectLoadedCheckBox;

    private final ObjectProperty<KafkaCluster> objectProperty = new SimpleObjectProperty<>();

    @FXML
    private Button addBtn;
    @FXML
    private Button cancelBtn;

    @FXML
    public void initialize() {
//        clusterNameTextField.textProperty().bindBidirectional(kafkaClusterObjectProperty.map());
        objectProperty.addListener((observable, oldValue, newValue) -> {
            clusterNameTextField.setText(newValue.getName());
            bootstrapServerTextField.setText(newValue.getBootstrapServer());
            schemaRegistryTextField.setText(newValue.getSchemaRegistryUrl());
            isOnlySubjectLoadedCheckBox.setSelected(newValue.isOnlySubjectLoaded());
        });
    }

    @FXML
    protected void add() throws IOException {
        modelRef.set(KafkaCluster.builder()
                .name(clusterNameTextField.getText())
                .bootstrapServer(bootstrapServerTextField.getText())
                .schemaRegistryUrl(schemaRegistryTextField.getText())
                .isOnlySubjectLoaded(isOnlySubjectLoadedCheckBox.isSelected()).build());
        Stage stage = (Stage) addBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void cancel() throws IOException {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}
