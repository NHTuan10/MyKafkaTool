package com.example.mytool.ui.control;

import com.example.mytool.Application;
import com.example.mytool.ui.TableViewConfigurer;
import com.example.mytool.ui.util.ViewUtil;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.function.Predicate;

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

    protected BooleanProperty editable = new SimpleBooleanProperty(true);
    @FXML
    protected TextField filterTextField;

    @FXML
    protected Label filterLabel;

    public EditableTableControl() {
        this(true);
    }

    public EditableTableControl(@NamedArg(value = "editable", defaultValue = "false") Boolean editable) {
        this.editable.set(editable);
        itemClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource(
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
            TableColumn<T, ?> tableColumn = new TableColumn<>(columnName);
            table.getColumns().add(tableColumn);
        });

        TableViewConfigurer.configureTableView(itemClass, table, false);

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableItems = FXCollections.observableArrayList();
        filterTextField.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                table.setItems(tableItems.filtered(filterPredicate(this.filterTextField.getText())));
            }
        });
        table.setItems(tableItems);
        configureEditableControls();
    }


    @FXML
    protected void addItem() {
    }

    @FXML
    protected void removeItem() {
        List<Integer> indicesToRemove = table.getSelectionModel().getSelectedIndices().reversed();
        indicesToRemove.forEach((i) -> tableItems.remove((int) i));
    }

    @FXML
    protected void refresh() throws Exception {

    }

    protected Predicate<T> filterPredicate(String filterText) {
        return (item) -> true;
    }

    protected void configureEditableControls() {
        table.editableProperty().bind(editable);
        addItemBtn.disableProperty().bind(editable.not());
        removeItemBtn.disableProperty().bind(editable.not()
                .or(table.getSelectionModel().selectedItemProperty().isNull()));
    }

//    public record Filter<T> (T item, String filterText){}
}
