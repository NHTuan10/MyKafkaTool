package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.ui.util.ModalUtils;
import io.github.nhtuan10.mykafkatool.ui.util.TableViewConfigurer;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

//TODO: create a new CG view which includes this table. Then add info such as number of topic, members, partition, members, total lag, topics, etc.

@Slf4j
public class ConsumerGroupTable extends AbstractConsumerGroupTable<ConsumerGroupTableItem> {

//    public ConsumerGroupTable() {
//        super();
//    }

    @Override
    protected void configureTableView() {
        TableViewConfigurer.TableViewConfiguration<ConsumerGroupTableItem> configuration = new TableViewConfigurer.TableViewConfiguration<>(
                SelectionMode.SINGLE
                , false
                , true
                , true
                , new TableViewConfigurer.TableViewConfiguration.ExtraFieldsToCopyAndExport<>(List.of()
                , item -> List.of()), List.of());
        configureTableView(configuration);
    }

    @Override
    @FXML
    public void refresh() {
        loadConsumerGroupDetails((clusterName1, consumerGroupId) -> {
            try {
                return clusterManager.describeConsumerGroupDetails(clusterName1, consumerGroupId);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, List.of(ConsumerGroupTableItemFXModel.TOPIC));
    }

    public void resetCG(CGResetOption resetOption, Long startTimestamp) {
        var selectedItems = table.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            ModalUtils.showAlertDialog(Alert.AlertType.WARNING, "Please choose a consumer group and topic to reset offsets", "Please choose a consumer group and topic to reset offsets", ButtonType.OK);
            return;
        }
        if (resetOption == CGResetOption.START_TIMESTAMP && startTimestamp == null) {
            ModalUtils.showAlertDialog(Alert.AlertType.WARNING, "Please select start time to reset", "Please select start time to reset", ButtonType.OK);
        }
        selectedItems.forEach(item -> {
            if (ModalUtils.confirmAlert("Reset Consumer Group Offset", "Do you want to reset consumer group %s for topic %s to the %s".formatted(item.getGroupID(), item.getTopic(), resetOption), "YES", "NO")) {
                try {
                    var topicPartitionList = clusterManager.getTopicPartitions(clusterName, item.getTopic()).stream().map(tpi -> new TopicPartition(item.getTopic(), tpi.partition())).toList();
                    resetCG(resetOption, item.getGroupID(), topicPartitionList, startTimestamp);
                    ModalUtils.showAlertDialog(Alert.AlertType.INFORMATION, "Reset offset successfully", "Reset offset successfully", ButtonType.OK);
                    refresh();
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
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
//                if (topic != null) {
//                    this.consumerGroupIds = clusterManager.getConsumerGroupList(clusterName).stream().map(ConsumerGroupListing::groupId).toList();
//                    return FXCollections.observableArrayList(clusterManager
//                            .listConsumerGroupDetails(clusterName, consumerGroupIds).stream()
//                            .filter(item -> item.getTopic().equals(topic.name())).toList());
//                } else {
//                    return FXCollections.observableArrayList(clusterManager.listConsumerGroupDetails(clusterName, consumerGroupIds));
//                }
////                return FXCollections.observableArrayList(clusterManager.listConsumerGroupDetails(consumerGroupTreeItem.getClusterName(), List.of(consumerGroupTreeItem.getConsumerGroupId())));
//            } catch (ExecutionException | InterruptedException e) {
//                log.error("Error when get consumer group offsets", e);
//                throw new RuntimeException(e);
//            }
//        }, (items) -> {
//            isBusy.set(false);
//            setItems(items);
//            ObservableList<TableColumn<ConsumerGroupTableItem, ?>> sortOrder = table.getSortOrder();
//            final List<String> sortedColumnNames = List.of(ConsumerGroupTableItemFXModel.TOPIC);
//            if (sortOrder.isEmpty()) {
//                List<TableColumn<ConsumerGroupTableItem, ?>> sortedColumns = table.getColumns().stream()
//                        .filter(c -> sortedColumnNames.contains(c.getId()))
//                        .peek(c -> c.setSortType(TableColumn.SortType.ASCENDING))
//                        .toList();
//
//                table.getSortOrder().addAll(sortedColumns);
//                table.sort();
//            }
//        }, (e) -> {
//            isBusy.set(false);
//            setItems(FXCollections.emptyObservableList());
//            throw ((RuntimeException) e);
//        });
//    }

}
