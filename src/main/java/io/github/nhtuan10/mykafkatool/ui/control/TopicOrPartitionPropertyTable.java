package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.UIPropertyTableItem;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
public class TopicOrPartitionPropertyTable extends EditableTableControl<UIPropertyTableItem> {
    private KafkaTopic kafkaTopic;
    private KafkaPartition kafkaPartition;
    private ConsumerType type;
    private final ClusterManager clusterManager;

    public TopicOrPartitionPropertyTable() {
        super(false);
        clusterManager = ClusterManager.getInstance();
    }

//    @FXML
//    protected void initialize() {
//        super.initialize();
//    }

    @Override
    protected Predicate<UIPropertyTableItem> filterPredicate(Filter filter) {
        return Filter.buildFilterPredicate(filter, UIPropertyTableItem::getName, UIPropertyTableItem::getValue);
    }

    public void loadTopicConfig(KafkaTopic kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
        this.type = ConsumerType.TOPIC;
        refresh();
    }

    public void loadPartitionConfig(KafkaPartition kafkaPartition) {
        this.kafkaPartition = kafkaPartition;
        this.type = ConsumerType.PARTITION;
        refresh();
    }


    @Override
    @FXML
    public void refresh() {
        if (type == ConsumerType.TOPIC) {
            refreshTopicConfig();
        } else if (type == ConsumerType.PARTITION) {
            refreshPartitionConfig();
        }
    }

    private void refreshPartitionConfig() {
        final String clusterName = kafkaPartition.topic().cluster().getName();
        final String topic = kafkaPartition.topic().name();
        Callable<Void> getPartitionInfo = () -> {
            try {
                Pair<Long, Long> partitionOffsetsInfo = clusterManager.getPartitionOffsetInfo(clusterName, new TopicPartition(topic, kafkaPartition.id()), null);
                ObservableList<UIPropertyTableItem> list = FXCollections.observableArrayList(
                        new UIPropertyTableItem(UIPropertyTableItem.START_OFFSET, partitionOffsetsInfo.getLeft().toString())
                        , new UIPropertyTableItem(UIPropertyTableItem.END_OFFSET, partitionOffsetsInfo.getRight().toString())
                        , new UIPropertyTableItem(UIPropertyTableItem.NO_MESSAGES, String.valueOf(partitionOffsetsInfo.getRight() - partitionOffsetsInfo.getLeft())));

                TopicPartitionInfo partitionInfo = clusterManager.getTopicPartitionInfo(clusterName, topic, kafkaPartition.id());
                list.addAll(getPartitionInfoForUI(partitionInfo));

                setItems(list);
                return null;
            } catch (ExecutionException | InterruptedException e) {
                log.error("Error when get partition info", e);
                throw new RuntimeException(e);
            }
        };
        Consumer<Void> onSuccess = (val) -> {
            log.info("Successfully get topic config & partitions properties for cluster {}, topic {} and partition {}", clusterName, topic, kafkaPartition.id());
        };
        Consumer<Throwable> onFailure = (exception) -> {
            log.error("Error when getting topic config & partitions properties for cluster {} and topic {} and partition {}", clusterName, topic, kafkaPartition.id(), exception);
        };
        ViewUtil.runBackgroundTask(getPartitionInfo, onSuccess, onFailure);
    }

    private void refreshTopicConfig() {
        String clusterName = kafkaTopic.cluster().getName();
        String topicName = kafkaTopic.name();
        Callable<Void> getTopicAndPartitionProperties = () -> {
            try {
                // topic config table
                ObservableList<UIPropertyTableItem> config = FXCollections.observableArrayList();
                Collection<ConfigEntry> configEntries = clusterManager.getTopicConfig(clusterName, topicName);
                configEntries.forEach(entry -> config.add(new UIPropertyTableItem(entry.name(), entry.value())));
                setItems(config);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                log.error("Error when get topic config properties", e);
//                            topicConfigTable.setItems(FXCollections.emptyObservableList());
//                            throw new RuntimeException(e);
            }
            return null;
        };
        Consumer<Void> onSuccess = (val) -> {
            log.info("Successfully get topic config & partitions properties for cluster {} and topic {}", clusterName, topicName);
        };
        Consumer<Throwable> onFailure = (exception) -> {
            log.error("Error when getting topic config & partitions properties for cluster {} and topic {}", clusterName, topicName, exception);
        };
        ViewUtil.runBackgroundTask(getTopicAndPartitionProperties, onSuccess, onFailure);
    }

    public static List<UIPropertyTableItem> getPartitionInfoForUI(TopicPartitionInfo partitionInfo) {
        List<UIPropertyTableItem> list = new ArrayList<>();
        Node leader = partitionInfo.leader();
        list.add(new UIPropertyTableItem(leader.host() + ":" + leader.port(), UIPropertyTableItem.LEADER));
        list.addAll(partitionInfo.replicas().stream().filter(r -> r != leader).map(replica -> {
            if (partitionInfo.isr().contains(replica)) {
                return new UIPropertyTableItem(replica.host() + ":" + replica.port(), UIPropertyTableItem.REPLICA_IN_SYNC);
            } else {
                return new UIPropertyTableItem(replica.host() + ":" + replica.port(), UIPropertyTableItem.REPLICA_NOT_IN_SYNC);
            }
        }).toList());
        return list;
    }

    public enum ConsumerType {
        PARTITION, TOPIC;
    }
}
