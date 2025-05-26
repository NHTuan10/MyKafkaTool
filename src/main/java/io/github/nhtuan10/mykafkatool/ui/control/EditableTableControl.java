package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.TableViewConfigurer;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
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
import java.util.List;
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
    protected Label noRowsLabel;

    protected StageHolder stageHolder;

    public void setStage(Stage stage) {
        stageHolder.setStage(stage);
    }

    public EditableTableControl() {
        this(true);
    }

    public EditableTableControl(@NamedArg(value = "editable", defaultValue = "false") Boolean editable) {
        this.editable = new SimpleBooleanProperty(true);
        this.stageHolder = new StageHolder();
        this.filterTextProperty = new SimpleStringProperty("");
        this.editable.set(editable);
        this.itemClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
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
        TableViewConfigurer.configureTableView(itemClass, table, stageHolder);

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableItems = FXCollections.observableArrayList();
//        table.setItems(tableItems.filtered(filterPredicate(new Filter(this.filterTextProperty.get(), this.regexFilterToggleBtn.isSelected()))));
        applyFilter(new Filter(this.filterTextProperty.get(), this.regexFilterToggleBtn.isSelected()));
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
        SortedList<T> sortedList = new SortedList<>(tableItems.filtered(filterPredicate(filter)));
        sortedList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedList);
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

    protected void configureEditableControls() {
        table.editableProperty().bind(editable);
        addItemBtn.visibleProperty().bind(editable);
        removeItemsBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        removeItemsBtn.visibleProperty().bind(editable);
    }


    public void setItems(ObservableList<T> items) {
        //TODO: make this setItems consistent
        tableItems.setAll(items);
    }

    public ObservableList<T> getItems() {
        return tableItems;
    }
//    public record Filter<T> (T item, String filterText){}

}
