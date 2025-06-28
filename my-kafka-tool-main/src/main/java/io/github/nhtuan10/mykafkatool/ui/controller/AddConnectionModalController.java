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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AddConnectionModalController extends ModalController {
    private final ObjectProperty<KafkaCluster> objectProperty;

    private final JsonHighlighter jsonHighlighter;

    private final AuthProviderManager authProviderManager;

    private final ObjectMapper objectMapper;

    private final Map<AuthProvider, List<Hyperlink>> hyperlinks;

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

    @FXML
    private HBox sampleSecurityConfigContainer;

    @Inject
    public AddConnectionModalController(JsonHighlighter jsonHighlighter, AuthProviderManager authProviderManager, @RichTextFxObjectMapper ObjectMapper objectMapper) {
        this.objectProperty = new SimpleObjectProperty<>();
        this.jsonHighlighter = jsonHighlighter;
        this.authProviderManager = authProviderManager;
        this.objectMapper = objectMapper;
        this.hyperlinks = authProviderManager.getAllAuthProviders().stream().collect(Collectors.toMap(
                auth -> auth
                , authProvider -> authProvider.getSampleConfig().stream().map(sample -> {
                    Hyperlink hyperlink = new Hyperlink(sample.getKey());
                    hyperlink.setOnAction(event -> {
                        this.securityConfigTextArea.replaceText(sample.getValue());
                    });
                    return hyperlink;
                }).toList()));
    }

    @FXML
    public void initialize() {
//        if (objectProperty.get() != null) {
//            setValuesForFields(objectProperty.get());
//        }
//        objectProperty.addListener((observable, oldValue, newValue) -> {
//            if (newValue != null && !Objects.equals(newValue, oldValue)) {
//                setValuesForFields(newValue);
//            }
//        });
        securityTypeComboxBox.getItems().addAll(authProviderManager.getAllAuthProviders());
        securityTypeComboxBox.getSelectionModel().select(authProviderManager.getNoAuthProvider());
        securityConfigTextArea.textProperty().addListener((obs, oldText, newText) -> {
            ViewUtils.highlightJsonInCodeArea(newText, securityConfigTextArea, false, objectMapper, jsonHighlighter);
        });
        securityTypeComboxBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            sampleSecurityConfigContainer.getChildren().setAll(hyperlinks.get(newValue));
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
            ViewUtils.highlightJsonInCodeArea(authProvider.toConfigText(authConfig), securityConfigTextArea, true, objectMapper, jsonHighlighter);
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
