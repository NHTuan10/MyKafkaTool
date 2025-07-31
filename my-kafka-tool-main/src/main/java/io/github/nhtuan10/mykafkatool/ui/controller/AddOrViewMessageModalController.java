package io.github.nhtuan10.mykafkatool.ui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.api.model.DisplayType;
import io.github.nhtuan10.mykafkatool.api.model.KafkaMessage;
import io.github.nhtuan10.mykafkatool.api.serdes.PluggableDeserializer;
import io.github.nhtuan10.mykafkatool.api.serdes.PluggableSerializer;
import io.github.nhtuan10.mykafkatool.configuration.annotation.RichTextFxObjectMapper;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.producer.ProducerUtil;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import io.github.nhtuan10.mykafkatool.ui.UIErrorHandler;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.event.EventDispatcher;
import io.github.nhtuan10.mykafkatool.ui.event.PartitionUIEvent;
import io.github.nhtuan10.mykafkatool.ui.event.TopicUIEvent;
import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageHeaderTable;
import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageHeaderTableItem;
import io.github.nhtuan10.mykafkatool.ui.util.ModalUtils;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import io.github.nhtuan10.mykafkatool.util.Utils;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class AddOrViewMessageModalController extends ModalController {

    private final SerDesHelper serDesHelper;
    private final JsonHighlighter jsonHighlighter;
    private final ObjectMapper objectMapper;
    private final ProducerUtil producerUtil;
    private final Alert helpDialog;
    private final EventDispatcher eventDispatcher;
    private final BooleanProperty editable;
    private final BooleanProperty isBusy;
    private String valueContentType;
    private StringProperty keyTemplate;
    private StringProperty valueTemplate;
    private KafkaTopic kafkaTopic;
    private KafkaPartition kafkaPartition;

    @FXML
    private TextArea keyTextArea;
    @FXML
    private CodeArea valueTextArea;
    @FXML
    private CodeArea schemaTextArea;
    @FXML
    private Button closeBtn;
    @FXML
    private Pane choiceButtonContainer;
    @FXML
    private SplitPane splitPane;
    @FXML
    private Tab headerTab;

    @FXML
    private ComboBox<String> valueContentTypeComboBox;

    @FXML
    private ComboBox<DisplayType> valueDisplayTypeComboBox;

    @FXML
    private KafkaMessageHeaderTable headerTable;

    @FXML
    private TextField numMsgToSend;

    @FXML
    private HBox multipleSendOptionContainer;

    @FXML
    private CheckBox isHandlebarsEnabled;

    @FXML
    private Hyperlink expressionHelpLink;

    @FXML
    private CheckBox previewHandlebars;

    @FXML
    private HBox handlebarsPreviewContainer;

    @FXML
    private TextField nthMsg;

    @FXML
    private Label howManyLabel;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Label clusterTopicAndPartitionInfo;

    @Inject
    public AddOrViewMessageModalController(SerDesHelper serDesHelper, JsonHighlighter jsonHighlighter, @RichTextFxObjectMapper ObjectMapper objectMapper,
                                           ProducerUtil producerUtil, EventDispatcher eventDispatcher) {
        this.serDesHelper = serDesHelper;
        this.jsonHighlighter = jsonHighlighter;
        this.objectMapper = objectMapper;
        this.producerUtil = producerUtil;
        try {
            this.helpDialog = ModalUtils.buildHelpDialog("handlebars-help.txt", "Handlebars Expression Help");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.editable = new SimpleBooleanProperty(true);
        this.eventDispatcher = eventDispatcher;
        this.isBusy = new SimpleBooleanProperty(false);
    }

    @FXML
    void initialize() {
        valueContentTypeComboBox.setOnAction(event -> {
            enableDisableSchemaTextArea();
            DisplayType displayType = serDesHelper.getPluggableSerialize(valueContentTypeComboBox.getValue()).getDisplayType();
            valueDisplayTypeComboBox.getSelectionModel().select(displayType);
        });
        valueDisplayTypeComboBox.setItems(FXCollections.observableArrayList(DisplayType.values()));
        valueTextArea.textProperty().addListener((obs, oldText, newText) -> {
            refreshDisplayedValue(newText, valueTextArea, valueDisplayTypeComboBox.getValue(), false);
            if (isHandlebarsEnabled.isSelected() && !previewHandlebars.isSelected()) {
                this.valueTemplate.set(this.valueTextArea.getText());
            }
        });

        keyTextArea.textProperty().addListener((obs, oldText, newText) -> {
            if (isHandlebarsEnabled.isSelected() && !previewHandlebars.isSelected()) {
                this.keyTemplate.set(this.keyTextArea.getText());
            }
        });

        schemaTextArea.textProperty().addListener((obs, oldText, newText) -> {
            refreshDisplayedValue(newText, schemaTextArea, DisplayType.JSON, false);
        });
//        expressionHelpLink.setText(" ?⃝");
        expressionHelpLink.setOnAction((e) -> showHelp());
//        previewHandlebars.visibleProperty().bind(isHandlebarsEnabled.selectedProperty());
        handlebarsPreviewContainer.visibleProperty().bind(isHandlebarsEnabled.selectedProperty());
        keyTemplate = new SimpleStringProperty(keyTextArea.getText());
        valueTemplate = new SimpleStringProperty(valueTextArea.getText());
        isHandlebarsEnabled.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal && !previewHandlebars.isSelected()) {
                keyTemplate.set(keyTextArea.getText());
                valueTemplate.set(valueTextArea.getText());
            }
        });
        previewHandlebars.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                if (newVal) {
                    keyTemplate.setValue(keyTextArea.getText());
                    valueTemplate.setValue(valueTextArea.getText());
                    int n = 1;
                    if (StringUtils.isNotBlank(nthMsg.getText()))
                        n = Integer.parseInt(nthMsg.getText());
                    nthMsg.setText(String.valueOf(n));
                    previewKeyAndValueHandlebars(n, keyTextArea.getText(), valueTextArea.getText());
                } else {
                    keyTextArea.setText(keyTemplate.get());
                    refreshDisplayedValue(valueTemplate.get(), valueTextArea, valueDisplayTypeComboBox.getValue(), false);
                }
            }

        });
        nthMsg.textProperty().addListener((obs, oldVal, newVal) -> {
            int n;
            if (isHandlebarsEnabled.isSelected() && previewHandlebars.isSelected() && StringUtils.isNotBlank(newVal) && (n = Integer.parseInt(newVal)) > 0) {
                previewKeyAndValueHandlebars(n, keyTemplate.get(), valueTemplate.get());
            }
        });
        var handlebarsEditableCheck = isHandlebarsEnabled.selectedProperty().not().or(previewHandlebars.selectedProperty().not());
        keyTextArea.editableProperty().bind(editable.and(handlebarsEditableCheck));
        valueTextArea.editableProperty().bind(editable.and(handlebarsEditableCheck));
        howManyLabel.visibleProperty().bind(editable);
        howManyLabel.managedProperty().bind(editable);
        multipleSendOptionContainer.visibleProperty().bind(editable);
        multipleSendOptionContainer.managedProperty().bind(editable);
