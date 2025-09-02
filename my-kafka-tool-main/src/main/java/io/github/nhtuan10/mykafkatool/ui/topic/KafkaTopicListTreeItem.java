package io.github.nhtuan10.mykafkatool.ui.topic;

import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.control.FilterableTreeItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class KafkaTopicListTreeItem<T> extends FilterableTreeItem<T> {
    private final ClusterManager clusterManager;

    public KafkaTopicListTreeItem(T value, ClusterManager clusterManager) {
        super(value);
        this.clusterManager = clusterManager;
    }

    public KafkaTopicListTreeItem(T value, Node graphic, ClusterManager clusterManager) {
        super(value, graphic);
        this.clusterManager = clusterManager;
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
//            super.getChildren().setAll(buildChildren(this));
            super.getSourceChildren().setAll(buildChildren(this));
        }
        return super.getChildren();
    }

    private ObservableList<TreeItem<T>> buildChildren(TreeItem<T> TreeItem) {

        T value = TreeItem.getValue();
        if (value instanceof KafkaTopicListTreeItemValue conn) {
            ObservableList<TreeItem<T>> children = FXCollections.observableArrayList();
            try {
                List<KafkaTopic> topics = clusterManager.getAllTopics(conn.getCluster()).stream().sorted(Comparator.comparing(KafkaTopic::name)).toList();
                topics.forEach(kafkaTopic -> {
                    KafkaTopicTreeItem<T> topicTreeIten = new KafkaTopicTreeItem<>( (T) kafkaTopic, clusterManager);
                    children.add(topicTreeIten);
                    topicTreeIten.getChildren();
                });
                return children;
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                log.error("Error loading topics", e);
                conn.getCluster().setStatus(KafkaCluster.ClusterStatus.DISCONNECTED);
                throw new RuntimeException(e);
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
