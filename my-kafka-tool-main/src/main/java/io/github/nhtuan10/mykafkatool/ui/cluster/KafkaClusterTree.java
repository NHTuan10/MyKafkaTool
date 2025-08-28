package io.github.nhtuan10.mykafkatool.ui.cluster;

import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.constant.UIStyleConstant;
import io.github.nhtuan10.mykafkatool.exception.ClusterNameExistedException;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.schemaregistry.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.ui.consumergroup.ConsumerGroupListTreeItem;
import io.github.nhtuan10.mykafkatool.ui.consumergroup.ConsumerGroupTreeItem;
import io.github.nhtuan10.mykafkatool.ui.control.CopyTextMenuItem;
import io.github.nhtuan10.mykafkatool.ui.control.FilterableTreeItem;
import io.github.nhtuan10.mykafkatool.ui.event.*;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaPartitionTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicListTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicTreeItem;
import io.github.nhtuan10.mykafkatool.ui.util.ModalUtils;
import io.github.nhtuan10.mykafkatool.userpreference.UserPreference;
import io.github.nhtuan10.mykafkatool.userpreference.UserPreferenceManager;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
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
import java.util.function.Predicate;

@Slf4j
public class KafkaClusterTree {
    private final ClusterManager clusterManager;

    private final TreeView clusterTree;

    private final SchemaRegistryManager schemaRegistryManager;

    private final EventDispatcher eventDispatcher;

    private final UserPreferenceManager userPreferenceManager;

    @Setter
    private Stage stage;

