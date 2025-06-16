package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.control.EditableTableControl;
import io.github.nhtuan10.mykafkatool.ui.util.TableViewConfigurer;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.ConsumerGroupListing;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

//TODO: create a new CG view which includes this table. Then add info such as number of topic, members, partition, members, total lag, topics, etc.

@Slf4j
public class ConsumerTable extends EditableTableControl<ConsumerTableItem> {
    //    private ConsumerGroupTreeItem consumerGroupTreeItem;
    //    @Inject
    private String clusterName;
    private List<String> consumerGroupIds;
    private ObjectProperty<KafkaTopic> topic;
    private final ClusterManager clusterManager;
    private BooleanProperty isBusy;

    public ConsumerTable() {
        super(false);
        this.clusterManager = MyKafkaToolApplication.DAGGER_APP_COMPONENT.clusterManager();
    }

//    @Override
//    protected Predicate<ConsumerGroupOffsetTableItem> filterPredicate(Filter filter) {
//        return Filter.buildFilterPredicate(filter
//                , ConsumerGroupOffsetTableItem::getTopic
//                , ConsumerGroupOffsetTableItem::getClientID
//                , ConsumerGroupOffsetTableItem::getHost
//                , ConsumerGroupOffsetTableItem::getCommittedOffset
//                , ConsumerGroupOffsetTableItem::getLag
//                , ConsumerGroupOffsetTableItem::getLastCommit
//                , item -> String.valueOf(item.getStart())
//                , item -> String.valueOf(item.getEnd())
//                , item -> String.valueOf(item.getPartition())
//        );
//    }

    @FXML
    protected void initialize() {
        topic = new SimpleObjectProperty<>();
        super.initialize();
        Optional<TableColumn<ConsumerTableItem, ?>> tableColumnOpt = TableViewConfigurer.getTableColumnById(table, ConsumerTableItemFXModel.GROUP_ID);
        tableColumnOpt.ifPresent((tableColumn) -> {
            tableColumn.visibleProperty().bind(this.topic.isNotNull());
        });
    }

    public void loadCG(String clusterName, List<String> consumerGroupIds, BooleanProperty isBusy) {
        this.clusterName = clusterName;
        this.consumerGroupIds = consumerGroupIds;
        this.topic.set(null);
        this.isBusy = isBusy;
        refresh();
    }

    public void loadCG(KafkaTopic topic, BooleanProperty isBusy) {
        this.clusterName = topic.cluster().getName();
        this.topic.set(topic);
        this.isBusy = isBusy;
        refresh();
    }

    @Override
    @FXML
    public void refresh() {
        isBusy.set(true);
        ViewUtils.runBackgroundTask(() -> {
            try {
                if (topic.get() != null) {
                    this.consumerGroupIds = clusterManager.getConsumerGroupList(clusterName).stream().map(ConsumerGroupListing::groupId).toList();
                    return FXCollections.observableArrayList(clusterManager
                            .listConsumerDetails(clusterName, consumerGroupIds).stream()
                            .filter(item -> item.getTopic().equals(topic.get().name())).toList());
                } else {
                    return FXCollections.observableArrayList(clusterManager.listConsumerDetails(clusterName, consumerGroupIds));
                }
            } catch (ExecutionException | InterruptedException e) {
//                isBusy.set(false);
                log.error("Error when get consumer group offsets", e);
                throw new RuntimeException(e);
            }
        }, (items) -> {
            isBusy.set(false);
            setItems(items);
            ObservableList<TableColumn<ConsumerTableItem, ?>> sortOrder = table.getSortOrder();
            final List<String> sortedColumnNames = List.of(ConsumerTableItemFXModel.CONSUMER_ID, ConsumerTableItemFXModel.TOPIC, ConsumerTableItemFXModel.PARTITION);
            if (sortOrder.isEmpty()) {
                List<TableColumn<ConsumerTableItem, ?>> sortedColumns = table.getColumns().stream()
                        .filter(c -> sortedColumnNames.contains(c.getId()))
                        .toList();
                sortedColumns.forEach(c -> c.setSortType(TableColumn.SortType.ASCENDING));
                table.getSortOrder().addAll(sortedColumns);
                table.sort();
            }
        }, e -> {
            isBusy.set(false);
            setItems(FXCollections.emptyObservableList());
            throw ((RuntimeException) e);
        });
    }
}
