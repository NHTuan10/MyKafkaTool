package com.example.mytool.ui.control;

import com.example.mytool.Application;
import com.example.mytool.ui.TableViewConfigurer;
import com.example.mytool.ui.util.ViewUtil;
import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Slf4j
public class EditableTableControl<T> extends AnchorPane {

    @FXML
    protected Button addItemBtn;

    @FXML
    protected Button removeItemsBtn;

    @FXML
    protected Button refreshBtn;

    @FXML
    protected TableView<T> table;

    protected ObservableList<T> tableItems;

    protected Class<T> itemClass;

    protected BooleanProperty editable = new SimpleBooleanProperty(true);
    @FXML
    protected TextField filterTextField;

    protected StringProperty filterTextProperty;

    @FXML
    protected ToggleButton regexFilterToggleBtn;

    @FXML
    protected Label filterLabel;

    @FXML
    protected Label noRowsLabel;

    public EditableTableControl() {
        this(true);
    }

    public EditableTableControl(@NamedArg(value = "editable", defaultValue = "false") Boolean editable) {
        filterTextProperty = new SimpleStringProperty("");
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
        table.setItems(tableItems.filtered(filterPredicate(new Filter(this.filterTextProperty.get(), this.regexFilterToggleBtn.isSelected()))));

        filterTextField.textProperty().bindBidirectional(filterTextProperty);
//        filterTextField.setOnKeyPressed(e -> {
//            if (e.getCode().equals(KeyCode.ENTER)) {
////                var filteredList = tableItems.filtered(filterPredicate(this.filterTextProperty.get()));
////                table.setItems(filteredList);
//                applyFilter(new Filter(filterTextProperty.get(), regexFilterToggleBtn.isSelected()));
//            }
//        });
        this.filterTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilter(new Filter(newValue, this.regexFilterToggleBtn.isSelected()));
        });
        regexFilterToggleBtn.selectedProperty().addListener((observable, oldValue, newValue) -> {
            applyFilter(new Filter(filterTextProperty.get(), regexFilterToggleBtn.isSelected()));
        });
        SimpleIntegerProperty noRowsIntProp = new SimpleIntegerProperty();
//        noRowsIntProp.bind(table.itemsProperty().map(List::size));
        noRowsLabel.textProperty().bind(noRowsIntProp.asString().concat(" Rows"));
        table.itemsProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> noRowsIntProp.set(newValue.size()));
        });
        table.getItems().addListener((ListChangeListener<T>) change -> {
            Platform.runLater(() -> noRowsIntProp.set(table.getItems().size()));
        });
        tableItems.addListener((ListChangeListener<? super T>) change -> {
            Platform.runLater(() -> noRowsIntProp.set(table.getItems().size()));
        });

//        table.itemsProperty().addListener((observable, oldValue, newValue) -> {
//            noRowsIntProp.set(newValue.size());
//        });


//        noRowsIntProp.bind(table.itemsProperty().map(List::size));

        configureEditableControls();
    }

    public void applyFilter(Filter filter) {
//        this.filterTextField.setText(filterText);
        this.filterTextProperty.set(filter.getFilterText());
        this.regexFilterToggleBtn.setSelected(filter.isRegexFilter());
//        table.setItems(tableItems.filtered(filterPredicate(this.filterTextField.getText())));
        table.setItems(tableItems.filtered(filterPredicate(filter)));
    }

    @FXML
    protected void addItem() {
    }

    @FXML
    protected void removeItems() {
        List<Integer> indicesToRemove = table.getSelectionModel().getSelectedIndices().reversed();
        indicesToRemove.forEach((i) -> {
            T item = tableItems.get(i);
            boolean success = doRemoveItem(i, item);
            if (success) {
                tableItems.remove((int) i);
            }
        });
    }

    protected boolean doRemoveItem(int index, T item) {
        return true;
    }

    @FXML
    protected void refresh() throws Exception {

    }

    protected Predicate<T> filterPredicate(Filter filter) {
        return (item) -> true;
    }

    @SafeVarargs
    protected final Predicate<T> buildFilterPredicate(@NonNull Filter filter, Function<T, String>... fieldGetters) {
        assert (filter.getFilterText() != null);
        if (regexFilterToggleBtn.isSelected()) {
            Pattern pattern = Pattern.compile(filter.getFilterText(), Pattern.CASE_INSENSITIVE);
            return item -> {
                boolean isMatched = false;
                for (Function<T, String> fieldGetter : fieldGetters) {
                    isMatched = isMatched || (fieldGetter.apply(item) != null && pattern.matcher(fieldGetter.apply(item)).find());
                }
                return isMatched;
            };
        } else {
            return item -> {
                boolean isMatched = false;
                for (Function<T, String> fieldGetter : fieldGetters) {
                    isMatched = isMatched || (fieldGetter.apply(item) != null && fieldGetter.apply(item).toLowerCase().contains(filter.getFilterText().toLowerCase()));
                }
                return isMatched;
            };

        }
    }

    protected void configureEditableControls() {
        table.editableProperty().bind(editable);
        addItemBtn.disableProperty().bind(editable.not());
        removeItemsBtn.disableProperty().bind(editable.not()
                .or(table.getSelectionModel().selectedItemProperty().isNull()));
    }


    public void setItems(ObservableList<T> items) {
        //TODO: make this setItems consistent
        tableItems.setAll(items);
    }

    public ObservableList<T> getItems() {
        return tableItems;
    }
//    public record Filter<T> (T item, String filterText){}

    @Data
    @AllArgsConstructor
    protected static class Filter {
        private String filterText;
        private boolean isRegexFilter;
    }
}
