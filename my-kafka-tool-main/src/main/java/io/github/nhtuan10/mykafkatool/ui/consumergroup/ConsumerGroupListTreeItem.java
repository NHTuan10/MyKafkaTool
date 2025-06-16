package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.common.ConsumerGroupState;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class ConsumerGroupListTreeItem<T> extends TreeItem<T> {
    private final ClusterManager clusterManager;

    public ConsumerGroupListTreeItem(T value, ClusterManager clusterManager) {
        super(value);
        this.clusterManager = clusterManager;
    }

    public ConsumerGroupListTreeItem(T value, Node graphic, ClusterManager clusterManager) {
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
            super.getChildren().setAll(buildChildren(this));
        }
        return super.getChildren();
    }

    private ObservableList<TreeItem<T>> buildChildren(TreeItem<T> treeItem) {

        T value = treeItem.getValue();
        if (value instanceof ConsumerGroupListTreeItemValue consumerGroupListTreeItemValue) {
            ObservableList<TreeItem<T>> children = FXCollections.observableArrayList();
            String clusterName = consumerGroupListTreeItemValue.getCluster().getName();
            try {
                List<ConsumerGroupListing> consumerGroupListings = clusterManager.getConsumerGroupList(clusterName).stream()
                        .sorted(Comparator.comparing(ConsumerGroupListing::groupId)).toList();
                consumerGroupListings.forEach(consumerGroupListing -> {
                    String consumerGroupId = consumerGroupListing.groupId();
                    StringBuilder displayValSB = new StringBuilder(consumerGroupId);
                    String state = consumerGroupListing.state().map(ConsumerGroupState::toString).orElse("");
                    displayValSB.append(" [").append(state).append("]");

                    TreeItem<T> consumerGroupItem = (TreeItem<T>) new ConsumerGroupTreeItem(displayValSB.toString(), clusterName, consumerGroupId, state);
                    children.add(consumerGroupItem);
                });
                // clusterManager.getConsumerGroup(clusterName, consumerGroupListings.stream().map(ConsumerGroupListing::groupId).collect(Collectors.toList()));

                return children;
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                log.error("Error loading consumer groups", e);
                consumerGroupListTreeItemValue.getCluster().setStatus(KafkaCluster.ClusterStatus.DISCONNECTED);
                throw new RuntimeException(e);
            }
        }
        return FXCollections.emptyObservableList();
    }

    @Data
    public static class ConsumerGroupListTreeItemValue {
        private KafkaCluster cluster;
        private String display;

        @Override
        public String toString() {
            return display;
        }

        public ConsumerGroupListTreeItemValue(KafkaCluster cluster) {
            this.cluster = cluster;
            this.display = AppConstant.CONSUMER_GROUPS_TREE_ITEM_DISPLAY_NAME;
        }
    }
}
