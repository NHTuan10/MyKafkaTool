package io.github.nhtuan10.mykafkatool.ui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.api.model.DisplayType;
import io.github.nhtuan10.mykafkatool.api.model.KafkaMessage;
import io.github.nhtuan10.mykafkatool.api.serdes.PluggableDeserializer;
import io.github.nhtuan10.mykafkatool.api.serdes.PluggableSerializer;
import io.github.nhtuan10.mykafkatool.configuration.annotation.RichTextFxObjectMapper;
import io.github.nhtuan10.mykafkatool.producer.ProducerUtil;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageHeaderTable;
import io.github.nhtuan10.mykafkatool.ui.topic.UIPropertyTableItem;
import io.github.nhtuan10.mykafkatool.ui.util.ModalUtils;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import io.github.nhtuan10.mykafkatool.util.Utils;
import jakarta.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class AddOrViewMessageModalController extends ModalController {

    private final SerDesHelper serDesHelper;
    private final JsonHighlighter jsonHighlighter;
    private final ObjectMapper objectMapper;
    private final ProducerUtil producerUtil;
    private String valueContentType;
    private Alert helpDialog;
    private BooleanProperty editable;

    @FXML
    private TextArea keyTextArea;
    @FXML
    private CodeArea valueTextArea;
    @FXML
    private CodeArea schemaTextArea;
    @FXML
    private Button okBtn;
    @FXML
    private Button cancelBtn;
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

//    @FXML
//    private HBox handlebarsPreviewContainer;
//
//    @FXML
//    private TextField nthMsg;

    @Inject
    public AddOrViewMessageModalController(SerDesHelper serDesHelper, JsonHighlighter jsonHighlighter, @RichTextFxObjectMapper ObjectMapper objectMapper, ProducerUtil producerUtil) {
        this.serDesHelper = serDesHelper;
        this.jsonHighlighter = jsonHighlighter;
        this.objectMapper = objectMapper;
        this.producerUtil = producerUtil;
        this.helpDialog = buildHelpDialog();
        this.editable = new SimpleBooleanProperty();
    }

    @FXML
    void initialize() {
        valueContentTypeComboBox.setOnAction(event -> {
            enableDisableSchemaTextArea();
        });
        valueDisplayTypeComboBox.setItems(FXCollections.observableArrayList(DisplayType.values()));
        valueTextArea.textProperty().addListener((obs, oldText, newText) -> {
            refreshDisplayedValue(newText, valueTextArea, valueDisplayTypeComboBox.getValue(), false);
        });
        schemaTextArea.textProperty().addListener((obs, oldText, newText) -> {
            refreshDisplayedValue(newText, schemaTextArea, DisplayType.JSON, false);
        });
//        expressionHelpLink.setText(" ?⃝");
        expressionHelpLink.setOnAction((e) -> showHelp());
        previewHandlebars.visibleProperty().bind(isHandlebarsEnabled.selectedProperty());
//        handlebarsPreviewContainer.visibleProperty().bind(isHandlebarsEnabled.selectedProperty());
        StringProperty origKey = new SimpleStringProperty(keyTextArea.getText());
        StringProperty origValue = new SimpleStringProperty(valueTextArea.getText());
        previewHandlebars.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                if (newVal) {
                    origKey.setValue(keyTextArea.getText());
                    origValue.setValue(valueTextArea.getText());
                    try {
                        keyTextArea.setText(Utils.evalHandleBar(keyTextArea.getText()));
                        refreshDisplayedValue(Utils.evalHandleBar(valueTextArea.getText()), valueTextArea, valueDisplayTypeComboBox.getValue(), true);
                    } catch (Exception e) {
                        showHandlebarsEvalError(e);
                        previewHandlebars.setSelected(false);
                    }
                } else {
                    keyTextArea.setText(origKey.get());
                    refreshDisplayedValue(origValue.get(), valueTextArea, valueDisplayTypeComboBox.getValue(), false);
                }
            }

        });
        var handlebarsEditableCheck = isHandlebarsEnabled.selectedProperty().not().or(previewHandlebars.selectedProperty().not());
        keyTextArea.editableProperty().bind(editable.and(handlebarsEditableCheck));
        valueTextArea.editableProperty().bind(editable.and(handlebarsEditableCheck));
    }

    private void showHelp() {
        helpDialog.showAndWait();
    }

    private Alert buildHelpDialog() {
        TextArea textArea = new TextArea("YOUR_MESSAGE_HERE");
        textArea.setEditable(false);
        textArea.setWrapText(true);
        AnchorPane anchorPane = new AnchorPane();
//        GridPane gridPane = new GridPane();
//        gridPane.setMaxWidth(Double.MAX_VALUE);
//        gridPane.add(textArea, 0, 0);
//        gridPane.
        anchorPane.getChildren().add(textArea);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Stuff");
        alert.getDialogPane().setContent(anchorPane);
        alert.setResizable(true);
        return alert;
    }

    private void enableDisableSchemaTextArea() {
        PluggableSerializer serializer = serDesHelper.getPluggableSerialize(valueContentTypeComboBox.getValue());
        schemaTextArea.setDisable(!serializer.mayNeedUserInputForSchema());
    }

    @FXML
    protected void ok() {
        String schemaText = schemaTextArea.getText();
        String keyText = keyTextArea.getText();
        String valueText = valueTextArea.getText();
        String valueContentTypeText = valueContentTypeComboBox.getValue();
        int numberOfMessages = Integer.parseInt(numMsgToSend.getText());

        if (!validateSchema(valueContentTypeText, schemaText)) return;
        SerDesHelper.ValidationResult valueValidationResult = serDesHelper.validateMessageAgainstSchema(valueContentTypeText, valueText, schemaText);
        if (!valueValidationResult.isValid()) {
            log.warn("The message is invalid against the schema", valueValidationResult.exception());
            ModalUtils.showAlertDialog(Alert.AlertType.WARNING, valueValidationResult.exception().getMessage(), "The message is invalid against the schema");
            return;
        }

        Map<String, byte[]> headers = headerTable.getItems().stream().collect(Collectors.toMap(UIPropertyTableItem::getName, (item) -> item.getValue().getBytes(StandardCharsets.UTF_8)));
        List<String> keys, values;
        if (isHandlebarsEnabled.isSelected()) {
            try {
                keys = Utils.evalHandleBar(keyText, numberOfMessages);
                values = Utils.evalHandleBar(valueText, numberOfMessages);
            } catch (Exception e) {
                showHandlebarsEvalError(e);
                return;
            }
        } else {
            keys = IntStream.range(0, numberOfMessages).mapToObj(i -> keyText).toList();
            values = IntStream.range(0, numberOfMessages).mapToObj(i -> valueText).toList();
        }

        var kafkaMessages = IntStream.range(0, numberOfMessages).mapToObj((i) ->
                new KafkaMessage(keys.get(i), values.get(i), valueContentTypeText, schemaText, headers)
        ).toList();
        modelRef.set(kafkaMessages);
        Stage stage = (Stage) okBtn.getScene().getWindow();
        stage.close();
    }

    private void showHandlebarsEvalError(Exception e) {
        ModalUtils.showAlertDialog(Alert.AlertType.WARNING, e.getMessage(), "Error evaluating handlebars expression");
    }

    @FXML
    protected void cancel() throws IOException {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
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

        DisplayType displayType;

        if (editable) { // For Add Message Modal
            multipleSendOptionContainer.setVisible(true);
            multipleSendOptionContainer.setManaged(true);
            if (valueContentType != null && serDesHelper.getPluggableSerialize(valueContentType) != null) {
                valueContentTypeComboBox.getSelectionModel().select(valueContentType);
            }
            displayType = Optional.ofNullable(serDesHelper.getPluggableSerialize(valueContentType))
                    .map(PluggableSerializer::getDisplayType).orElse(DisplayType.TEXT);
            enableDisableSchemaTextArea();
        } else { // For View Message Modal
//            choiceButtonContainer.setVisible(false);
//            choiceButtonContainer.setMinHeight(0);
//            choiceButtonContainer.setPrefHeight(0);
            multipleSendOptionContainer.setVisible(false);
            multipleSendOptionContainer.setManaged(false);
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
                ViewUtils.highlightJsonInCodeArea(inValue, codeArea, prettyPrint, objectMapper, jsonHighlighter);
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
