package com.example.mytool.ui.controller;

import com.example.mytool.api.KafkaMessage;
import com.example.mytool.api.PluggableSerializer;
import com.example.mytool.serde.SerdeUtil;
import com.example.mytool.ui.TableViewConfigurer;
import com.example.mytool.ui.UIPropertyItem;
import com.example.mytool.ui.util.EditingTableCell;
import com.example.mytool.ui.util.ViewUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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
    private TableView<UIPropertyItem> headerTable;

    private ObservableList<UIPropertyItem> headerItems;

    @FXML
    void initialize() {
        TableViewConfigurer.configureTableView(UIPropertyItem.class, headerTable);

        headerTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        headerItems = FXCollections.observableArrayList();
        headerTable.setItems(headerItems);
        valueContentTypeComboBox.setOnAction(event -> {
//            if (valueContentTypeComboBox.getValue().equals(SerdeUtil.SERDE_AVRO)) {
//                schemaTextArea.setDisable(false);
//            } else {
//                schemaTextArea.setDisable(true);
//            }
            enableDisableSchemaTextArea();
        });
    }

    private void enableDisableSchemaTextArea() {
        PluggableSerializer serializer = serdeUtil.getPluggableSerialize(valueContentTypeComboBox.getValue());
        schemaTextArea.setDisable(!serializer.isUserSchemaInputRequired());
    }

    @FXML
    protected void ok() throws IOException {
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

        Map<String, String> headers = headerItems.stream().collect(Collectors.toMap(UIPropertyItem::getName, UIPropertyItem::getValue));
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
        headerItems.add(new UIPropertyItem("", ""));
    }

    @FXML
    protected void removeHeader() {
        List<Integer> indicesToRemove = headerTable.getSelectionModel().getSelectedIndices().reversed();
        indicesToRemove.forEach((i) -> {
            headerItems.remove((int) i);
        });
    }

    public void configureEditableControls(boolean editable) {

        keyTextArea.setEditable(editable);
        valueTextArea.setEditable(editable);
//        valueContentTypeComboBox.setDisable(!editable);
        headerTable.setEditable(editable);
        valueContentTypeComboBox.getSelectionModel().selectFirst();
        if (editable) {
            headerTable.setEditable(true);

            Callback<TableColumn<UIPropertyItem, String>,
                    TableCell<UIPropertyItem, String>> cellFactory
                    = (TableColumn<UIPropertyItem, String> p) -> new EditingTableCell();

            TableColumn<UIPropertyItem, String> nameColumn = (TableColumn<UIPropertyItem, String>) headerTable.getColumns().get(0);
//            nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
//            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
//            nameColumn.setCellFactory((tableColumn)-> new EditingTableCell()); // Use TextField for editing
            nameColumn.setCellFactory(cellFactory); // Use TextField for editing
            nameColumn.setOnEditCommit(event -> {
                // Update the model when editing is committed
//                UIPropertyItem row = event.getRowValue();
//                row.setName(event.getNewValue());
                event.getTableView().getItems().get(
                        event.getTablePosition().getRow()).setName(event.getNewValue());
            });
//            nameColumn.setOnEditCancel(event -> {
//                event.getRowValue();
//            });

            TableColumn<UIPropertyItem, String> valueColumn = (TableColumn<UIPropertyItem, String>) headerTable.getColumns().get(1);

//            valueColumn.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
//            valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
//            valueColumn.setCellFactory(TextFieldTableCell.forTableColumn()); // Use TextField for editing
//            valueColumn.setCellFactory((tableColumn)-> new EditingTableCell());
            valueColumn.setCellFactory(cellFactory);
            valueColumn.setOnEditCommit(event -> {
                // Update the model when editing is committed
//                UIPropertyItem row = event.getRowValue();
//                row.setValue(event.getNewValue());
                event.getTableView().getItems().get(
                        event.getTablePosition().getRow()).setValue(event.getNewValue());
            });
            enableDisableSchemaTextArea();
        } else {

            //suppress combox box drop down
            valueContentTypeComboBox.setOnShowing(event -> event.consume());
            schemaTextArea.setEditable(false);
//            if (!SerdeUtil.SERDE_AVRO.equals(valueContentTypeComboBox.getValue())) {
//                schemaTextArea.setDisable(true);
//            }
        }
    }

    private boolean validateSchema(String valueContentType, String schema) {
        boolean valid = true;
        PluggableSerializer serializer = serdeUtil.getPluggableSerialize(valueContentType);
        if (serializer.isUserSchemaInputRequired()) {
            try {
                if (StringUtils.isNotBlank(schema) &&
                        serializer.parseSchema(schema) != null) {
                    valid = true;
                } else {
                    valid = false;
                }
            } catch (Exception e) {
                log.warn("Error when parse schema", e);
                valid = false;
            }
        }
        if (!valid) {
            ViewUtil.showAlertDialog(Alert.AlertType.WARNING, "Schema is invalid", null,
                    ButtonType.OK);
        }
        return valid;
    }
}
