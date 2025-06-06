package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.partition.KafkaPartitionsTableItem;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Slf4j
public class TopicPartitionsTable extends EditableTableControl<KafkaPartitionsTableItem> {
    private KafkaTopic kafkaTopic;
    private KafkaPartition kafkaPartition;
    private final ClusterManager clusterManager;
    private BooleanProperty isBlockingUINeeded;
    private ReadOnlyBooleanProperty isShownOnWindow;
    private StringProperty totalMessages;

    public TopicPartitionsTable() {
        super(false);
        clusterManager = ClusterManager.getInstance();
    }

    public void setProperties(BooleanProperty isBlockingAppUINeeded, ReadOnlyBooleanProperty isShownOnWindow) {
        this.isBlockingUINeeded = isBlockingAppUINeeded;
        this.isShownOnWindow = isShownOnWindow;
    }

    @FXML
    protected void initialize() {
        super.initialize();
        numberOfRowsLabel.textProperty().bind(noRowsIntProp.asString().concat(" Partitions"));
    }

//    @Override
//    protected Predicate<KafkaPartitionsTableItem> filterPredicate(Filter filter) {
//        return Filter.buildFilterPredicate(filter, KafkaPartitionsTableItem::getLeader);
//    }

    public void loadTopicPartitions(KafkaTopic kafkaTopic, KafkaPartition kafkaPartition, StringProperty totalMessages) {
        this.kafkaTopic = kafkaTopic;
        this.kafkaPartition = kafkaPartition;
        this.totalMessages = totalMessages;
        refresh();
    }

    @Override
    @FXML
    public void refresh() {
        String clusterName = kafkaTopic.cluster().getName();
        String topicName = kafkaTopic.name();
        isBlockingUINeeded.set(isShownOnWindow.get());
        ObservableList<KafkaPartitionsTableItem> partitionsTableItems = FXCollections.observableArrayList();
        Callable<Long> task = () -> {
            List<TopicPartitionInfo> topicPartitionInfos;
            try {
                if (kafkaPartition != null) {
                    topicPartitionInfos = List.of(clusterManager.getTopicPartitionInfo(clusterName, topicName, kafkaPartition.id()));
                } else {
                    topicPartitionInfos = clusterManager.getTopicPartitions(clusterName, topicName);
                }
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                log.error("Error when get partition info for cluster {}, topic {} and partition {}", clusterName, topicName, kafkaPartition, e);
                throw new RuntimeException(e);
            }
            partitionsTableItems.addAll(getPartitionInfoForUI(topicPartitionInfos));
            return partitionsTableItems.stream().mapToLong(KafkaPartitionsTableItem::getNumOfMessages).sum();
        };
        Consumer<Long> onSuccess = (val) -> {
            setItems(partitionsTableItems);
            totalMessages.set(val + " Messages");
            isBlockingUINeeded.set(false);
            log.info("Successfully get partitions properties for cluster {} and topic {}", clusterName, topicName);
        };
        Consumer<Throwable> onFailure = (exception) -> {
            isBlockingUINeeded.set(false);
            throw new RuntimeException(exception);
        };
        ViewUtils.runBackgroundTask(task, onSuccess, onFailure);
    }

    private List<KafkaPartitionsTableItem> getPartitionInfoForUI(List<TopicPartitionInfo> topicPartitionInfos) {
        String clusterName = kafkaTopic.cluster().getName();
        String topicName = kafkaTopic.name();
        return topicPartitionInfos.stream().map((topicPartitionInfo) -> {
            try {
                Pair<Long, Long> partitionOffsetsInfo = clusterManager.getPartitionOffsetInfo(clusterName, new TopicPartition(topicName, topicPartitionInfo.partition()), null, null);
                return ViewUtils.mapToUIPartitionTableItem(topicPartitionInfo, partitionOffsetsInfo);
            } catch (ExecutionException | InterruptedException e) {
                log.error("Error when get partitions  offset info for Partitions table of cluster {} and topic {}", clusterName, topicName, e);
                throw new RuntimeException(e);
            }
        }).toList();
    }
}
