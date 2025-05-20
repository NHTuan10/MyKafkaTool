package com.example.mytool.ui.topic;

import com.example.mytool.manager.ClusterManager;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.ui.partition.KafkaPartitionTreeItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
@AllArgsConstructor
public class KafkaTopicTreeItem<T> extends TreeItem<T> {
    public KafkaTopicTreeItem(T value) {
        super(value);
    }

    public KafkaTopicTreeItem(T value, Node graphic) {
        super(value, graphic);
    }

    private boolean isFirstTimeChildren = true;

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public ObservableList<TreeItem<T>> getChildren() {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false;

            // First getChildren() call, so we actually go off and
            // determine the children of the File contained in this TreeItem.
            super.getChildren().setAll(buildChildren(this));
        }
        return super.getChildren();
    }

    private ObservableList<TreeItem<T>> buildChildren(TreeItem<T> treeItem) {

        if (treeItem.getValue() instanceof KafkaTopic topic) {
            ObservableList<TreeItem<T>> children = FXCollections.observableArrayList();
            try {
                List<TopicPartitionInfo> partitionInfoList = ClusterManager.getInstance().getTopicPartitions(topic.cluster().getName(), topic.name());

                partitionInfoList.forEach(partitionInfo -> {
//                    KafkaPartition partition = new KafkaPartition(partitionInfo.partition(), topic, partitionInfo);
                    KafkaPartition partition = new KafkaPartition(partitionInfo.partition(), topic);
                    children.add(new KafkaPartitionTreeItem<>((T) partition));
                });

                return children;
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                log.error("Error loading partitions", e);
            }
        }
        return FXCollections.emptyObservableList();
    }

}
