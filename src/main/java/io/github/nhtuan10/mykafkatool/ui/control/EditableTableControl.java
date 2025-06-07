package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.TableViewConfigurer;
import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

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

    protected BooleanProperty editable;
    @FXML
    protected TextField filterTextField;

    protected StringProperty filterTextProperty;

    @FXML
    protected ToggleButton regexFilterToggleBtn;

    @FXML
    protected Label filterLabel;

    @FXML
    protected Label numberOfRowsLabel;

    protected SimpleIntegerProperty noRowsIntProp = new SimpleIntegerProperty();

    protected StageHolder stageHolder;

    private final List<Function<T, String>> tableItemFieldGetters;

    public void setStage(Stage stage) {
        stageHolder.setStage(stage);
    }

    public EditableTableControl() {
        this(true);
    }

    public EditableTableControl(@NamedArg(value = "editable", defaultValue = "true") Boolean editable) {
        this.editable = new SimpleBooleanProperty(editable);
        this.stageHolder = new StageHolder();
        this.filterTextProperty = new SimpleStringProperty("");
        this.itemClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        tableItemFieldGetters = getTableItemFieldGetters();
        FXMLLoader fxmlLoader = new FXMLLoader(MyKafkaToolApplication.class.getResource(
                "editable-table.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private List<Function<T, String>> getTableItemFieldGetters() {
        List<String> itemClassFields = TableViewConfigurer.getFilterableFieldsFromTableItem(itemClass);
        return itemClassFields.stream().map(fieldName -> {
            String getterName = "get" + StringUtils.capitalize(fieldName);
            return (Function<T, String>) t -> {
                try {
                    return String.valueOf(itemClass.getMethod(getterName).invoke(t));
                } catch (Exception e) {
                    log.error("Error when invoking getter method: {}", getterName, e);
                    return "";
                }
            };
        }).toList();
    }

    @FXML
    protected void initialize() {
        table.getColumns().clear();
        List<String> itemClassFields = TableViewConfigurer.getTableColumnNamesFromTableItem(itemClass);
        itemClassFields.forEach(fieldName -> {
            String columnName = StringUtils.capitalize(StringUtils.join(
                    StringUtils.splitByCharacterTypeCamelCase(fieldName),
                    ' '
            ));
            TableColumn<T, ?> tableColumn = new TableColumn<>(columnName);
            table.getColumns().add(tableColumn);
        });
        TableViewConfigurer.configureTableView(itemClass, table, stageHolder);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableItems = FXCollections.observableArrayList();
        applyFilter(new Filter(this.filterTextProperty.get(), this.regexFilterToggleBtn.isSelected()));
        filterTextField.textProperty().bindBidirectional(filterTextProperty);
        filterTextProperty.addListener((observable, oldValue, newValue) -> {
            applyFilter(new Filter(newValue, this.regexFilterToggleBtn.isSelected()));
        });
        regexFilterToggleBtn.selectedProperty().addListener((observable, oldValue, newValue) -> {
            applyFilter(new Filter(filterTextProperty.get(), regexFilterToggleBtn.isSelected()));
        });
        numberOfRowsLabel.textProperty().bind(noRowsIntProp.asString().concat(" Rows"));
        table.itemsProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> noRowsIntProp.set(newValue.size()));
        });
        table.getItems().addListener((ListChangeListener<T>) change -> {
            TableViewConfigurer.configureTableViewHeaderTooltip(table);

            Platform.runLater(() -> noRowsIntProp.set(table.getItems().size()));
        });
        tableItems.addListener((ListChangeListener<? super T>) change -> {
            Platform.runLater(() -> noRowsIntProp.set(table.getItems().size()));
        });

        configureEditableControls();
    }

    public void applyFilter(Filter filter, Predicate<T>... extraPredicates) {
        this.filterTextProperty.set(filter.getFilterText());
        this.regexFilterToggleBtn.setSelected(filter.isRegexFilter());
        Predicate<T> predicate = Arrays.stream(extraPredicates).reduce(filterPredicate(filter), Predicate::and);
        SortedList<T> sortedList = new SortedList<>(tableItems.filtered(predicate));
        sortedList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedList);
    }

    @FXML
    protected void addItem() {
    }

    public void addItem(T item) {
        tableItems.add(item);
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
        return Filter.buildFilterPredicate(filter, this.tableItemFieldGetters);
    }

    protected void configureEditableControls() {
        table.editableProperty().bind(editable);
        addItemBtn.visibleProperty().bind(editable);
        removeItemsBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        removeItemsBtn.visibleProperty().bind(editable);
    }


    public void setItems(ObservableList<T> items) {
        setItems(items, true);
    }

    public void setItems(ObservableList<T> items, boolean doesApplyFilter) {
        tableItems = items;
        if (doesApplyFilter) {
            applyFilter(new Filter(this.filterTextProperty.get(), this.regexFilterToggleBtn.isSelected()));
        }
    }

    public ObservableList<T> getItems() {
        return tableItems;
    }

    public ObservableList<T> getShownItems() {
        return table.getItems();
    }
//    public record Filter<T> (T item, String filterText){}

    public void setEditable(boolean editable) {
        this.editable.set(editable);
    }

    public void clear() {
        tableItems.clear();
    }
}
