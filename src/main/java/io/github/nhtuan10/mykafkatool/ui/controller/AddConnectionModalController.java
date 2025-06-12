package io.github.nhtuan10.mykafkatool.ui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.api.auth.AuthConfig;
import io.github.nhtuan10.mykafkatool.api.auth.AuthProvider;
import io.github.nhtuan10.mykafkatool.configuration.annotation.RichTextFxObjectMapper;
import io.github.nhtuan10.mykafkatool.manager.AuthProviderManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import jakarta.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

import java.io.IOException;
import java.util.Map;

public class AddConnectionModalController extends ModalController {
    private final ObjectProperty<KafkaCluster> objectProperty;

    private final JsonHighlighter jsonHighlighter;

    private final AuthProviderManager authProviderManager;

    private final ObjectMapper objectMapper;

    @FXML
    private TextField clusterNameTextField;
    @FXML
    private TextField bootstrapServerTextField;
    @FXML
    private TextField schemaRegistryTextField;
    @FXML
    private CheckBox isOnlySubjectLoadedCheckBox;
    @FXML
    private CodeArea securityConfigTextArea;
    @FXML
    private Button addBtn;
    @FXML
    private Button cancelBtn;
    @FXML
    private ComboBox<AuthProvider> securityTypeComboxBox;

    @Inject
    public AddConnectionModalController(JsonHighlighter jsonHighlighter, AuthProviderManager authProviderManager, @RichTextFxObjectMapper ObjectMapper objectMapper) {
//        this.jsonHighlighter = new JsonHighlighter();
        this.objectProperty = new SimpleObjectProperty<>();
        this.jsonHighlighter = jsonHighlighter;
        this.authProviderManager = authProviderManager;
        this.objectMapper = objectMapper;
    }

    @FXML
    public void initialize() {
//        if (objectProperty.get() != null) {
//            setValuesForFields(objectProperty.get());
//        }
//        objectProperty.addListener((observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                setValuesForFields(newValue);
//            }
//
//        });
        securityTypeComboxBox.getItems().addAll(authProviderManager.getAllAuthProviders());
        securityTypeComboxBox.getSelectionModel().select(authProviderManager.getNoAuthProvider());
        securityConfigTextArea.textProperty().addListener((obs, oldText, newText) -> {
            ViewUtils.highlightJsonInCodeArea(newText, securityConfigTextArea, false, objectMapper, jsonHighlighter);
        });
    }

    @Override
    public void setFields(ModalController modalController, Stage stage, Map<String, Object> text) {
        this.stage = stage;
        KafkaCluster kafkaCluster = (KafkaCluster) text.get("objectProperty");
        this.objectProperty.set(kafkaCluster);
        if (kafkaCluster != null) {
            setValuesForFields(kafkaCluster);
        }
    }

    private void setValuesForFields(KafkaCluster newValue) {
        clusterNameTextField.setText(newValue.getName());
        bootstrapServerTextField.setText(newValue.getBootstrapServer());
        schemaRegistryTextField.setText(newValue.getSchemaRegistryUrl());
        isOnlySubjectLoadedCheckBox.setSelected(newValue.isOnlySubjectLoaded());
        AuthConfig authConfig = newValue.getAuthConfig();
        AuthProvider authProvider = authProviderManager.getAuthProvider(authConfig.name());
        securityTypeComboxBox.getSelectionModel().select(authProvider);
        try {
            securityConfigTextArea.replaceText(authProvider.toConfigText(authConfig));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    protected void add() throws Exception {
        AuthProvider authProvider = securityTypeComboxBox.getValue();
        AuthConfig authConfig = authProvider.fromConfigText(securityConfigTextArea.getText());
        KafkaCluster kafkaCluster = KafkaCluster.builder(clusterNameTextField.getText(), bootstrapServerTextField.getText())
                .schemaRegistryUrl(schemaRegistryTextField.getText())
                .isOnlySubjectLoaded(isOnlySubjectLoadedCheckBox.isSelected())
                .authConfig(authConfig).build();
        modelRef.set(kafkaCluster);
        Stage stage = (Stage) addBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void cancel() throws IOException {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}
