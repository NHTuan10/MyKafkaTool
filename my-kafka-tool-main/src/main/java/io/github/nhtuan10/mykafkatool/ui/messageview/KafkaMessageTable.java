package io.github.nhtuan10.mykafkatool.ui.messageview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.constant.UIStyleConstant;
import io.github.nhtuan10.mykafkatool.consumer.KafkaConsumerService;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.control.EditableTableControl;
import io.github.nhtuan10.mykafkatool.ui.util.TableViewConfigurer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class KafkaMessageTable extends EditableTableControl<KafkaMessageTableItem> {
    private SerDesHelper serDesHelper;
    private KafkaConsumerService.MessagePollingPosition messagePollingPosition;
    private ObjectMapper objectMapper = MyKafkaToolApplication.DAGGER_APP_COMPONENT.sharedObjectMapper();
    @Setter
    private KafkaMessageViewController parentController;

    public KafkaMessageTable() {
        super(false);
        this.refreshBtn.setVisible(false);
    }

    @FXML
    protected void initialize() {
        super.initialize();
        messagePollingPosition = KafkaConsumerService.MessagePollingPosition.LAST;
        numberOfRowsLabel.textProperty().bind(new SimpleStringProperty("Showing: ").concat(noRowsIntProp.asString().concat(" Messages")));
        setDefaultColumnWidths();
    }

    @Override
    protected void configureTableView() {

        List<MenuItem> menuItems = createContextMenuItems();
        TableViewConfigurer.TableViewConfiguration<KafkaMessageTableItem> configuration = new TableViewConfigurer.TableViewConfiguration<>(
                SelectionMode.MULTIPLE
                , false
                , true
                , true
                , new TableViewConfigurer.TableViewConfiguration.ExtraFieldsToCopyAndExport<>(List.of("Headers")
                , item -> {
            String headers;
            try {
                headers = objectMapper.writeValueAsString(Arrays.stream(item.getHeaders().toArray()).collect(Collectors.toMap(Header::key, Header::value)));
            } catch (JsonProcessingException e) {
                log.error("Error when serializing headers", e);
                headers = "Error when serializing headers";
            }
            return List.of(headers);
        }
        ), menuItems);
        configureTableView(configuration);
        TableViewConfigurer.getTableColumnById(table, KafkaMessageTableItem.SERIALIZED_VALUE_SIZE).ifPresent(column ->
                column.setText("Serialized Value Size (Bytes)"));
    }

    private MenuItem createViewOrReproduceMenuItem(String text, boolean isReproduceMenuItem) {
        MenuItem menuItem = new MenuItem(text);
        menuItem.setOnAction(event -> {
            List<KafkaMessageTableItem> selectedItems = table.getSelectionModel().getSelectedItems();
            selectedItems.forEach(item -> {
                parentController.viewOrReproduceMessage(serDesHelper, item, isReproduceMenuItem);
            });
        });
        return menuItem;
    }

    private List<MenuItem> createContextMenuItems() {
        return List.of(createViewOrReproduceMenuItem("View Message", false),
                createViewOrReproduceMenuItem("Re-produce Message", true));
    }

    //    @Override
//    public Predicate<KafkaMessageTableItem> filterPredicate(Filter filter) {
//        return Filter.buildFilterPredicate(filter
//                , KafkaMessageTableItem::getKey
//                , KafkaMessageTableItem::getValue
//                , KafkaMessageTableItem::getTimestamp
//                , (item) -> String.valueOf(item.getPartition())
//                 ,(item) -> String.valueOf(item.getOffset())
//        );
//    }
    private void setDefaultColumnWidths() {
        table.getColumns().forEach(column -> {
            switch (column.getId()) {
                case KafkaMessageTableItem.KEY -> column.setPrefWidth(100d);
                case KafkaMessageTableItem.VALUE -> column.setPrefWidth(300d);
                case KafkaMessageTableItem.TIMESTAMP -> column.setPrefWidth(200d);
                case KafkaMessageTableItem.SERIALIZED_VALUE_SIZE -> column.setPrefWidth(150d);
            }
        });
    }

    public void configureMessageTable(SerDesHelper serDesHelper) {
        //TODO: for message table, allow copy more details such as key, offset, partition, timestamp, headers, serialized size, etc.
        this.serDesHelper = serDesHelper;
        table.setRowFactory(tv -> {
            TableRow<KafkaMessageTableItem> row = new TableRow<>() {
                @Override
                protected void updateItem(KafkaMessageTableItem item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll(UIStyleConstant.ERROR_ROW_CLASS);
                    if (!empty && item != null) {
                        if (item.getIsErrorItem()) {
                            getStyleClass().add(UIStyleConstant.ERROR_ROW_CLASS);
                        }
                        getStyleClass().add(UIStyleConstant.TABLE_ROW_BORDER_CLASS);
                    } else {
                        getStyleClass().removeAll(UIStyleConstant.ERROR_ROW_CLASS, UIStyleConstant.TABLE_ROW_BORDER_CLASS);
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    KafkaMessageTableItem rowData = row.getItem();
                    log.debug("Double click on: {}", rowData.getKey());
                    parentController.viewOrReproduceMessage(serDesHelper, rowData, false);
                }
            });
            return row;
        });
    }

    public static List<KafkaMessageHeaderTableItem> mapToMsgHeaderTableItem(Headers headers) {
        return Arrays.stream(headers.toArray()).map(header -> KafkaMessageHeaderTableItemFXModel.builder().key(header.key()).value(new String(header.value())).build()).toList();
    }

    @Override
    public void applyFilter(Filter filter, Predicate<KafkaMessageTableItem>... extraPredicates) {
        configureSortAndFilterForMessageTable(filter, this.messagePollingPosition, extraPredicates);
    }

    @SafeVarargs
    public final void configureSortAndFilterForMessageTable(Filter filter, KafkaConsumerService.MessagePollingPosition pollingPosition, Predicate<KafkaMessageTableItem>... extraPredicates) {
        this.messagePollingPosition = pollingPosition;
        super.applyFilter(filter, extraPredicates);
        ObservableList<TableColumn<KafkaMessageTableItem, ?>> sortOrder = table.getSortOrder();
        if (sortOrder == null || sortOrder.isEmpty()) {
            TableViewConfigurer.getTableColumnById(table, KafkaMessageTableItem.TIMESTAMP).ifPresent((timestampColumn) -> {
                timestampColumn.setSortType(pollingPosition == KafkaConsumerService.MessagePollingPosition.FIRST
                        ? TableColumn.SortType.ASCENDING
                        : TableColumn.SortType.DESCENDING);
                table.getSortOrder().add(timestampColumn);
                table.sort();
            });
        }
    }

    public Filter getFilter() {
        return this.filterProperty.get();
    }

    public void handleNumOfMsgChanged(int numOfMsgLongProp) {
        this.noRowsIntProp.set(numOfMsgLongProp);
    }

    public void resizeColumn() {
        TableViewConfigurer.autoResizeColumns(table);
    }

    public void addFilterListener(Consumer<Filter> runnable) {
//        this.filterTextProperty.addListener((observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                runnable.accept(new Filter(newValue, this.regexFilterToggleBtn.isSelected()));
//            }
//        });
//        this.regexFilterToggleBtn.selectedProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                runnable.accept(new Filter(this.filterTextField.getText(), newValue));
//            }
//        });
        Filter filter = this.filterProperty.get();
        List.of(filterTextField.textProperty(), regexFilterToggleBtn.selectedProperty(), caseSensitiveFilterToggleBtn.selectedProperty(), negativeFilterToggleBtn.selectedProperty())
                .forEach(property -> property.addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        runnable.accept(this.filterProperty.get().copy());
                    }
                }));
    }
}
