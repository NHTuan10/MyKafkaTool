package com.example.mytool.ui.controller;

import com.example.mytool.producer.Message;
import com.example.mytool.serde.SerdeUtil;
import com.example.mytool.ui.TableViewConfigurer;
import com.example.mytool.ui.UIPropertyItem;
import com.example.mytool.ui.util.ViewUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class AddMessageModalController extends ModalController {

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
    private Tab headerTab;

    @FXML
    private ComboBox<String> valueContentTypeComboBox;

    @FXML
    private TableView<UIPropertyItem> headerTable;

    @FXML
    void initialize() {
        TableViewConfigurer.configureTableView(UIPropertyItem.class, headerTable);
        valueContentTypeComboBox.setOnAction(event -> {
            if (valueContentTypeComboBox.getValue().equals(SerdeUtil.SERDE_AVRO)) {
                schemaTextArea.setDisable(false);
            } else {
                schemaTextArea.setDisable(true);
            }
        });
    }

    @FXML
    protected void ok() throws IOException {
        String schemaText = schemaTextArea.getText();
        String valueText = valueTextArea.getText();
        String valueContentTypeText = valueContentTypeComboBox.getValue();
        if (!MainController.validateSchema(valueContentTypeText, schemaText)) return;
        SerdeUtil.ValidationResult valueValidationResult = SerdeUtil.validateMessageAgainstSchema(valueContentTypeText, valueText, schemaText);
        if (!valueValidationResult.isValid()) {
            log.warn("The message is invalid against the schema", valueValidationResult.exception());
            ViewUtil.showAlertDialog(Alert.AlertType.WARNING, valueValidationResult.exception().getMessage(), "The message is invalid against the schema");
            return;
        }

        modelRef.set(new Message(keyTextArea.getText(), valueText, valueContentTypeText, schemaText));
        Stage stage = (Stage) okBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void cancel() throws IOException {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    public void configureEditableControls(boolean editable) {

        keyTextArea.setEditable(editable);
        valueTextArea.setEditable(editable);
//        valueContentTypeComboBox.setDisable(!editable);
        headerTable.setEditable(editable);
        valueContentTypeComboBox.getSelectionModel().selectFirst();
        if (!editable) {
            //suppress combox box drop down
            valueContentTypeComboBox.setOnShowing(event -> event.consume());
            ;
        }

        if (!editable || !SerdeUtil.SERDE_AVRO.equals(valueContentTypeComboBox.getValue())) {
            schemaTextArea.setDisable(true);
        }

    }
}
