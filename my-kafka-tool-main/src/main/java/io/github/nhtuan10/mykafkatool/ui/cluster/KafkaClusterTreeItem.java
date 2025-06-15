package io.github.nhtuan10.mykafkatool.ui.cluster;

import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.exception.ClusterNameExistedException;
import io.github.nhtuan10.mykafkatool.manager.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.ui.consumergroup.ConsumerGroupListTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicListTreeItem;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@AllArgsConstructor
public class KafkaClusterTreeItem<T> extends TreeItem<T> {
    public KafkaClusterTreeItem(T value) {
        super(value);
        if (value instanceof KafkaCluster kafkaCluster) {
            kafkaCluster.nameProperty().addListener(nameListener);
            kafkaCluster.statusProperty().addListener(nameListener);
            this.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (oldValue != null) {
                    ((KafkaCluster) oldValue).nameProperty().removeListener(nameListener);
                }
                if (newValue != null) {
                    ((KafkaCluster) newValue).nameProperty().addListener(nameListener);
                }
            });
        }

    }

    public KafkaClusterTreeItem(T value, Node graphic) {
        super(value, graphic);
    }

    private ChangeListener<Object> nameListener = (obs, oldName, newName) -> {
        TreeModificationEvent<T> event = new TreeModificationEvent<>(TreeItem.valueChangedEvent(), this);
        Event.fireEvent(this, event);
    };

//    public KafkaTopicListTreeItem<?> getKafkaTopicListTreeItem() {
//        return (KafkaTopicListTreeItem<?>) this.getChildren().filtered(KafkaTopicListTreeItem.class::isInstance).stream().findFirst().orElseThrow();
//    }
//
//
//    public ConsumerGroupListTreeItem<?> getConsumerGroupListTreeItem() {
//        return (ConsumerGroupListTreeItem<?>) this.getChildren().filtered(ConsumerGroupListTreeItem.class::isInstance).stream().findFirst().orElseThrow();
//    }


    public boolean removeKafkaTopicListTreeItem() {
        return this.getChildren().removeIf(KafkaTopicListTreeItem.class::isInstance);
    }


    public boolean removeConsumerGroupListTreeItem() {
        return this.getChildren().removeIf(ConsumerGroupListTreeItem.class::isInstance);
    }

    public void addOrUpdateSchemaRegistryItem(SchemaRegistryManager schemaRegistryManager, KafkaCluster cluster) throws ClusterNameExistedException {
        removeSchemaRegistryItem();
        if (StringUtils.isNotBlank(cluster.getSchemaRegistryUrl())) {
            schemaRegistryManager.connectToSchemaRegistry(cluster);
            TreeItem<Object> schemaRegistry = new TreeItem<>(AppConstant.SCHEMA_REGISTRY_TREE_ITEM_DISPLAY_NAME);
            this.getChildren().add((TreeItem<T>) schemaRegistry);
        }

    }

    public boolean removeSchemaRegistryItem() {
        return this.getChildren().removeIf(item -> item.getValue().equals(AppConstant.SCHEMA_REGISTRY_TREE_ITEM_DISPLAY_NAME));
    }

}
