package io.github.nhtuan10.mykafkatool.ui.messageview;

import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.constant.UIStyleConstant;
import io.github.nhtuan10.mykafkatool.consumer.KafkaConsumerService;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.control.EditableTableControl;
import io.github.nhtuan10.mykafkatool.ui.topic.UIPropertyTableItem;
import io.github.nhtuan10.mykafkatool.ui.util.TableViewConfigurer;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
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
public class KafkaMessageTable extends EditableTableControl<KafkaMessageTableItem> {
    SerDesHelper serDesHelper;
    KafkaConsumerService.MessagePollingPosition messagePollingPosition;

    public KafkaMessageTable() {
        super(false);
        this.refreshBtn.setVisible(false);
    }

    @FXML
    protected void initialize() {
        super.initialize();
        messagePollingPosition = KafkaConsumerService.MessagePollingPosition.LAST;
        numberOfRowsLabel.textProperty().bind(new SimpleStringProperty("Showing: ").concat(noRowsIntProp.asString().concat(" Messages")));
//        setDefaultColumnWidths();
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
//    private void setDefaultColumnWidths() {
//        table.getColumns().forEach(column -> {
//           switch (column.getId()){
//               case KafkaMessageTableItem.KEY -> column.setPrefWidth(100d);
//               case KafkaMessageTableItem.VALUE -> column.setPrefWidth(440d);
//               case KafkaMessageTableItem.TIMESTAMP -> column.setPrefWidth(300d);
//           }
//        });
//    }

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
                        if (item.isErrorItem()) {
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
                        ViewUtils.showPopUpModal(AppConstant.ADD_MESSAGE_MODAL_FXML, "View Message", new AtomicReference<>(), msgModalFieldMap, false, true, stageHolder.getStage());
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
            table.getColumns().stream().filter(c -> c.getId().equals(KafkaMessageTableItem.TIMESTAMP)).findFirst().ifPresent((timestampColumn) -> {
                timestampColumn.setSortType(pollingPosition == KafkaConsumerService.MessagePollingPosition.FIRST
                        ? TableColumn.SortType.ASCENDING
                        : TableColumn.SortType.DESCENDING);
                table.getSortOrder().add(timestampColumn);
                table.sort();
            });
        }
    }

    public Filter getFilter() {
        return new Filter(this.filterTextField.getText(), this.regexFilterToggleBtn.isSelected());
    }

    public void handleNumOfMsgChanged(int numOfMsgLongProp) {
        this.noRowsIntProp.set(numOfMsgLongProp);
    }

    public void resizeColumn() {
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
