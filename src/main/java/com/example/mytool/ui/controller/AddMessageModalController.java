package com.example.mytool.ui.controller;

import com.example.mytool.ui.TableViewConfigurer;
import com.example.mytool.ui.UIPropertyItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;

public class AddMessageModalController extends ModalController {

    @FXML
    private TextArea key;
    @FXML
    private TextArea value;
    @FXML
    private TextArea schema;
    @FXML
    private Button okBtn;
    @FXML
    private Button cancelBtn;

    @FXML
    private Tab headerTab;

    @FXML
    private TableView<UIPropertyItem> headerTable;

    @FXML
    void initialize() {
        TableViewConfigurer.configureTableView(UIPropertyItem.class, headerTable);
    }

    @FXML
    protected void ok() throws IOException {
        modelRef.set(Triple.of(key.getText(), value.getText(), schema.getText()));
        Stage stage = (Stage) okBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void cancel() throws IOException {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    public void configureEditableControls(boolean editable) {
        key.setEditable(editable);
        value.setEditable(editable);
        schema.setEditable(editable);
    }
}
