package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.cg.ConsumerGroupOffsetTableItem;
import io.github.nhtuan10.mykafkatool.ui.cg.ConsumerGroupTreeItem;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

//TODO: create a new CG view which includes this table. Then add info such as number of topic, members, partition, members, total lag, topics, etc.

@Slf4j
public class ConsumerGroupTable extends EditableTableControl<ConsumerGroupOffsetTableItem> {
    private ConsumerGroupTreeItem consumerGroupTreeItem;
    private final ClusterManager clusterManager;
    private BooleanProperty isBusy;

    public ConsumerGroupTable() {
        super(false);
        clusterManager = ClusterManager.getInstance();
    }

    @Override
    protected Predicate<ConsumerGroupOffsetTableItem> filterPredicate(Filter filter) {
        return Filter.buildFilterPredicate(filter, ConsumerGroupOffsetTableItem::getTopic);
    }

    public void loadCG(ConsumerGroupTreeItem consumerGroupTreeItem, BooleanProperty isBusy) {
        this.consumerGroupTreeItem = consumerGroupTreeItem;
        this.isBusy = isBusy;
        refresh();
    }

    @Override
    @FXML
    public void refresh() {
        isBusy.set(true);
        ViewUtil.runBackgroundTask(() -> {
            try {
                return FXCollections.observableArrayList(clusterManager.listConsumerGroupOffsets(consumerGroupTreeItem.getClusterName(), consumerGroupTreeItem.getConsumerGroupId()));
            } catch (ExecutionException | InterruptedException e) {
                isBusy.set(false);
                log.error("Error when get consumer group offsets", e);
                throw new RuntimeException(e);
            }
        }, (items) -> {
            isBusy.set(false);
            setItems(items);
            ObservableList<TableColumn<ConsumerGroupOffsetTableItem, ?>> sortOrder = table.getSortOrder();
            final List<String> sortedColumnNames = List.of(ConsumerGroupOffsetTableItem.CLIENT_ID, ConsumerGroupOffsetTableItem.TOPIC, ConsumerGroupOffsetTableItem.PARTITION);
            if (sortOrder.isEmpty()) {
                List<TableColumn<ConsumerGroupOffsetTableItem, ?>> sortedColumns = table.getColumns().stream()
                        .filter(c -> sortedColumnNames.contains(c.getText()))
                        .peek(c -> c.setSortType(TableColumn.SortType.ASCENDING))
                        .toList();

                table.getSortOrder().addAll(sortedColumns);
                table.sort();
            }
        }, (e) -> {
            isBusy.set(false);
            setItems(FXCollections.emptyObservableList());
            throw ((RuntimeException) e);
        });
    }

}
