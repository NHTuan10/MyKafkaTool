package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.util.TableViewConfigurer;
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
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
public class EditableTableControl<T> extends AnchorPane {

    protected ObservableList<T> tableItems;

    protected Class<T> itemClass;

    protected BooleanProperty editable;

    //    protected StringProperty filterTextProperty;
    protected ObjectProperty<Filter> filterProperty;

    protected SimpleIntegerProperty noRowsIntProp = new SimpleIntegerProperty();

    protected StageHolder stageHolder;

    private final List<Function<T, String>> tableItemFieldGetters;

    @FXML
    protected Button addItemBtn;

    @FXML
    protected Button removeItemsBtn;

    @FXML
    protected Button refreshBtn;

    @FXML
    protected TableView<T> table;

    @FXML
    protected TextField filterTextField;

    @FXML
    protected ToggleButton regexFilterToggleBtn;

    @FXML
    protected ToggleButton caseSensitiveFilterToggleBtn;

    @FXML
    protected ToggleButton negativeFilterToggleBtn;

    @FXML
    protected Label filterLabel;

    @FXML
    protected Label numberOfRowsLabel;

    public void setStage(Stage stage) {
        stageHolder.setStage(stage);
    }

    public EditableTableControl() {
        this(true);
    }

    public EditableTableControl(@NamedArg(value = "editable", defaultValue = "true") Boolean editable) {
        this.editable = new SimpleBooleanProperty(editable);
        this.stageHolder = new StageHolder();
        this.filterProperty = new SimpleObjectProperty<>(new Filter());
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
        configureTableView();
        tableItems = FXCollections.observableArrayList();
        applyFilter(new Filter());
//        List.of(filterProperty.get().filterTextProperty(), filterProperty.get().isRegexFilterProperty(),filterProperty.get().isCaseSensitiveProperty(), filterProperty.get().isNegativeProperty() )
        List.of(filterTextField.textProperty(), regexFilterToggleBtn.selectedProperty(), caseSensitiveFilterToggleBtn.selectedProperty(), negativeFilterToggleBtn.selectedProperty())
                .forEach(property -> property.addListener((observable, oldValue, newValue) -> filterItems()));

//        filterProperty.get().filterTextProperty().addListener((observable, oldValue, newValue) -> {
//            filterItems();
//        });
//        filterProperty.get().isCaseSensitiveProperty().addListener((observable, oldValue, newValue) -> {
//            filterItems();
//        });

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

    protected void configureTableView() {
        configureTableView((TableViewConfigurer.TableViewConfiguration<T>) TableViewConfigurer.TableViewConfiguration.DEFAULT);
    }

    protected void configureTableView(TableViewConfigurer.TableViewConfiguration<T> tableViewConfiguration) {
        List<String> itemClassFields = TableViewConfigurer.getTableColumnNamesFromTableItem(itemClass);
        itemClassFields.forEach(fieldName -> {
            String columnName = StringUtils.capitalize(StringUtils.join(
                    StringUtils.splitByCharacterTypeCamelCase(fieldName),
                    ' '
            ));
            TableColumn<T, ?> tableColumn = new TableColumn<>(columnName);
            table.getColumns().add(tableColumn);
        });
        TableViewConfigurer.configureTableView(itemClass, table, stageHolder, tableViewConfiguration);
    }

    public void applyFilter(Filter filter, Predicate<T>... extraPredicates) {
//        filterTextField.textProperty().unbindBidirectional(filterProperty.get().filterTextProperty());
//        regexFilterToggleBtn.selectedProperty().unbindBidirectional(filterProperty.get().isRegexFilterProperty());
//        negativeFilterToggleBtn.selectedProperty().unbindBidirectional(filterProperty.get().isNegativeProperty());
//        caseSensitiveFilterToggleBtn.selectedProperty().unbindBidirectional(filterProperty.get().isCaseSensitiveProperty());

        Filter filterProp = this.filterProperty.get();
        filterProp.setFilterText(filter.getFilterText());
        filterProp.setIsRegexFilter(filter.getIsRegexFilter());
        filterProp.setIsNegative(filter.getIsNegative());
        filterProp.setIsCaseSensitive(filter.getIsCaseSensitive());

        filterTextField.textProperty().bindBidirectional(filterProp.filterTextProperty());
        regexFilterToggleBtn.selectedProperty().bindBidirectional(filterProp.isRegexFilterProperty());
        negativeFilterToggleBtn.selectedProperty().bindBidirectional(filterProp.isNegativeProperty());
        caseSensitiveFilterToggleBtn.selectedProperty().bindBidirectional(filterProp.isCaseSensitiveProperty());

        filterItems(filter, extraPredicates);
    }

    protected void filterItems() {
        filterItems(this.filterProperty.get());
    }

    protected void filterItems(Filter filter, Predicate<T>... extraPredicates) {
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
        List<Integer> indicesToRemove = table.getSelectionModel().getSelectedIndices().stream().sorted(Comparator.reverseOrder()).toList();
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
            filterItems();
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
