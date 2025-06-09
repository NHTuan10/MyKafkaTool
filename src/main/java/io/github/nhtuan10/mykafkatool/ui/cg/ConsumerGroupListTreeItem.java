package io.github.nhtuan10.mykafkatool.ui.cg;

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

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ConsumerGroupListTreeItem<T> extends TreeItem<T> {
    public ConsumerGroupListTreeItem(T value) {
        super(value);
    }

    public ConsumerGroupListTreeItem(T value, Node graphic) {
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

    private ObservableList<TreeItem<T>> buildChildren(TreeItem<T> treeItem) {

        T value = treeItem.getValue();
        if (value instanceof ConsumerGroupListTreeItemValue consumerGroupListTreeItemValue) {
            ObservableList<TreeItem<T>> children = FXCollections.observableArrayList();
            String clusterName = consumerGroupListTreeItemValue.getCluster().getName();
            try {
                List<ConsumerGroupListing> consumerGroupListings = ClusterManager.getInstance().getConsumerGroupList(clusterName).stream()
                        .sorted(Comparator.comparing(ConsumerGroupListing::groupId)).toList();
                consumerGroupListings.forEach(consumerGroupListing -> {
                    String consumerGroupId = consumerGroupListing.groupId();
                    StringBuilder displayValSB = new StringBuilder(consumerGroupId);
                    consumerGroupListing.state().ifPresent(state -> displayValSB.append(" [").append(state).append("]"));

                    TreeItem<T> consumerGroupItem = (TreeItem<T>) new ConsumerGroupTreeItem(displayValSB.toString(), clusterName, consumerGroupId);
                    children.add(consumerGroupItem);
                });
                // ClusterManager.getInstance().getConsumerGroup(clusterName, consumerGroupListings.stream().map(ConsumerGroupListing::groupId).collect(Collectors.toList()));

                return children;
            } catch (ExecutionException | InterruptedException e) {
                log.error("Error loading consumer groups", e);
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
            this.display = AppConstant.TREE_ITEM_CONSUMER_GROUPS_DISPLAY_NAME;
        }
    }
}
