package io.github.nhtuan10.mykafkatool.ui.topic;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.control.EditableTableControl;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
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
import java.util.function.Consumer;

@Slf4j
public class TopicAndPartitionPropertyTable extends EditableTableControl<UIPropertyTableItem> {
    private KafkaTopic kafkaTopic;
    private KafkaPartition kafkaPartition;
    private ConsumerType type;
    private final ClusterManager clusterManager;
    private BooleanProperty isBlockingUINeeded;
    private ReadOnlyBooleanProperty isShownOnWindow;

    public TopicAndPartitionPropertyTable() {
        super(false);
        clusterManager = MyKafkaToolApplication.DAGGER_APP_COMPONENT.clusterManager();
    }

    @FXML
    protected void initialize() {
        // TODO: For topic, need to make properties editable. a table to list all CG for this topic
        super.initialize();
        numberOfRowsLabel.textProperty().bind(noRowsIntProp.asString().concat(" Properties"));
//        isShownOnWindow = new SimpleBooleanProperty(false);
//        isShownOnWindow.bind(this.sceneProperty().map(Scene::windowProperty)
//                .map((windowReadOnlyObjectProperty) ->  windowReadOnlyObjectProperty.get().showingProperty().get())
//                .orElse(false));
    }

    public void setProperties(BooleanProperty isBlockingAppUINeeded, ReadOnlyBooleanProperty isShownOnWindow) {
        this.isBlockingUINeeded = isBlockingAppUINeeded;
        this.isShownOnWindow = isShownOnWindow;
    }

//    @Override
//    protected Predicate<UIPropertyTableItem> filterPredicate(Filter filter) {
//        return Filter.buildFilterPredicate(filter, UIPropertyTableItem::getName, UIPropertyTableItem::getValue);
//    }

    public void loadTopicConfig(KafkaTopic kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
        this.type = ConsumerType.TOPIC;
        refreshTopicConfig();
    }

    public void loadPartitionConfig(KafkaPartition kafkaPartition) {
        this.kafkaPartition = kafkaPartition;
        this.type = ConsumerType.PARTITION;
        refreshPartitionConfig();
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
        isBlockingUINeeded.set(isShownOnWindow.get());
//        isBlockingUINeeded.set(isFocused);
        final String clusterName = kafkaPartition.topic().cluster().getName();
        final String topic = kafkaPartition.topic().name();
        Callable<ObservableList<UIPropertyTableItem>> getPartitionInfo = () -> {
            try {
                Pair<Long, Long> partitionOffsetsInfo = clusterManager.getPartitionOffsetInfo(clusterName, new TopicPartition(topic, kafkaPartition.id()), null, null);
                ObservableList<UIPropertyTableItem> list = FXCollections.observableArrayList(
                        new UIPropertyTableItem(UIPropertyTableItem.START_OFFSET, partitionOffsetsInfo.getLeft().toString())
                        , new UIPropertyTableItem(UIPropertyTableItem.END_OFFSET, partitionOffsetsInfo.getRight().toString())
                        , new UIPropertyTableItem(UIPropertyTableItem.NO_MESSAGES, String.valueOf(partitionOffsetsInfo.getRight() - partitionOffsetsInfo.getLeft())));

                TopicPartitionInfo partitionInfo = clusterManager.getTopicPartitionInfo(clusterName, topic, kafkaPartition.id());
                list.addAll(getPartitionInfoForUI(partitionInfo));

                return list;
            } catch (Exception e) {
                log.error("Error when get partition info", e);
//                setItems(FXCollections.observableArrayList());
                throw new RuntimeException(e);
            }
        };
        Consumer<ObservableList<UIPropertyTableItem>> onSuccess = (list) -> {
            setItems(list);
            isBlockingUINeeded.set(false);
            log.info("Successfully get topic config & partitions properties for cluster {}, topic {} and partition {}", clusterName, topic, kafkaPartition.id());
        };
        Consumer<Throwable> onFailure = (exception) -> {
            setItems(FXCollections.emptyObservableList());
            table.setPlaceholder(new Label("Error when get partition info: " + exception.getCause().getMessage()));
            isBlockingUINeeded.set(false);
            log.error("Error when getting topic config & partitions properties for cluster {} and topic {} and partition {}", clusterName, topic, kafkaPartition.id(), exception);
        };
        ViewUtils.runBackgroundTask(getPartitionInfo, onSuccess, onFailure);
    }

    private void refreshTopicConfig() {
        isBlockingUINeeded.set(isShownOnWindow.get());
//        isBlockingUINeeded.set(isFocused);
        String clusterName = kafkaTopic.cluster().getName();
        String topicName = kafkaTopic.name();
        Callable<ObservableList<UIPropertyTableItem>> getTopicAndPartitionProperties = () -> {
            try {
                // topic config table
                ObservableList<UIPropertyTableItem> configs = FXCollections.observableArrayList();
                Collection<ConfigEntry> configEntries = clusterManager.getTopicConfig(clusterName, topicName);
                configEntries.forEach(entry -> configs.add(new UIPropertyTableItem(entry.name(), entry.value())));
                return configs;
            } catch (Exception e) {
                log.error("Error when get topic config properties", e);
//                setItems(FXCollections.observableArrayList());
                throw new RuntimeException(e);
            }
        };
        Consumer<ObservableList<UIPropertyTableItem>> onSuccess = (list) -> {
            setItems(list);
            isBlockingUINeeded.set(false);
            log.info("Successfully get topic config & partitions properties for cluster {} and topic {}", clusterName, topicName);
        };
        Consumer<Throwable> onFailure = (exception) -> {
            setItems(FXCollections.emptyObservableList());
            table.setPlaceholder(new Label("Error when get topic config properties: " + exception.getCause().getMessage()));
            isBlockingUINeeded.set(false);
            log.error("Error when getting topic config & partitions properties for cluster {} and topic {}", clusterName, topicName, exception);
        };
        ViewUtils.runBackgroundTask(getTopicAndPartitionProperties, onSuccess, onFailure);
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
        PARTITION, TOPIC
    }
}