//        progressIndicator.setIndeterminate(true);
        progressIndicator.visibleProperty().bind(isBusy);
    }

    private void previewKeyAndValueHandlebars(int n, String keyTemplate, String valueTemplate) {
        try {
            keyTextArea.setText(Utils.evalHandlebarsAtNth(keyTemplate, n));
            refreshDisplayedValue(Utils.evalHandlebarsAtNth(valueTemplate, n), valueTextArea, valueDisplayTypeComboBox.getValue(), true);
        } catch (Exception e) {
            showHandlebarsEvalError(e);
            previewHandlebars.setSelected(false);
        }
    }

    private void showHelp() {
        if (!helpDialog.isShowing()) {
            helpDialog.showAndWait();
        }
    }

    private void enableDisableSchemaTextArea() {
        PluggableSerializer serializer = serDesHelper.getPluggableSerialize(valueContentTypeComboBox.getValue());
        schemaTextArea.setDisable(!serializer.mayNeedUserInputForSchema());
    }

    @FXML
    protected void send() {
        String schemaText = schemaTextArea.getText();
        String keyText = keyTextArea.getText();
        String valueText = valueTextArea.getText();
        String valueContentTypeText = valueContentTypeComboBox.getValue();
        int numberOfMessages = Integer.parseInt(numMsgToSend.getText());

        if (!validateSchema(valueContentTypeText, schemaText)) return;
        Callable<Integer> task = () -> {
            Platform.runLater(() -> {
                this.isBusy.set(true);
                progressIndicator.setProgress(-1);
            });
            Map<String, byte[]> headers = headerTable.getItems().stream().collect(Collectors.toMap(KafkaMessageHeaderTableItem::getKey, (item) -> item.getValue().getBytes(StandardCharsets.UTF_8)));
            List<String> keys, values;
            if (isHandlebarsEnabled.isSelected()) {
                try {
                    keys = Utils.evalHandlebars(keyTemplate.get(), numberOfMessages);
                    values = Utils.evalHandlebars(valueTemplate.get(), numberOfMessages);
                } catch (Exception e) {
                    showHandlebarsEvalError(e);
                    return 0;
                }
            } else {
                keys = IntStream.range(0, numberOfMessages).mapToObj(i -> keyText).toList();
                values = IntStream.range(0, numberOfMessages).mapToObj(i -> valueText).toList();
            }

//        var kafkaMessages = IntStream.range(0, numberOfMessages).mapToObj((i) -> {
            var kafkaMessages = new ArrayList<KafkaMessage>();
            for (int i = 0; i < numberOfMessages; i++) {
                SerDesHelper.ValidationResult valueValidationResult = serDesHelper.validateMessageAgainstSchema(valueContentTypeText, valueText, schemaText);
                if (!valueValidationResult.isValid()) {
                    log.warn("The message is invalid against the schema", valueValidationResult.exception());
                    ModalUtils.showAlertDialog(Alert.AlertType.WARNING, valueValidationResult.exception().getMessage(), "The message is invalid against the schema");
                    return 0;
                }
                kafkaMessages.add(new KafkaMessage(keys.get(i), values.get(i), valueContentTypeText, schemaText, headers));
            }
//        }).toList();

            for (var msg : kafkaMessages) {
                producerUtil.sendMessage(kafkaTopic, kafkaPartition, msg);
            }
            return kafkaMessages.size();
        };
        ViewUtils.runBackgroundTask(task, (count) -> {
            this.isBusy.set(false);
            eventDispatcher.publishEvent(TopicUIEvent.newRefreshTopicEven(kafkaTopic));
            if (kafkaPartition != null) {
                eventDispatcher.publishEvent(PartitionUIEvent.newRefreshPartitionEven(kafkaPartition));
            }
            modelRef.set(modelRef.get() != null ? (Integer) modelRef.get() + count : count);
            ModalUtils.showAlertDialog(Alert.AlertType.INFORMATION, "Added %s message successfully!".formatted(count), "Successfully Added!", ButtonType.OK);
        }, (ex) -> {
            this.isBusy.set(false);
            UIErrorHandler.showError(Thread.currentThread(), ex);
        });

//        Stage stage = (Stage) okBtn.getScene().getWindow();
//        stage.close();
    }

    private void showHandlebarsEvalError(Exception e) {
        ModalUtils.showAlertDialog(Alert.AlertType.WARNING, e.getMessage(), "Error evaluating handlebars expression");
    }

    @FXML
    protected void close() throws IOException {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }


    public void launch(boolean editable) {
        super.launch(editable);
        this.editable.set(editable);
        this.headerTable.setStage(stage);
//        keyTextArea.setEditable(editable);
//        valueTextArea.setEditable(editable);
        final String initValue = valueTextArea.getText();
        valueDisplayTypeComboBox.setOnAction(event -> {
            valueDisplayTypeToggleEventAction(editable, initValue);
        });
        //TODO:[Low Priority] Set value on below combox box based on the valueContentTypeComboBox
        valueContentTypeComboBox.getSelectionModel().selectFirst();
        headerTable.setEditable(editable);

        DisplayType displayType = DisplayType.TEXT;

        if (editable) { // For Add Message Modal
            valueContentTypeComboBox.getItems().setAll(FXCollections.observableArrayList(serDesHelper.getSupportedValueSerializer()));
            if (valueContentType != null && serDesHelper.getPluggableSerialize(valueContentType) != null) {
                valueContentTypeComboBox.getSelectionModel().select(valueContentType);
            } else {
                valueContentTypeComboBox.getSelectionModel().selectFirst();
            }
            if (serDesHelper.getPluggableSerialize(valueContentType) != null) {
                displayType = serDesHelper.getPluggableSerialize(valueContentType).getDisplayType();
            } else if (serDesHelper.getPluggableDeserialize(valueContentType) != null) {
                displayType = serDesHelper.getPluggableDeserialize(valueContentType).getDisplayType();
            }
            enableDisableSchemaTextArea();
        } else { // For View Message Modal
//            choiceButtonContainer.setVisible(false);
//            choiceButtonContainer.setMinHeight(0);
//            choiceButtonContainer.setPrefHeight(0);
//            multipleSendOptionContainer.setVisible(false);
//            multipleSendOptionContainer.setManaged(false);
            splitPane.getItems().remove(choiceButtonContainer);
//            ( (SplitPane) choiceButtonContainer.getParent()).getItems().remove(choiceButtonContainer);
            if (valueContentType != null && serDesHelper.getPluggableDeserialize(valueContentType) != null) {
                valueContentTypeComboBox.getSelectionModel().select(valueContentType);
            }
            displayType = Optional.ofNullable(serDesHelper.getPluggableDeserialize(valueContentType))
                    .map(PluggableDeserializer::getDisplayType).orElse(DisplayType.TEXT);
            valueDisplayTypeComboBox.getSelectionModel().select(displayType);
            //suppress combox box drop down
            valueContentTypeComboBox.setOnShowing(Event::consume);
            schemaTextArea.setEditable(false);
        }
        valueDisplayTypeComboBox.getSelectionModel().select(displayType);
        valueDisplayTypeToggleEventAction(true, initValue);
        String text = kafkaPartition != null ?
                "Cluster: %s - Topic: %s - Partition: %s".formatted(kafkaTopic.cluster().getName(), kafkaTopic.name(), kafkaPartition.id()) :
                "Cluster: %s - Topic: %s".formatted(kafkaTopic.cluster().getName(), kafkaTopic.name());
        this.clusterTopicAndPartitionInfo.setText(text);
    }

    private void valueDisplayTypeToggleEventAction(boolean editable, String initValue) {
        if (!editable) {
            refreshDisplayedValue(initValue, valueTextArea, valueDisplayTypeComboBox.getValue(), true);
        }
        refreshDisplayedValue(valueTextArea.getText(), valueTextArea, valueDisplayTypeComboBox.getValue(), true);
    }

    private void refreshDisplayedValue(String inValue, CodeArea codeArea, DisplayType displayType, boolean prettyPrint) {
        if (StringUtils.isNotBlank(inValue)) {

            if (displayType == DisplayType.JSON) {
                ViewUtils.setValueAndHighlightJsonInCodeArea(inValue, codeArea, prettyPrint, objectMapper, jsonHighlighter);
            } else if (displayType == DisplayType.TEXT) {
//                if (prettyPrint) {
                codeArea.replaceText(inValue);
//                }
                codeArea.clearStyle(0, inValue.length());
                StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<Collection<String>>()
                        .add(List.of(JsonHighlighter.NORMAL_TEXT), inValue.length());
                codeArea.setStyleSpans(0, spansBuilder.create());
            }

        }
    }

    private boolean validateSchema(String valueContentType, String schema) {
        boolean valid = SerDesHelper.isValidSchemaForSerialization(serDesHelper, valueContentType, schema);
        if (!valid) {
            ModalUtils.showAlertDialog(Alert.AlertType.WARNING, "Schema is invalid", null,
                    ButtonType.OK);
        }
        return valid;
    }

}
