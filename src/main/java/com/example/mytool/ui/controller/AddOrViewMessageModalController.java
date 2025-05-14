package com.example.mytool.ui.controller;

import com.example.mytool.api.KafkaMessage;
import com.example.mytool.api.PluggableSerializer;
import com.example.mytool.serdes.SerdeUtil;
import com.example.mytool.ui.TableViewConfigurer;
import com.example.mytool.ui.UIPropertyTableItem;
import com.example.mytool.ui.util.ViewUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AddOrViewMessageModalController extends ModalController {

    private SerdeUtil serdeUtil;

    @FXML
    private TextArea keyTextArea;
    @FXML
    private TextArea valueTextArea;
    @FXML
    private TextArea schemaTextArea;
    @FXML
    private Button okBtn;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button addHeaderBtn;

    @FXML
    private Button removeHeaderBtn;
    @FXML
    private Tab headerTab;

    @FXML
    private ComboBox<String> valueContentTypeComboBox;

    @FXML
    private TableView<UIPropertyTableItem> headerTable;

    private ObservableList<UIPropertyTableItem> headerItems;

    @FXML
    void initialize() {
        TableViewConfigurer.configureTableView(UIPropertyTableItem.class, headerTable);

        headerTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        headerItems = FXCollections.observableArrayList();
        headerTable.setItems(headerItems);
        valueContentTypeComboBox.setOnAction(event -> {
            enableDisableSchemaTextArea();
        });
    }

    private void enableDisableSchemaTextArea() {
        PluggableSerializer serializer = serdeUtil.getPluggableSerialize(valueContentTypeComboBox.getValue());
        schemaTextArea.setDisable(!serializer.isUserSchemaInputRequired());
    }

    @FXML
    protected void ok() {
        String schemaText = schemaTextArea.getText();
        String valueText = valueTextArea.getText();
        String valueContentTypeText = valueContentTypeComboBox.getValue();
        if (!validateSchema(valueContentTypeText, schemaText)) return;
        SerdeUtil.ValidationResult valueValidationResult = serdeUtil.validateMessageAgainstSchema(valueContentTypeText, valueText, schemaText);
        if (!valueValidationResult.isValid()) {
            log.warn("The message is invalid against the schema", valueValidationResult.exception());
            ViewUtil.showAlertDialog(Alert.AlertType.WARNING, valueValidationResult.exception().getMessage(), "The message is invalid against the schema");
            return;
        }

        Map<String, String> headers = headerItems.stream().collect(Collectors.toMap(UIPropertyTableItem::getName, UIPropertyTableItem::getValue));
        modelRef.set(new KafkaMessage(keyTextArea.getText(), valueText, valueContentTypeText, schemaText, headers));
        Stage stage = (Stage) okBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void cancel() throws IOException {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void addHeader() {
        headerItems.add(new UIPropertyTableItem("", ""));
    }

    @FXML
    protected void removeHeader() {
        List<Integer> indicesToRemove = headerTable.getSelectionModel().getSelectedIndices().reversed();
        indicesToRemove.forEach((i) -> headerItems.remove((int) i));
    }

    public void configureEditableControls(boolean editable) {

        keyTextArea.setEditable(editable);
        valueTextArea.setEditable(editable);
//        valueContentTypeComboBox.setDisable(!editable);
        headerTable.setEditable(editable);
        valueContentTypeComboBox.getSelectionModel().selectFirst();
        if (editable) {
            TableViewConfigurer.configureEditableKeyValueTable(headerTable);
            enableDisableSchemaTextArea();
        } else {
            //suppress combox box drop down
            valueContentTypeComboBox.setOnShowing(Event::consume);
            schemaTextArea.setEditable(false);
        }
    }

    private boolean validateSchema(String valueContentType, String schema) {
        boolean valid = SerdeUtil.isValidSchemaForSerialization(serdeUtil, valueContentType, schema);
        if (!valid) {
            ViewUtil.showAlertDialog(Alert.AlertType.WARNING, "Schema is invalid", null,
                    ButtonType.OK);
        }
        return valid;
    }

}
