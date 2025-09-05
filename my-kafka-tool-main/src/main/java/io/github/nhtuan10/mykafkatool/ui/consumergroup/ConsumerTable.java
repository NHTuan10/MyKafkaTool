package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.ui.util.ModalUtils;
import io.github.nhtuan10.mykafkatool.ui.util.TableViewConfigurer;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class ConsumerTable extends AbstractConsumerGroupTable<ConsumerTableItem> {
    //    private ConsumerGroupTreeItem consumerGroupTreeItem;
    //    @Inject
//    private String clusterName;
//    private List<String> consumerGroupIds;
//    private ObjectProperty<KafkaTopic> topic;
//    private final ClusterManager clusterManager;
//    private BooleanProperty isBusy;

//    public ConsumerTable() {
//        super(MyKafkaToolApplication.DAGGER_APP_COMPONENT.clusterManager());
//    }

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
    public void init() {
        Optional<TableColumn<ConsumerTableItem, ?>> groupIdColumnOpt = TableViewConfigurer.getTableColumnById(table, ConsumerTableItemFXModel.GROUP_ID);
        groupIdColumnOpt.ifPresent((tableColumn) -> {
            tableColumn.visibleProperty().bind(this.topic.isNotNull());
        });
        Optional<TableColumn<ConsumerTableItem, ?>> topicColumnOpt = TableViewConfigurer.getTableColumnById(table, ConsumerTableItemFXModel.TOPIC);
        topicColumnOpt.ifPresent((tableColumn) -> {
            tableColumn.visibleProperty().bind(this.topic.isNull());
        });
    }

    @Override
    @FXML
    public void refresh() {
        loadConsumerGroupDetails((clusterName1, consumerGroupId) -> {
            try {
                return clusterManager.describeConsumerDetails(clusterName1, consumerGroupId);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, List.of(ConsumerTableItemFXModel.CONSUMER_ID, ConsumerTableItemFXModel.TOPIC, ConsumerTableItemFXModel.PARTITION));
    }

    public void resetCG(CGResetOption resetOption, Long startTimestamp) {
        var selectedItems = table.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            ModalUtils.showAlertDialog(Alert.AlertType.WARNING, "Please choose a consumer to reset offsets", "Please choose a consumer to reset offsets", ButtonType.OK);
            return;
        }
        if (resetOption == CGResetOption.START_TIMESTAMP && startTimestamp == null) {
            ModalUtils.showAlertDialog(Alert.AlertType.WARNING, "Please select start time to reset", "Please select start time to reset", ButtonType.OK);
        }
        Map<String, List<TopicPartition>> topicPartitionMap = selectedItems.stream()
                .collect(Collectors.groupingBy(
                        ConsumerTableItem::getGroupID,
                        Collectors.mapping(item -> new TopicPartition(item.getTopic(), item.getPartition()), Collectors.toList())));
        topicPartitionMap.forEach((cg, topicPartitions) -> {
            String tpStr = topicPartitions.stream().map(TopicPartition::toString).collect(Collectors.joining(", "));
            if (ModalUtils.confirmAlert("Reset Consumer Group Offset", "Do you want to reset consumer group %s for topic partitions %s to the %s".formatted(cg, tpStr, resetOption), "YES", "NO")) {
                try {
                    resetCG(resetOption, cg, topicPartitionMap.get(cg), startTimestamp);
                    ModalUtils.showAlertDialog(Alert.AlertType.INFORMATION, "Reset offset successfully for consumer group %s".formatted(cg), "Reset offset successfully", ButtonType.OK);
                    refresh();
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

//    @Override
//    @FXML
//    public void refresh() {
//        isBusy.set(true);
//        ViewUtils.runBackgroundTask(() -> {
//            try {
//                if (topic.get() != null) {
//                    this.consumerGroupIds = clusterManager.getConsumerGroupList(clusterName).stream().map(ConsumerGroupListing::groupId).toList();
//                    return FXCollections.observableArrayList(clusterManager
//                            .describeConsumerDetails(clusterName, consumerGroupIds).stream()
//                            .filter(item -> item.getTopic().equals(topic.get().name())).toList());
//                } else {
//                    return FXCollections.observableArrayList(clusterManager.describeConsumerDetails(clusterName, consumerGroupIds));
//                }
//            } catch (ExecutionException | InterruptedException e) {
/// /                isBusy.set(false);
//                log.error("Error when get consumer group offsets", e);
//                throw new RuntimeException(e);
//            }
//        }, (items) -> {
//            isBusy.set(false);
//            setItems(items);
//            ObservableList<TableColumn<ConsumerTableItem, ?>> sortOrder = table.getSortOrder();
//            final List<String> sortedColumnNames = List.of(ConsumerTableItemFXModel.CONSUMER_ID, ConsumerTableItemFXModel.TOPIC, ConsumerTableItemFXModel.PARTITION);
//            if (sortOrder.isEmpty()) {
//                List<TableColumn<ConsumerTableItem, ?>> sortedColumns = table.getColumns().stream()
//                        .filter(c -> sortedColumnNames.contains(c.getId()))
//                        .toList();
//                sortedColumns.forEach(c -> c.setSortType(TableColumn.SortType.ASCENDING));
//                table.getSortOrder().addAll(sortedColumns);
//                table.sort();
//            }
//        }, e -> {
//            isBusy.set(false);
//            setItems(FXCollections.emptyObservableList());
//            throw ((RuntimeException) e);
//        });
//    }
}
