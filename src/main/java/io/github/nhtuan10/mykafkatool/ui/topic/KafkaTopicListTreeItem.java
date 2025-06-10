package io.github.nhtuan10.mykafkatool.ui.topic;

import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class KafkaTopicListTreeItem<T> extends TreeItem<T> {
    public KafkaTopicListTreeItem(T value) {
        super(value);
    }

    public KafkaTopicListTreeItem(T value, Node graphic) {
        super(value, graphic);
    }

    private boolean loadChildren = true;

    public void reloadChildren() {
        loadChildren = true;
        getChildren();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public ObservableList<TreeItem<T>> getChildren() {
        if (loadChildren) {
            loadChildren = false;

            // First getChildren() call, so we actually go off and
            // determine the children of the File contained in this TreeItem.
            super.getChildren().setAll(buildChildren(this));
        }
        return super.getChildren();
    }

    private ObservableList<TreeItem<T>> buildChildren(TreeItem<T> TreeItem) {

        T value = TreeItem.getValue();
        if (value instanceof KafkaTopicListTreeItemValue conn) {
            ObservableList<TreeItem<T>> children = FXCollections.observableArrayList();
            try {
                //TODO: use Dagger
                List<String> topics = ClusterManager.getInstance().getAllTopics(conn.getCluster().getName()).stream().sorted().toList();
                topics.forEach(topicName -> {
                    KafkaTopicTreeItem<T> topic = new KafkaTopicTreeItem<>((T) new KafkaTopic(topicName, conn.getCluster()));
                    children.add(topic);
                    topic.getChildren();
                });
                return children;
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                log.error("Error loading topics", e);
            }
        }
        return FXCollections.emptyObservableList();
    }

    @Data
    @AllArgsConstructor
    public static class KafkaTopicListTreeItemValue {
        private String display;
        private KafkaCluster cluster;

        @Override
        public String toString() {
            return display;
        }

        public KafkaTopicListTreeItemValue(KafkaCluster cluster) {
            this.cluster = cluster;
            this.display = AppConstant.TREE_ITEM_TOPIC_LIST_DISPLAY_NAME;
        }
    }
}
