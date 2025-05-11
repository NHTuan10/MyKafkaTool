package com.example.mytool.ui.control;

import com.example.mytool.MyApplication;
import com.example.mytool.ui.TableViewConfigurer;
import com.example.mytool.ui.util.ViewUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.List;

@Slf4j
public class EditableTableControl<T> extends AnchorPane {

    @FXML
    protected Button addItemBtn;

    @FXML
    protected Button removeItemBtn;

    @FXML
    protected Button refreshBtn;

    @FXML
    protected TableView<T> table;

    protected ObservableList<T> tableItems;

    protected Class<T> itemClass;

//    private final TypeToken<T> typeToken = new TypeToken<T>(getClass()) { };

//    private final Type type = typeToken.getType();

    public EditableTableControl() {
        itemClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
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
    protected void initialize() {
        table.getColumns().clear();
        List<String> itemClassFields = ViewUtil.getPropertyFieldNamesFromTableItem(itemClass);
        itemClassFields.forEach(fieldName -> {
            String columnName = StringUtils.capitalize(StringUtils.join(
                    StringUtils.splitByCharacterTypeCamelCase(fieldName),
                    ' '
            ));
            TableColumn tableColumn = new TableColumn(columnName);
            table.getColumns().add(tableColumn);
        });

        TableViewConfigurer.configureTableView(itemClass, table);

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableItems = FXCollections.observableArrayList();
        table.setItems(tableItems);
    }


    @FXML
    protected void addItem() {
//        tableItems.add(new UIPropertyItem("", ""));
    }

    @FXML
    protected void removeItem() {
        List<Integer> indicesToRemove = table.getSelectionModel().getSelectedIndices().reversed();
        indicesToRemove.forEach((i) -> {
            tableItems.remove((int) i);
        });
    }

    @FXML
    protected void refresh() throws Exception {

    }

    public void configureEditableControls(boolean editable) {
        table.setEditable(editable);
        if (editable) {
//            TableViewConfigurer.configureEditableKeyValueTable(table);
        }
    }

}
