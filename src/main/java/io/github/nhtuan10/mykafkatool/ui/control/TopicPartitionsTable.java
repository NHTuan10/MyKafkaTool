package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.partition.KafkaPartitionsTableItem;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
import javafx.beans.property.BooleanProperty;
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
import java.util.function.Predicate;

@Slf4j
public class TopicPartitionsTable extends EditableTableControl<KafkaPartitionsTableItem> {
    private KafkaTopic kafkaTopic;
    private final ClusterManager clusterManager;
    private BooleanProperty isBlockingUINeeded;
    private StringProperty totalMessages;

    public TopicPartitionsTable() {
        super(false);
        clusterManager = ClusterManager.getInstance();
    }

    @Override
    protected Predicate<KafkaPartitionsTableItem> filterPredicate(Filter filter) {
        return Filter.buildFilterPredicate(filter, KafkaPartitionsTableItem::getLeader);
    }

    public void loadTopicPartitions(KafkaTopic kafkaTopic, StringProperty totalMessages, BooleanProperty isBusy) {
        this.kafkaTopic = kafkaTopic;
        this.isBlockingUINeeded = isBusy;
        this.totalMessages = totalMessages;
        refresh(false);
    }

    @Override
    @FXML
    public void refresh() {
        refresh(true);
    }

    public void refresh(boolean isFocused) {
        String clusterName = kafkaTopic.cluster().getName();
        String topicName = kafkaTopic.name();
        isBlockingUINeeded.set(isFocused);
        ObservableList<KafkaPartitionsTableItem> partitionsTableItems = FXCollections.observableArrayList();
        Callable<Long> task = () -> {
            List<TopicPartitionInfo> topicPartitionInfos;
            try {
                topicPartitionInfos = clusterManager.getTopicPartitions(clusterName, topicName);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                log.error("Error when get partition info for cluster {} and topic {}", clusterName, topicName, e);
                throw new RuntimeException(e);
            }
            topicPartitionInfos.forEach(partitionInfo -> {
                try {
                    Pair<Long, Long> partitionOffsetsInfo = clusterManager.getPartitionOffsetInfo(clusterName, new TopicPartition(topicName, partitionInfo.partition()), null);
                    KafkaPartitionsTableItem partitionsTableItem = ViewUtil.mapToUIPartitionTableItem(partitionInfo, partitionOffsetsInfo);
                    partitionsTableItems.add(partitionsTableItem);
                } catch (ExecutionException | InterruptedException e) {
                    log.error("Error when get partitions  offset info for Partitions table of cluster {} and topic {}", clusterName, topicName, e);
                    throw new RuntimeException(e);
                }
            });
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
        ViewUtil.runBackgroundTask(task, onSuccess, onFailure);
    }

}
