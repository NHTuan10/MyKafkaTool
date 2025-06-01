package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.consumer.KafkaConsumerService;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.KafkaMessageTableItem;
import io.github.nhtuan10.mykafkatool.ui.TableViewConfigurer;
import io.github.nhtuan10.mykafkatool.ui.UIPropertyTableItem;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
public class MessageTable extends EditableTableControl<KafkaMessageTableItem> {
    SerDesHelper serDesHelper;
    KafkaConsumerService.MessagePollingPosition messagePollingPosition;

    @FXML
    protected void initialize() {
        super.initialize();
        messagePollingPosition = KafkaConsumerService.MessagePollingPosition.LAST;
        numberOfRowsLabel.textProperty().bind(new SimpleStringProperty("Showing: ").concat(noRowsIntProp.asString().concat(" Messages")));
    }

    @Override
    public Predicate<KafkaMessageTableItem> filterPredicate(Filter filter) {
        return Filter.buildFilterPredicate(filter
                , KafkaMessageTableItem::getKey
                , KafkaMessageTableItem::getValue
                , KafkaMessageTableItem::getTimestamp
        );
    }

    public void configureMessageTable(SerDesHelper serDesHelper) {
        this.serDesHelper = serDesHelper;
//        TableViewConfigurer.configureTableView(KafkaMessageTableItem.class, table, stageHolder);
        table.setRowFactory(tv -> {
            TableRow<KafkaMessageTableItem> row = new TableRow<>() {
                @Override
                protected void updateItem(KafkaMessageTableItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty && item != null) {
                        String color;
                        if (isSelected()) {
                            color = item.isErrorItem() ? "#C06666" : "lightgray";
                        } else {
                            color = item.isErrorItem() ? "lightcoral" : "transparent";
                        }
                        setStyle("-fx-background-color: %s; -fx-border-color: transparent transparent lightgray transparent;".formatted(color));
                    } else {
                        setStyle("");
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    KafkaMessageTableItem rowData = row.getItem();
                    log.debug("Double click on: {}", rowData.getKey());
                    Map<String, Object> msgModalFieldMap = Map.of(
                            "valueContentType", rowData.getValueContentType(),
                            "serDesHelper", serDesHelper,
                            "keyTextArea", rowData.getKey(),
                            "valueTextArea", rowData.getValue(),
                            "valueContentTypeComboBox", FXCollections.observableArrayList(rowData.getValueContentType()),
                            "headerTable",
                            FXCollections.observableArrayList(
                                    Arrays.stream(rowData.getHeaders().toArray()).map(header -> new UIPropertyTableItem(header.key(), new String(header.value()))).toList()));
                    try {
                        ViewUtil.showPopUpModal(AppConstant.ADD_MESSAGE_MODAL_FXML, "View Message", new AtomicReference<>(), msgModalFieldMap, false, true, stageHolder.getStage());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return row;
        });
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
            TableColumn<KafkaMessageTableItem, ?> timestampColumn = table.getColumns().get(5);
            timestampColumn.setSortType(pollingPosition == KafkaConsumerService.MessagePollingPosition.FIRST
                    ? TableColumn.SortType.ASCENDING
                    : TableColumn.SortType.DESCENDING);
            table.getSortOrder().add(timestampColumn);
            table.sort();
        }
    }

    public Filter getFilter() {
        return new Filter(this.filterTextField.getText(), this.regexFilterToggleBtn.isSelected());
    }

    public void handleNumOfMsgChanged(int numOfMsgLongProp) {
        this.noRowsIntProp.set(numOfMsgLongProp);
        TableViewConfigurer.autoResizeColumns(table);
    }

    public void addFilterListener(Consumer<Filter> runnable) {
        this.filterTextProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                runnable.accept(new Filter(newValue, this.regexFilterToggleBtn.isSelected()));
            }
        });
        this.regexFilterToggleBtn.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                runnable.accept(new Filter(this.filterTextField.getText(), newValue));
            }
        });
    }
}
