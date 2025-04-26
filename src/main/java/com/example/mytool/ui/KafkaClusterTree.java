package com.example.mytool.ui;

import com.example.mytool.constant.AppConstant;
import com.example.mytool.exception.ClusterNameExistedException;
import com.example.mytool.manager.ClusterManager;
import com.example.mytool.manager.UserPreferenceManager;
import com.example.mytool.model.kafka.KafkaCluster;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.ui.util.ViewUtil;
import javafx.scene.control.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class KafkaClusterTree {
    ClusterManager clusterManager;

    TreeView clusterTree;

    public KafkaClusterTree(ClusterManager clusterManager, TreeView clusterTree) {
        this.clusterManager = clusterManager;
        this.clusterTree = clusterTree;
    }

    public void addTopic() throws IOException, InterruptedException, ExecutionException {
        if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicListTreeItem<?> topicListTreeItem) {
            String clusterName = ((KafkaTopicListTreeItem.KafkaTopicListTreeItemValue) topicListTreeItem.getValue()).getCluster().getName();
            AtomicReference modelRef = new AtomicReference<>();
            ViewUtil.showAddModal("add-topic-modal.fxml", "Add New Topic", modelRef, Map.of());
            NewTopic newTopic = (NewTopic) modelRef.get();
            CreateTopicsResult result = clusterManager.addTopic(clusterName, newTopic);
            result.all().get();
            topicListTreeItem.reloadChildren();
        }

    }

    public void configureClusterTreeActionMenu() {
        MenuItem addNewConnectionItem = configureAddConnectionActionMenuItem();
        MenuItem blankItem = new MenuItem("");
//        blankItem.setDisable(true);
        blankItem.setVisible(false);

        MenuItem deleteTopicItem = configureDeleteActionMenuItem();

        MenuItem addNewTopicItem = configureAddTopicActionMenuItem();

        MenuItem purgeTopicItem = configurePurgeTopicActionMenuItem();

        MenuItem purgePartitionItem = configurePurgePartitionActionMenuItem();

        MenuItem deleteConnectionItem = configureDeleteConnectionActionMenuItem();

        ContextMenu clusterTreeContextMenu = new ContextMenu(blankItem);

        clusterTreeContextMenu.setOnShowing(ae -> {
            TreeItem treeItem = (TreeItem) clusterTree.getSelectionModel().getSelectedItem();
            if ((treeItem == null) || (treeItem.getParent() == null && AppConstant.TREE_ITEM_CLUSTERS_DISPLAY_NAME.equalsIgnoreCase((String) treeItem.getValue()))) {
                clusterTreeContextMenu.getItems().setAll(addNewConnectionItem);
            } else if (treeItem.getParent() != null && treeItem.getParent().getParent() == null) {
                clusterTreeContextMenu.getItems().setAll(deleteConnectionItem);
            } else if (treeItem instanceof KafkaTopicListTreeItem<?> topicListTreeItem) {
                MenuItem refreshItem = new MenuItem("Refresh");
                refreshItem.setOnAction(actionEvent -> topicListTreeItem.reloadChildren());
                clusterTreeContextMenu.getItems().setAll(addNewTopicItem, refreshItem);
            } else if (treeItem instanceof KafkaTopicTreeItem<?>) {
                clusterTreeContextMenu.getItems().setAll(deleteTopicItem, purgeTopicItem);
            } else if (treeItem instanceof KafkaPartitionTreeItem<?>) {
                clusterTreeContextMenu.getItems().setAll(purgePartitionItem);
            } else if (treeItem instanceof ConsumerGroupListTreeItem<?> consumerGroupListTreeItem) {
                MenuItem refreshItem = new MenuItem("Refresh");
                refreshItem.setOnAction(actionEvent -> consumerGroupListTreeItem.reloadChildren());
                clusterTreeContextMenu.getItems().setAll(addNewTopicItem, refreshItem);
            } else {
                clusterTreeContextMenu.getItems().setAll(blankItem);

            }
        });
        clusterTree.setContextMenu(clusterTreeContextMenu);
    }

    private MenuItem configurePurgePartitionActionMenuItem() {
        MenuItem purgePartitionItem = new MenuItem("Purge Partition");
        purgePartitionItem.setOnAction(ae -> {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaPartitionTreeItem<?> selectedPartitionTreeItem) {
                KafkaPartition partition = (KafkaPartition) selectedPartitionTreeItem.getValue();
                if (ViewUtil.confirmAlert("Purge Partition", "Are you sure to delete all data in the partition " + partition.getId() + " ?", "Yes", "Cancel")) {
                    try {
                        clusterManager.purgePartition(partition);
                    } catch (ExecutionException | InterruptedException e) {
                        log.error("Error when purge partition", e);
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        return purgePartitionItem;
    }

    private MenuItem configurePurgeTopicActionMenuItem() {
        MenuItem purgeTopicItem = new MenuItem("Purge Topic");
        purgeTopicItem.setOnAction(ae -> {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedTopicTreeItem) {
                KafkaTopic topic = (KafkaTopic) selectedTopicTreeItem.getValue();
                if (ViewUtil.confirmAlert("Purge Topic", "Are you sure to delete all data in the topic " + topic.getName() + " ?", "Yes", "Cancel")) {
                    try {
                        clusterManager.purgeTopic(topic);
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        log.error("Error when purge topic", e);
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        return purgeTopicItem;
    }

    private MenuItem configureAddTopicActionMenuItem() {
        MenuItem addNewTopicItem = new MenuItem("Add New Topic");
        addNewTopicItem.setOnAction(ae -> {
            try {
                addTopic();

            } catch (IOException | ExecutionException | InterruptedException e) {
                log.error("Error when add new topic", e);
                throw new RuntimeException(e);
            }
        });
        return addNewTopicItem;
    }

    private MenuItem configureDeleteActionMenuItem() {
        MenuItem deleteTopicItem = new MenuItem("Delete");
        deleteTopicItem.setOnAction(ae -> {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedTopicTreeItem) {
                KafkaTopic topic = (KafkaTopic) selectedTopicTreeItem.getValue();
                if (ViewUtil.confirmAlert("Delete Topic", "Are you sure to delete " + topic.getName() + " ?", "Yes", "Cancel")) {
                    clusterManager.deleteTopic(topic.getCluster().getName(), topic.getName());
                }
            }
        });
        return deleteTopicItem;
    }

    private MenuItem configureAddConnectionActionMenuItem() {
        MenuItem addNewConnectionItem = new MenuItem("Add New Connection");
        addNewConnectionItem.setOnAction(ae -> {
            try {
//                TreeItem treeItem = (TreeItem) clusterTree.getSelectionModel().getSelectedItem();
//                if (treeItem != null && treeItem.getParent() == null && AppConstant.TREE_ITEM_CLUSTERS_DISPLAY_NAME.equalsIgnoreCase((String) treeItem.getValue())){
                Pair<String, String> newConnection;
                while (true) {
                    AtomicReference modelRef = new AtomicReference<>();
                    ViewUtil.showAddModal("add-connection-modal.fxml", "Add New Connection", modelRef, Map.of());
                    newConnection = (Pair<String, String>) modelRef.get();

                    if (newConnection != null && (StringUtils.isBlank(newConnection.getLeft()) || StringUtils.isBlank(newConnection.getRight()) || ViewUtil.isClusterNameExistedInTree(clusterTree, newConnection.getLeft()))) {
                        String clusterName = newConnection.getLeft();
                        log.warn("User enter an invalid cluster name {} or bootstrap server", clusterName);
                        ViewUtil.showAlertDialog(Alert.AlertType.WARNING, "Cluster name " + clusterName + " or bootstrap server is invalid, please try again. Please note that cluster name need to be unique", "Invalid Or Duplicated Connection", ButtonType.OK);
                    } else {
                        break;
                    }
                }
                if (newConnection != null) {
                    KafkaCluster cluster = new KafkaCluster(newConnection.getLeft(), newConnection.getRight());
                    ClusterManager.getInstance().connectToCluster(cluster);
                    ViewUtil.addClusterConnIntoClusterTreeView(clusterTree, cluster);
                    UserPreferenceManager.addClusterToUserPreference(cluster);
                }

            } catch (IOException | ClusterNameExistedException e) {
                log.error("Error when add new connection", e);
                throw new RuntimeException(e);
            }
        });
        return addNewConnectionItem;
    }

    private MenuItem configureDeleteConnectionActionMenuItem() {
        MenuItem addNewConnectionItem = new MenuItem("Delete Connection");
        addNewConnectionItem.setOnAction(ae -> {
            TreeItem<String> selectedItem = (TreeItem<String>) clusterTree.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getParent() != null) {
                // Remove the selected item from its parent's children
                String clusterName = selectedItem.getValue().toString();
                clusterManager.closeClusterConnection(clusterName);
                selectedItem.getParent().getChildren().remove(selectedItem);
                try {
                    UserPreferenceManager.removeClusterFromUserPreference(clusterName);
                } catch (IOException e) {
                    log.error("Error when removing connection", e);
                    throw new RuntimeException(e);
                }
            }

        });
        return addNewConnectionItem;
    }
}

