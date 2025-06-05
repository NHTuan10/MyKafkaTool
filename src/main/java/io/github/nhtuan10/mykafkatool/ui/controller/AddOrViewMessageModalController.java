package io.github.nhtuan10.mykafkatool.ui.controller;

import io.github.nhtuan10.mykafkatool.api.DisplayType;
import io.github.nhtuan10.mykafkatool.api.PluggableDeserializer;
import io.github.nhtuan10.mykafkatool.api.PluggableSerializer;
import io.github.nhtuan10.mykafkatool.api.model.KafkaMessage;
import io.github.nhtuan10.mykafkatool.serdes.AvroUtil;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import io.github.nhtuan10.mykafkatool.ui.UIPropertyTableItem;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.control.KafkaMessageHeaderTable;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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

@Slf4j
public class AddOrViewMessageModalController extends ModalController {

    private SerDesHelper serDesHelper;
    private String valueContentType;

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

    private final JsonHighlighter jsonHighlighter = new JsonHighlighter();

    @FXML
    void initialize() {
        valueContentTypeComboBox.setOnAction(event -> {
            enableDisableSchemaTextArea();
        });
        valueDisplayTypeComboBox.setItems(FXCollections.observableArrayList(DisplayType.values()));
        valueTextArea.textProperty().addListener((obs, oldText, newText) -> {
            refreshDisplayValue(newText, valueTextArea, valueDisplayTypeComboBox.getValue(), false);
        });
        schemaTextArea.textProperty().addListener((obs, oldText, newText) -> {
            refreshDisplayValue(newText, schemaTextArea, DisplayType.JSON, false);
        });
    }

    private void enableDisableSchemaTextArea() {
        PluggableSerializer serializer = serDesHelper.getPluggableSerialize(valueContentTypeComboBox.getValue());
        schemaTextArea.setDisable(!serializer.mayNeedUserInputForSchema());
    }

    @FXML
    protected void ok() {
        String schemaText = schemaTextArea.getText();
        String valueText = valueTextArea.getText();
        String valueContentTypeText = valueContentTypeComboBox.getValue();
        if (!validateSchema(valueContentTypeText, schemaText)) return;
        SerDesHelper.ValidationResult valueValidationResult = serDesHelper.validateMessageAgainstSchema(valueContentTypeText, valueText, schemaText);
        if (!valueValidationResult.isValid()) {
            log.warn("The message is invalid against the schema", valueValidationResult.exception());
            ViewUtil.showAlertDialog(Alert.AlertType.WARNING, valueValidationResult.exception().getMessage(), "The message is invalid against the schema");
            return;
        }

        Map<String, byte[]> headers = headerTable.getItems().stream().collect(Collectors.toMap(UIPropertyTableItem::getName, (item) -> item.getValue().getBytes(StandardCharsets.UTF_8)));
        modelRef.set(new KafkaMessage(keyTextArea.getText(), valueText, valueContentTypeText, schemaText, headers));
        Stage stage = (Stage) okBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void cancel() throws IOException {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }


    public void launch(boolean editable) {
        super.launch(editable);
        this.headerTable.setStage(stage);
        keyTextArea.setEditable(editable);
        valueTextArea.setEditable(editable);
        final String initValue = valueTextArea.getText();
        valueDisplayTypeComboBox.setOnAction(event -> {
            valueDisplayTypeToggleEventAction(editable, initValue);
        });
        //TODO: Set value on below combox box based on the valueContentTypeComboBox
        valueContentTypeComboBox.getSelectionModel().selectFirst();
        headerTable.setEditable(editable);

        DisplayType displayType;

        if (editable) { // For Add Message Modal
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
            refreshDisplayValue(initValue, valueTextArea, valueDisplayTypeComboBox.getValue(), true);
        }
        refreshDisplayValue(valueTextArea.getText(), valueTextArea, valueDisplayTypeComboBox.getValue(), true);
    }

    private void refreshDisplayValue(String inValue, CodeArea codeArea, DisplayType displayType, boolean prettyPrint) {
        if (StringUtils.isNotBlank(inValue)) {

            if (displayType == DisplayType.JSON) {
                ViewUtil.highlightJsonInCodeArea(inValue, codeArea, prettyPrint, AvroUtil.OBJECT_MAPPER, jsonHighlighter);
            } else if (displayType == DisplayType.TEXT) {
                if (prettyPrint) {
                    codeArea.replaceText(inValue);
                }
                String code = codeArea.getText();
                codeArea.clearStyle(0, code.length());
                StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<Collection<String>>()
                        .add(List.of(JsonHighlighter.NORMAL_TEXT), code.length());
                codeArea.setStyleSpans(0, spansBuilder.create());
            }

        }
    }

    private boolean validateSchema(String valueContentType, String schema) {
        boolean valid = SerDesHelper.isValidSchemaForSerialization(serDesHelper, valueContentType, schema);
        if (!valid) {
            ViewUtil.showAlertDialog(Alert.AlertType.WARNING, "Schema is invalid", null,
                    ButtonType.OK);
        }
        return valid;
    }

}
