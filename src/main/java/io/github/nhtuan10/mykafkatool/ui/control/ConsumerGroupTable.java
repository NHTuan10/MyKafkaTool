package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.cg.ConsumerGroupOffsetTableItem;
import io.github.nhtuan10.mykafkatool.ui.cg.ConsumerGroupTreeItem;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

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
                setItems(FXCollections.observableArrayList(clusterManager.listConsumerGroupOffsets(consumerGroupTreeItem.getClusterName(), consumerGroupTreeItem.getConsumerGroupId())));
            } catch (ExecutionException | InterruptedException e) {
                isBusy.set(false);
                log.error("Error when get consumer group offsets", e);
                throw new RuntimeException(e);
            }
            return null;
        }, (e) -> isBusy.set(false), (e) -> {
            isBusy.set(false);
            throw ((RuntimeException) e);
        });
    }

}