    public KafkaClusterTree(ClusterManager clusterManager, TreeView clusterTree, SchemaRegistryManager schemaRegistryManager,
                            EventDispatcher eventDispatcher, UserPreferenceManager userPreferenceManager) {
        this.clusterManager = clusterManager;
        this.clusterTree = clusterTree;
        this.schemaRegistryManager = schemaRegistryManager;
        this.eventDispatcher = eventDispatcher;
        this.userPreferenceManager = userPreferenceManager;
//        TreeView clusterTree = (TreeView) stage.getScene().lookup("#clusterTree");

        TreeItem<Object> clustersItem = new TreeItem<>(AppConstant.CLUSTERS_TREE_ITEM_DISPLAY_NAME);
        clustersItem.setExpanded(true);
        clusterTree.setRoot(clustersItem);

        addAllConnectionsFromUserPreference(userPreferenceManager.loadUserPreference());
        //TODO: add  search topic, cluster, consumer groups function. Also show brokers & topic table, in-sync replicas, total topics, partitions


//        TreeView<String> tree = new TreeView<String> (rootItem);

//        clusterTree.setEditable(true);
//        clusterTree.setCellFactory((Callback<TreeView<String>, TreeCell<String>>) p -> new TextFieldTreeCellImpl());
        clusterTree.setCellFactory(param -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                TreeItem treeItem = getTreeItem();
                addStyle(item);
                if (empty) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item.toString());
                    if (item instanceof KafkaCluster cluster) {
                        setTooltip(new Tooltip("%s [%s]".formatted(cluster.getName(), cluster.getStatus())));
                    } else if (item instanceof KafkaTopic topic && treeItem instanceof KafkaTopicTreeItem<?> topicTreeItem) {
                        setTooltip(new Tooltip("%s [%s Partitions]".formatted(topic.name(), topicTreeItem.getNumOfPartitions())));
                    } else {
                        setTooltip(new Tooltip(item.toString()));
                    }
                }
            }

            private void addStyle(Object item) {
                if (getStyleClass() != null) {
                    List<String> treeItemClasses = List.of(UIStyleConstant.CLUSTER_TREE_ROOT_CLASS, UIStyleConstant.CLUSTER_TREE_ITEM_CLASS);
                    this.getStyleClass().removeAll(treeItemClasses);
                    if (item instanceof String rootItemVal && AppConstant.CLUSTERS_TREE_ITEM_DISPLAY_NAME.equalsIgnoreCase(rootItemVal)) {

                        this.getStyleClass().add(UIStyleConstant.CLUSTER_TREE_ROOT_CLASS);
                    } else if (item instanceof KafkaCluster) {
                        this.getStyleClass().add(UIStyleConstant.CLUSTER_TREE_ITEM_CLASS);
                    }

                }

            }
        });
    }

    public void setTreeItemFilterPredicate(Predicate<Object> predicate) {
        @SuppressWarnings("unchecked")
        List<FilterableTreeItem<Object>> l = clusterTree.getRoot().getChildren().stream().flatMap(clusterTreeItem -> {
            TreeItem<?> item = (TreeItem<?>) clusterTreeItem;
            return item.getChildren().stream().filter(treeItem -> treeItem instanceof KafkaTopicListTreeItem<?> || treeItem instanceof ConsumerGroupListTreeItem<?>);
        }).toList();
        l.forEach(filterableTreeItem -> filterableTreeItem.predicateProperty().set(predicate));
    }
    public void addAllConnectionsFromUserPreference(UserPreference userPreference) {
        userPreference.connections().forEach((cluster -> {
            try {
                if (!isClusterNameExistedInTree(clusterTree, cluster.getName())) {
                    connectToClusterAndSchemaRegistry(clusterTree, cluster, false, true);
                }
            } catch (ClusterNameExistedException e) {
                log.error("Error when add new connection during loading user preferences", e);
                throw new RuntimeException(e);
            }
        }));
    }

    public static boolean isClusterNameExistedInTree(TreeView clusterTree, String clusterName) throws ClusterNameExistedException {
        return ((ObservableList<TreeItem>) clusterTree.getRoot().getChildren()).stream()
                .anyMatch(treeItem -> ((KafkaCluster) treeItem.getValue()).getName().equals(clusterName));
    }

    public void addTopic() throws IOException, InterruptedException, ExecutionException {
        if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicListTreeItem<?> topicListTreeItem) {
            String clusterName = ((KafkaTopicListTreeItem.KafkaTopicListTreeItemValue) topicListTreeItem.getValue()).getCluster().getName();
            AtomicReference<Object> modelRef = new AtomicReference<>();
            ModalUtils.showPopUpModal("add-topic-modal.fxml", "Add New Topic", modelRef, Map.of(), stage);
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

            if ((treeItem == null) || (treeItem.getParent() == null && AppConstant.CLUSTERS_TREE_ITEM_DISPLAY_NAME.equalsIgnoreCase((String) treeItem.getValue()))) {
                clusterTreeContextMenu.getItems().setAll(createAddingConnectionActionMenuItem());
            } else if (treeItem instanceof KafkaClusterTreeItem<?> kafkaClusterTreeItem) { // tree item for a connection
                clusterTreeContextMenu.getItems().setAll(
                        connectClusterActionMenuItem(kafkaClusterTreeItem)
                        , configureEditingConnectionActionMenuItem(kafkaClusterTreeItem)
                        , cloneClusterActionMenuItem(kafkaClusterTreeItem)
                        , createDeletingConnectionActionMenuItem(kafkaClusterTreeItem)
                        , disconnectClusterActionMenuItem(kafkaClusterTreeItem)
                );
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
            } else if (treeItem instanceof ConsumerGroupTreeItem) {
                clusterTreeContextMenu.getItems().setAll(copyTreeItemNameMenuItem);
            } else if (treeItem.getValue() instanceof String value && AppConstant.SCHEMA_REGISTRY_TREE_ITEM_DISPLAY_NAME.equalsIgnoreCase(value)) {
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
            if (ModalUtils.confirmAlert("Purge Partition", "Are you sure to delete all data in the partition " + partition.id() + " ?", "Yes", "Cancel")) {
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
                if (ModalUtils.confirmAlert("Purge Topic", "Are you sure to delete all data in the topic " + topic.name() + " ?", "Yes", "Cancel")) {
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
                eventDispatcher.publishEvent(SchemaRegistryUIEvent.newBackgroundRefreshEven(topic.cluster()));
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
                if (ModalUtils.confirmAlert("Delete Topic", "Are you sure to delete " + topic.name() + " ?", "Yes", "Cancel")) {
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
        addNewConnection(null);
    }

    public void addNewConnection(KafkaCluster clonedFrom) {
        try {
            KafkaCluster newConnection;
            while (true) {
                AtomicReference<Object> modelRef = new AtomicReference<>();
                final Map<String, Object> initValues = clonedFrom != null ? Map.of("objectProperty", clonedFrom) : Map.of();
                ModalUtils.showPopUpModal("add-connection-modal.fxml", "Add New Connection", modelRef, initValues, true, true, stage, true);
                newConnection = (KafkaCluster) modelRef.get();

                if (newConnection != null && (StringUtils.isBlank(newConnection.getName()) || StringUtils.isBlank(newConnection.getBootstrapServer()) || isClusterNameExistedInTree(clusterTree, newConnection.getName()))) {
                    String clusterName = newConnection.getName();
                    log.warn("User enter an invalid cluster name {} or bootstrap server", clusterName);
                    ModalUtils.showAlertDialog(Alert.AlertType.WARNING, "Cluster name " + clusterName + " or bootstrap server is invalid, please try again. Please note that cluster name need to be unique", "Invalid Or Duplicated Connection", ButtonType.OK);
                } else {
                    break;
                }
            }
            if (newConnection != null) {
                connectToClusterAndSchemaRegistry(clusterTree, newConnection, true, true);
                userPreferenceManager.addClusterToUserPreference(newConnection);
            }

        } catch (IOException | ClusterNameExistedException e) {
            log.error("Error when add new connection", e);
            throw new RuntimeException(e);
        }
    }

    private MenuItem configureEditingConnectionActionMenuItem(KafkaClusterTreeItem<?> selectedItem) {
        MenuItem editConnectionItem = new MenuItem("Edit Connection");
        editConnectionItem.setOnAction(ae -> {

            if (selectedItem != null) {
                KafkaCluster oldConnection = (KafkaCluster) selectedItem.getValue();
                KafkaCluster newConnection;
                try {
                    while (true) {
                        AtomicReference<Object> modelRef = new AtomicReference<>();
                        ModalUtils.showPopUpModal("add-connection-modal.fxml", "Edit Connection", modelRef,
                                Map.of("objectProperty", oldConnection), true, true, stage, true);
                        newConnection = (KafkaCluster) modelRef.get();
                        if (newConnection != null &&
                                (StringUtils.isBlank(newConnection.getName()) || StringUtils.isBlank(newConnection.getBootstrapServer()) || isClusterNameDuplicatedWithOthers(selectedItem, newConnection.getName()))) {
                            String clusterName = newConnection.getName();
                            log.warn("User enter an invalid cluster name {} or bootstrap server", clusterName);
                            ModalUtils.showAlertDialog(Alert.AlertType.WARNING, "Cluster name " + clusterName + " or bootstrap server is invalid, please try again. Please note that cluster name need to be unique", "Invalid Or Duplicated Connection", ButtonType.OK);
                        } else {
                            //TODO: add dialog to confirm if user want to close connection and replace with new connection
                            break;
                        }
                    }
                    if (newConnection != null && !oldConnection.equals(newConnection)) {
//                        deleteConnection(selectedItem);
                        disconnectKafkaClusterAndSchemaRegistry((KafkaCluster) selectedItem.getValue());
                        connectToClusterAndSchemaRegistry(clusterTree, newConnection, true, false);
                        userPreferenceManager.updateClusterToUserPreference(oldConnection, newConnection);
                    }
                } catch (IOException | ClusterNameExistedException e) {
                    log.error("Error when add new connection", e);
                    throw new RuntimeException(e);
                }
            }
        });
        return editConnectionItem;
    }

    private boolean isClusterNameDuplicatedWithOthers(KafkaClusterTreeItem<?> kafkaClusterTreeItem, String newClusterName) {
        return getListOfOtherClusterName(kafkaClusterTreeItem).contains(newClusterName);
    }

    private List<String> getListOfOtherClusterName(KafkaClusterTreeItem<?> inKafkaClusterTreeItem) {
        return inKafkaClusterTreeItem.getParent().getChildren()
                .stream()
                .filter(kafkaClusterTreeItem -> kafkaClusterTreeItem != inKafkaClusterTreeItem)
                .map(treeItem -> ((KafkaCluster) ((TreeItem) treeItem).getValue()).getName())
                .toList();
    }

    private void connectToClusterAndSchemaRegistry(TreeView clusterTree, KafkaCluster cluster, boolean loadAllChildren, boolean isANewConnection) throws ClusterNameExistedException {

        String clusterName = cluster.getName();
        if (isANewConnection && isClusterNameExistedInTree(clusterTree, clusterName)) {
            throw new ClusterNameExistedException(clusterName, "Cluster already exists");
        }
        clusterManager.connectToCluster(cluster);
        cluster.setStatus(KafkaCluster.ClusterStatus.CONNECTED);
        KafkaTopicListTreeItem<Object> topicListTreeItem;
        ConsumerGroupListTreeItem<Object> consumerGroupListTreeItem;
        KafkaClusterTreeItem clusterTreeItem;
        if (isANewConnection) {
            clusterTreeItem = new KafkaClusterTreeItem(cluster);
            clusterTree.getRoot().getChildren().add(clusterTreeItem);
        } else {
            clusterTreeItem = (KafkaClusterTreeItem) clusterTree.getSelectionModel().getSelectedItem();
            if (clusterTreeItem == null) {
                log.error("Selected cluster tree item is null");
                throw new RuntimeException("Unexpected error when connecting to cluster");
            }
            clusterTreeItem.setValue(cluster);
//            topicListTreeItem = clusterTreeItem.getKafkaTopicListTreeItem();
//            consumerGroupListTreeItem = clusterTreeItem.getConsumerGroupListTreeItem();
        }
        clusterTreeItem.removeKafkaTopicListTreeItem();
        clusterTreeItem.removeConsumerGroupListTreeItem();
        topicListTreeItem = new KafkaTopicListTreeItem<>(new KafkaTopicListTreeItem.KafkaTopicListTreeItemValue(cluster), clusterManager);
        consumerGroupListTreeItem = new ConsumerGroupListTreeItem<>(new ConsumerGroupListTreeItem.ConsumerGroupListTreeItemValue(cluster), clusterManager);
        clusterTreeItem.getChildren().add(topicListTreeItem);
        clusterTreeItem.getChildren().add(consumerGroupListTreeItem);
        try {
            if (loadAllChildren) {
                topicListTreeItem.getChildren();
                consumerGroupListTreeItem.getChildren();
            }
        } catch (RuntimeException e) {
            cluster.setStatus(KafkaCluster.ClusterStatus.DISCONNECTED);
            throw e;
        }

//            if (StringUtils.isNotBlank(cluster.getSchemaRegistryUrl())) {
//
//            }
        clusterTreeItem.addOrUpdateSchemaRegistryItem(schemaRegistryManager, cluster);

//        else {
//            clusterTreeItem.removeSchemaRegistryItem();
//        }
    }

    private MenuItem createDeletingConnectionActionMenuItem(KafkaClusterTreeItem<?> selectedItem) {
        MenuItem deleteConnectionItem = new MenuItem("Delete Connection");
        deleteConnectionItem.setOnAction(ae -> {
            // Remove the selected item from its parent's children
            if (ModalUtils.confirmAlert("Delete Connection", "Are you sure to delete connection " + selectedItem.getValue().toString() + " ?", "Yes", "Cancel")) {
                deleteConnection(selectedItem);
            }
        });
        return deleteConnectionItem;
    }

    private void deleteConnection(KafkaClusterTreeItem<?> selectedItem) {
        KafkaCluster cluster = (KafkaCluster) selectedItem.getValue();
        disconnectKafkaClusterAndSchemaRegistry(cluster);
        selectedItem.getParent().getChildren().remove(selectedItem);
        try {
            userPreferenceManager.removeClusterFromUserPreference(cluster.getName());
        } catch (IOException e) {
            log.error("Error when removing connection", e);
            throw new RuntimeException(e);
        }
    }

    private MenuItem connectClusterActionMenuItem(KafkaClusterTreeItem<?> selectedItem) {
        MenuItem disconnectItem = new MenuItem("Connect");
        KafkaCluster kafkaCluster = (KafkaCluster) selectedItem.getValue();
        disconnectItem.setVisible(kafkaCluster.getStatus() != KafkaCluster.ClusterStatus.CONNECTED);
        disconnectItem.setOnAction(ae -> {
            // Remove the selected item from its parent's children
            try {
                connectToClusterAndSchemaRegistry(this.clusterTree, kafkaCluster, true, false);
            } catch (ClusterNameExistedException e) {
                log.error("Error when connecting to cluster", e);
                throw new RuntimeException(e);
            }
        });
        return disconnectItem;
    }

    private MenuItem disconnectClusterActionMenuItem(KafkaClusterTreeItem<?> selectedItem) {
        MenuItem disconnectItem = new MenuItem("Disconnect");
        KafkaCluster kafkaCluster = (KafkaCluster) selectedItem.getValue();
        disconnectItem.setVisible(kafkaCluster.getStatus() == KafkaCluster.ClusterStatus.CONNECTED);
        disconnectItem.setOnAction(ae -> {
            // Remove the selected item from its parent's children
            disconnectKafkaClusterAndSchemaRegistry(kafkaCluster);
        });
        return disconnectItem;
    }

    private void disconnectKafkaClusterAndSchemaRegistry(KafkaCluster cluster) {
//        if (cluster.getStatus() == KafkaCluster.ClusterStatus.CONNECTED) {
        try {
            clusterManager.closeClusterConnection(cluster.getName());
            schemaRegistryManager.disconnectFromSchemaRegistry(cluster.getName());
        } catch (Exception e) {
            log.error("Error when disconnecting cluster", e);
            ModalUtils.showAlertDialog(Alert.AlertType.WARNING, "Error when disconnecting cluster: " + e.getMessage(), "Error when disconnecting cluster", ButtonType.OK);
        }
        cluster.setStatus(KafkaCluster.ClusterStatus.DISCONNECTED);
//        } else {
//            ViewUtils.showAlertDialog(Alert.AlertType.WARNING, "Cluster is not connected", "Cluster is not connected", ButtonType.OK);

//        }
    }


    private MenuItem cloneClusterActionMenuItem(KafkaClusterTreeItem<?> selectedItem) {
        MenuItem cloneItem = new MenuItem("Clone");
        cloneItem.setOnAction(ae -> {
            addNewConnection((KafkaCluster) selectedItem.getValue());
        });
        return cloneItem;
    }

}

