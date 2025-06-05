package io.github.nhtuan10.mykafkatool.ui.cluster;

import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.exception.ClusterNameExistedException;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.manager.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.manager.UserPreferenceManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.CopyTextMenuItem;
import io.github.nhtuan10.mykafkatool.ui.cg.ConsumerGroupListTreeItem;
import io.github.nhtuan10.mykafkatool.ui.cg.ConsumerGroupTreeItem;
import io.github.nhtuan10.mykafkatool.ui.event.*;
import io.github.nhtuan10.mykafkatool.ui.partition.KafkaPartitionTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicListTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicTreeItem;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
public class KafkaClusterTree {
    final private ClusterManager clusterManager;

    final private TreeView clusterTree;

    final private SchemaRegistryManager schemaRegistryManager;

    final private EventDispatcher eventDispatcher;

    @Setter
    private Stage stage;

    public static void initClusterPanel(Stage stage) {
        TreeView clusterTree = (TreeView) stage.getScene().lookup("#clusterTree");

        TreeItem<Object> clustersItem = new TreeItem<>(AppConstant.TREE_ITEM_CLUSTERS_DISPLAY_NAME);
        clustersItem.setExpanded(true);
        clusterTree.setRoot(clustersItem);

        UserPreferenceManager.loadUserPreference().connections().forEach((cluster -> {
            try {
                if (!isClusterNameExistedInTree(clusterTree, cluster.getName())) {
                    addClusterToTreeView(clusterTree, cluster);
                }
            } catch (ClusterNameExistedException e) {
                log.error("Error when add new connection during loading user preferences", e);
                throw new RuntimeException(e);
            }
        }));
        //TODO: add  search topic, cluster, consumer groups function. Also show brokers & topic table, in-sync replicas, total topics, partitions


//        TreeView<String> tree = new TreeView<String> (rootItem);

//        clusterTree.setEditable(true);
//        clusterTree.setCellFactory((Callback<TreeView<String>, TreeCell<String>>) p -> new TextFieldTreeCellImpl());
        clusterTree.setCellFactory(param -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item.toString());
                    setTooltip(new Tooltip(item.toString()));
                }
            }
        });
    }

    public static void addClusterToTreeView(TreeView clusterTree, KafkaCluster cluster) throws ClusterNameExistedException {
        ClusterManager.getInstance().connectToCluster(cluster);
        String clusterName = cluster.getName();
        if (isClusterNameExistedInTree(clusterTree, clusterName)) {
            throw new ClusterNameExistedException(clusterName, "Cluster already exists");
        }
        TreeItem<Object> brokerTreeItem = new TreeItem<>(cluster);
        TreeItem<Object> topicListTreeItem = new KafkaTopicListTreeItem<>(new KafkaTopicListTreeItem.KafkaTopicListTreeItemValue(cluster));
        ConsumerGroupListTreeItem<Object> consumerGroupListTreeItem = new ConsumerGroupListTreeItem<>(new ConsumerGroupListTreeItem.ConsumerGroupListTreeItemValue(cluster));
        brokerTreeItem.getChildren().add(topicListTreeItem);
        brokerTreeItem.getChildren().add(consumerGroupListTreeItem);
        if (StringUtils.isNotBlank(cluster.getSchemaRegistryUrl())) {
            TreeItem<Object> schemaRegistry = new TreeItem<>(AppConstant.TREE_ITEM_SCHEMA_REGISTRY_DISPLAY_NAME);
            brokerTreeItem.getChildren().add(schemaRegistry);
            SchemaRegistryManager.getInstance().connectToSchemaRegistry(cluster);
        }
        clusterTree.getRoot().getChildren().add(brokerTreeItem);
    }

    public static void connectToClusterAndLoadAllChildren(TreeView clusterTree, KafkaCluster cluster) throws ClusterNameExistedException {
        ClusterManager.getInstance().connectToCluster(cluster);
        String clusterName = cluster.getName();
        if (isClusterNameExistedInTree(clusterTree, clusterName)) {
            throw new ClusterNameExistedException(clusterName, "Cluster already exists");
        }

        TreeItem<Object> brokerTreeItem = new TreeItem<>(cluster);
        TreeItem<Object> topicListTreeItem = new KafkaTopicListTreeItem<>(new KafkaTopicListTreeItem.KafkaTopicListTreeItemValue(cluster));
        ConsumerGroupListTreeItem<Object> consumerGroupListTreeItem = new ConsumerGroupListTreeItem<>(new ConsumerGroupListTreeItem.ConsumerGroupListTreeItemValue(cluster));

        topicListTreeItem.getChildren();
        brokerTreeItem.getChildren().add(topicListTreeItem);

        consumerGroupListTreeItem.getChildren();
        brokerTreeItem.getChildren().add(consumerGroupListTreeItem);

        if (StringUtils.isNotBlank(cluster.getSchemaRegistryUrl())) {
            TreeItem<Object> schemaRegistry = new TreeItem<>(AppConstant.TREE_ITEM_SCHEMA_REGISTRY_DISPLAY_NAME);
            brokerTreeItem.getChildren().add(schemaRegistry);
            SchemaRegistryManager.getInstance().connectToSchemaRegistry(cluster);
        }

        clusterTree.getRoot().getChildren().add(brokerTreeItem);
    }

    public static boolean isClusterNameExistedInTree(TreeView clusterTree, String clusterName) throws ClusterNameExistedException {
        return ((ObservableList<TreeItem>) clusterTree.getRoot().getChildren()).stream()
                .anyMatch(treeItem -> ((KafkaCluster) treeItem.getValue()).getName().equals(clusterName));
    }

    public void addTopic() throws IOException, InterruptedException, ExecutionException {
        if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicListTreeItem<?> topicListTreeItem) {
            String clusterName = ((KafkaTopicListTreeItem.KafkaTopicListTreeItemValue) topicListTreeItem.getValue()).getCluster().getName();
            AtomicReference<Object> modelRef = new AtomicReference<>();
            ViewUtils.showPopUpModal("add-topic-modal.fxml", "Add New Topic", modelRef, Map.of(), stage);
            NewTopic newTopic = (NewTopic) modelRef.get();
            if (newTopic != null) {
                CreateTopicsResult result = clusterManager.addTopic(clusterName, newTopic);
                result.all().get();
                topicListTreeItem.reloadChildren();
            }
        }

    }

    public void configureClusterTreeActionMenu() {
        MenuItem blankItem = new MenuItem("");
        blankItem.setVisible(false);

        ContextMenu clusterTreeContextMenu = new ContextMenu(blankItem);

        clusterTreeContextMenu.setOnShowing(ae -> {
            TreeItem treeItem = (TreeItem) clusterTree.getSelectionModel().getSelectedItem();

            CopyTextMenuItem copyTreeItemNameMenuItem = new CopyTextMenuItem(MessageFormat.format("Copy \"{0}\"", treeItem.getValue().toString()));
            copyTreeItemNameMenuItem.setOnAction(actionEvent -> {
                final ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(treeItem.getValue().toString());
                Clipboard.getSystemClipboard().setContent(clipboardContent);
            });

            if ((treeItem == null) || (treeItem.getParent() == null && AppConstant.TREE_ITEM_CLUSTERS_DISPLAY_NAME.equalsIgnoreCase((String) treeItem.getValue()))) {
                clusterTreeContextMenu.getItems().setAll(createAddingConnectionActionMenuItem());
            } else if (treeItem.getValue() instanceof KafkaCluster) { // tree item for a connection
                clusterTreeContextMenu.getItems().setAll(createEditingConnectionActionMenuItem(treeItem), createDeletingConnectionActionMenuItem(treeItem));
            } else if (treeItem instanceof KafkaTopicListTreeItem<?> topicListTreeItem) {
                // TODO: create a view with topic table or have a search topic function in topic list tree item
                MenuItem refreshItem = new MenuItem("Refresh");
                refreshItem.setOnAction(actionEvent -> topicListTreeItem.reloadChildren());
                clusterTreeContextMenu.getItems().setAll(createAddingTopicActionMenuItem(), refreshItem);
            } else if (treeItem instanceof KafkaTopicTreeItem<?>) {
                // TODO:  edit topic menu item
                clusterTreeContextMenu.getItems().setAll(getTopicActionMenuItems());
                clusterTreeContextMenu.getItems().add(copyTreeItemNameMenuItem);
            } else if (treeItem instanceof KafkaPartitionTreeItem<?> selectedPartitionTreeItem) {
                MenuItem refreshItem = new MenuItem("Refresh Metadata");
                KafkaPartition partition = (KafkaPartition) selectedPartitionTreeItem.getValue();
                refreshItem.setOnAction(actionEvent -> eventDispatcher.publishEvent(PartitionUIEvent.newRefreshPartitionEven(partition)));
                clusterTreeContextMenu.getItems().setAll(refreshItem, createPurgingPartitionActionMenuItem(partition));
            } else if (treeItem instanceof ConsumerGroupListTreeItem<?> consumerGroupListTreeItem) {
                MenuItem refreshItem = new MenuItem("Refresh");
                refreshItem.setOnAction(actionEvent -> consumerGroupListTreeItem.reloadChildren());
                clusterTreeContextMenu.getItems().setAll(refreshItem);
            } else if (treeItem instanceof ConsumerGroupTreeItem consumerGroupTreeItem) {
                clusterTreeContextMenu.getItems().setAll(copyTreeItemNameMenuItem);
            } else if (AppConstant.TREE_ITEM_SCHEMA_REGISTRY_DISPLAY_NAME.equalsIgnoreCase((String) treeItem.getValue())) {
                MenuItem refreshItem = new MenuItem("Refresh");
                KafkaCluster kafkaCluster = (KafkaCluster) treeItem.getParent().getValue();
                refreshItem.setOnAction(actionEvent -> {
//                    try {
//                        schemaRegistryControl.refresh();
//                    } catch (RestClientException | IOException | ExecutionException | InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
                    this.eventDispatcher.publishEvent(new SchemaRegistryUIEvent(kafkaCluster, UIEvent.Action.REFRESH_SCHEMA_REGISTRY));
                });
                clusterTreeContextMenu.getItems().setAll(refreshItem);
            } else {
                clusterTreeContextMenu.getItems().setAll(blankItem);
            }
            // TODO: Add refresh for consumer group too
        });
        clusterTree.setContextMenu(clusterTreeContextMenu);
    }

    private List<MenuItem> getTopicActionMenuItems() {
        MenuItem deleteTopicItem = createDeleteTopicActionMenuItem();
        MenuItem purgeTopicItem = createPurgeTopicActionMenuItem();
        MenuItem refreshTopicItem = createRefreshTopicActionMenuItem();
        return List.of(refreshTopicItem, purgeTopicItem, deleteTopicItem);
    }

    private MenuItem createPurgingPartitionActionMenuItem(KafkaPartition partition) {
        MenuItem purgePartitionItem = new MenuItem("Purge Partition");
        purgePartitionItem.setOnAction(ae -> {
//            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaPartitionTreeItem<?> selectedPartitionTreeItem) {
//            KafkaPartition partition = (KafkaPartition) selectedPartitionTreeItem.getValue();
            if (ViewUtils.confirmAlert("Purge Partition", "Are you sure to delete all data in the partition " + partition.id() + " ?", "Yes", "Cancel")) {
                try {
                    clusterManager.purgePartition(partition);
                    this.eventDispatcher.publishEvent(PartitionUIEvent.newRefreshPartitionEven(partition));
                } catch (ExecutionException | InterruptedException e) {
                    log.error("Error when purge partition", e);
                    throw new RuntimeException(e);
                }
            }
//            }
        });
        return purgePartitionItem;
    }

    private MenuItem createPurgeTopicActionMenuItem() {
        MenuItem purgeTopicItem = new MenuItem("Purge Topic");
        purgeTopicItem.setOnAction(ae -> {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedTopicTreeItem) {
                KafkaTopic topic = (KafkaTopic) selectedTopicTreeItem.getValue();
                if (ViewUtils.confirmAlert("Purge Topic", "Are you sure to delete all data in the topic " + topic.name() + " ?", "Yes", "Cancel")) {
                    try {
                        clusterManager.purgeTopic(topic);
                        eventDispatcher.publishEvent(TopicUIEvent.newRefreshTopicEven(topic));
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        log.error("Error when purge topic", e);
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        return purgeTopicItem;
    }

    private MenuItem createRefreshTopicActionMenuItem() {
        MenuItem refreshTopicItem = new MenuItem("Refresh Metadata");
        refreshTopicItem.setOnAction(ae -> {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedTopicTreeItem) {
                KafkaTopic topic = (KafkaTopic) selectedTopicTreeItem.getValue();
                selectedTopicTreeItem.reloadChildren();
                eventDispatcher.publishEvent(TopicUIEvent.newRefreshTopicEven(topic));
            }
        });
        return refreshTopicItem;
    }

    private MenuItem createAddingTopicActionMenuItem() {
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

    private MenuItem createDeleteTopicActionMenuItem() {
        MenuItem deleteTopicItem = new MenuItem("Delete");
        deleteTopicItem.setOnAction(ae -> {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedTopicTreeItem) {
                KafkaTopic topic = (KafkaTopic) selectedTopicTreeItem.getValue();
                if (ViewUtils.confirmAlert("Delete Topic", "Are you sure to delete " + topic.name() + " ?", "Yes", "Cancel")) {
                    try {
                        clusterManager.deleteTopic(topic.cluster().getName(), topic.name()).all().get();
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    selectedTopicTreeItem.getParent().getChildren().remove(selectedTopicTreeItem);
                }
            }
        });
        return deleteTopicItem;
    }

    private MenuItem createAddingConnectionActionMenuItem() {
        MenuItem addNewConnectionItem = new MenuItem("Add New Connection");
        addNewConnectionItem.setOnAction(ae -> {
            addNewConnection();
        });
        return addNewConnectionItem;
    }

    public void addNewConnection() {
        try {
            KafkaCluster newConnection;
            while (true) {
                AtomicReference<Object> modelRef = new AtomicReference<>();
                ViewUtils.showPopUpModal("add-connection-modal.fxml", "Add New Connection", modelRef, Map.of(), stage);
                newConnection = (KafkaCluster) modelRef.get();

                if (newConnection != null && (StringUtils.isBlank(newConnection.getName()) || StringUtils.isBlank(newConnection.getBootstrapServer()) || isClusterNameExistedInTree(clusterTree, newConnection.getName()))) {
                    String clusterName = newConnection.getName();
                    log.warn("User enter an invalid cluster name {} or bootstrap server", clusterName);
                    ViewUtils.showAlertDialog(Alert.AlertType.WARNING, "Cluster name " + clusterName + " or bootstrap server is invalid, please try again. Please note that cluster name need to be unique", "Invalid Or Duplicated Connection", ButtonType.OK);
                } else {
                    break;
                }
            }
            if (newConnection != null) {
                connectToClusterAndSchemaRegistry(clusterTree, newConnection);
                UserPreferenceManager.addClusterToUserPreference(newConnection);
            }

        } catch (IOException | ClusterNameExistedException e) {
            log.error("Error when add new connection", e);
            throw new RuntimeException(e);
        }
    }

    private MenuItem createEditingConnectionActionMenuItem(TreeItem<KafkaCluster> selectedItem) {
        MenuItem editConnectionItem = new MenuItem("Edit Connection");
        editConnectionItem.setOnAction(ae -> {

            if (selectedItem != null) {
                KafkaCluster oldConnection = selectedItem.getValue();
                KafkaCluster newConnection;
                try {
                    while (true) {
                        AtomicReference<Object> modelRef = new AtomicReference<>();
                        ViewUtils.showPopUpModal("add-connection-modal.fxml", "Edit Connection", modelRef,
                                Map.of("objectProperty", oldConnection), stage);
                        newConnection = (KafkaCluster) modelRef.get();
                        if (newConnection != null &&
                                (StringUtils.isBlank(newConnection.getName()) || StringUtils.isBlank(newConnection.getBootstrapServer()) || isClusterNameExistedInParentTree(selectedItem, newConnection.getName()))) {
                            String clusterName = newConnection.getName();
                            log.warn("User enter an invalid cluster name {} or bootstrap server", clusterName);
                            ViewUtils.showAlertDialog(Alert.AlertType.WARNING, "Cluster name " + clusterName + " or bootstrap server is invalid, please try again. Please note that cluster name need to be unique", "Invalid Or Duplicated Connection", ButtonType.OK);
                        } else {
                            //TODO: add dialog to confirm if user want to close connection and replace with new connection
                            break;
                        }
                    }
                    if (newConnection != null && !oldConnection.equals(newConnection)) {
                        deleteConnection(selectedItem);
                        connectToClusterAndSchemaRegistry(clusterTree, newConnection);
                        UserPreferenceManager.addClusterToUserPreference(newConnection);
                    }
                } catch (IOException | ClusterNameExistedException e) {
                    log.error("Error when add new connection", e);
                    throw new RuntimeException(e);
                }
            }
        });
        return editConnectionItem;
    }

    private boolean isClusterNameExistedInParentTree(TreeItem<KafkaCluster> kafkaClusterTreeItem, String newClusterName) {
        return getListOfOtherClusterName(kafkaClusterTreeItem).contains(newClusterName);
    }

    private List<String> getListOfOtherClusterName(TreeItem<KafkaCluster> inKafkaClusterTreeItem) {
        return inKafkaClusterTreeItem.getParent().getChildren()
                .stream()
                .filter(kafkaClusterTreeItem -> kafkaClusterTreeItem != inKafkaClusterTreeItem)
                .map(treeItem -> treeItem.getValue().getName())
                .toList();
    }

    private static void connectToClusterAndSchemaRegistry(TreeView clusterTree, KafkaCluster cluster) throws ClusterNameExistedException {
        connectToClusterAndLoadAllChildren(clusterTree, cluster);
    }

    private MenuItem createDeletingConnectionActionMenuItem(TreeItem<KafkaCluster> selectedItem) {
        MenuItem deleteConnectionItem = new MenuItem("Delete Connection");
        deleteConnectionItem.setOnAction(ae -> {
            // Remove the selected item from its parent's children
            deleteConnection(selectedItem);
        });
        return deleteConnectionItem;
    }

    private void deleteConnection(TreeItem<KafkaCluster> selectedItem) {
        String clusterName = selectedItem.getValue().getName();
        clusterManager.closeClusterConnection(clusterName);
        schemaRegistryManager.disconnectFromSchemaRegistry(clusterName);
        selectedItem.getParent().getChildren().remove(selectedItem);
        try {
            UserPreferenceManager.removeClusterFromUserPreference(clusterName);
        } catch (IOException e) {
            log.error("Error when removing connection", e);
            throw new RuntimeException(e);
        }
    }

}

