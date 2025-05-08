package com.example.mytool.ui.control;

import com.example.mytool.MyApplication;
import com.example.mytool.ui.TableViewConfigurer;
import com.example.mytool.ui.UIPropertyItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class EditableTableControl extends AnchorPane {

    @FXML
    private Button addItemBtn;

    @FXML
    private Button removeItemBtn;

    @FXML
    private TableView<UIPropertyItem> table;

    private ObservableList<UIPropertyItem> tableItems;

    public EditableTableControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(MyApplication.class.getResource(
                "editable-table.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    void initialize() {
        TableViewConfigurer.configureTableView(UIPropertyItem.class, table);

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableItems = FXCollections.observableArrayList();
        table.setItems(tableItems);

    }


    @FXML
    protected void addItem() {
        tableItems.add(new UIPropertyItem("", ""));
    }

    @FXML
    protected void removeItem() {
        List<Integer> indicesToRemove = table.getSelectionModel().getSelectedIndices().reversed();
        indicesToRemove.forEach((i) -> {
            tableItems.remove((int) i);
        });
    }

    public void configureEditableControls(boolean editable) {
        table.setEditable(editable);
        if (editable) {
            TableViewConfigurer.configureEditableKeyValueTable(table);
        }
    }

}
