package io.github.nhtuan10.mykafkatool.ui.cluster;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.exception.ClusterNameExistedException;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.manager.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.manager.UserPreferenceManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.cg.ConsumerGroupListTreeItem;
import io.github.nhtuan10.mykafkatool.ui.control.SchemaEditableTableControl;
import io.github.nhtuan10.mykafkatool.ui.partition.KafkaPartitionTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicListTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicTreeItem;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;

import java.io.IOException;
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

    final private SchemaEditableTableControl schemaEditableTableControl;

    final private SchemaRegistryManager schemaRegistryManager;

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
//                    connectToClusterAndSchemaRegistry(cluster, clusterTree);
                    addClusterToTreeView(clusterTree, cluster);
                }
            } catch (ClusterNameExistedException e) {
                log.error("Error when add new connection during loading user preferences", e);
                throw new RuntimeException(e);
            }
        }));

//        TreeView<String> tree = new TreeView<String> (rootItem);

//        clusterTree.setEditable(true);
//        clusterTree.setCellFactory((Callback<TreeView<String>, TreeCell<String>>) p -> new TextFieldTreeCellImpl());
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
            ViewUtil.showPopUpModal("add-topic-modal.fxml", "Add New Topic", modelRef, Map.of(), stage);
            NewTopic newTopic = (NewTopic) modelRef.get();
            if (newTopic != null) {
                CreateTopicsResult result = clusterManager.addTopic(clusterName, newTopic);
                result.all().get();
                topicListTreeItem.reloadChildren();
            }
        }

    }

    public void configureClusterTreeActionMenu() {
        MenuItem addNewConnectionItem = createAddConnectionActionMenuItem();
        MenuItem blankItem = new MenuItem("");
//        blankItem.setDisable(true);
        blankItem.setVisible(false);

        MenuItem deleteTopicItem = createDeleteActionMenuItem();

        MenuItem addNewTopicItem = createAddTopicActionMenuItem();

        MenuItem purgeTopicItem = createPurgeTopicActionMenuItem();

        MenuItem purgePartitionItem = createPurgePartitionActionMenuItem();

        MenuItem deleteConnectionItem = configureDeleteConnectionActionMenuItem();
        MenuItem editConnectionItem = createEditConnectionActionMenuItem();

        ContextMenu clusterTreeContextMenu = new ContextMenu(blankItem);

        clusterTreeContextMenu.setOnShowing(ae -> {
            TreeItem treeItem = (TreeItem) clusterTree.getSelectionModel().getSelectedItem();
            if ((treeItem == null) || (treeItem.getParent() == null && AppConstant.TREE_ITEM_CLUSTERS_DISPLAY_NAME.equalsIgnoreCase((String) treeItem.getValue()))) {
                clusterTreeContextMenu.getItems().setAll(addNewConnectionItem);
            } else if (treeItem.getValue() instanceof KafkaCluster) { // tree item for a connection
//                && treeItem.getParent() != null && treeItem.getParent().getParent() == null) {
                clusterTreeContextMenu.getItems().setAll(editConnectionItem, deleteConnectionItem);
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
                clusterTreeContextMenu.getItems().setAll(refreshItem);
            } else if (AppConstant.TREE_ITEM_SCHEMA_REGISTRY_DISPLAY_NAME.equalsIgnoreCase((String) treeItem.getValue())) {
                MenuItem refreshItem = new MenuItem("Refresh");
                refreshItem.setOnAction(actionEvent -> {
                    try {
                        schemaEditableTableControl.refresh();
                    } catch (RestClientException | IOException | ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
                clusterTreeContextMenu.getItems().setAll(refreshItem);
            } else {
                clusterTreeContextMenu.getItems().setAll(blankItem);
            }
        });
        clusterTree.setContextMenu(clusterTreeContextMenu);
    }

    private MenuItem createPurgePartitionActionMenuItem() {
        MenuItem purgePartitionItem = new MenuItem("Purge Partition");
        purgePartitionItem.setOnAction(ae -> {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaPartitionTreeItem<?> selectedPartitionTreeItem) {
                KafkaPartition partition = (KafkaPartition) selectedPartitionTreeItem.getValue();
                if (ViewUtil.confirmAlert("Purge Partition", "Are you sure to delete all data in the partition " + partition.id() + " ?", "Yes", "Cancel")) {
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

    private MenuItem createPurgeTopicActionMenuItem() {
        MenuItem purgeTopicItem = new MenuItem("Purge Topic");
        purgeTopicItem.setOnAction(ae -> {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedTopicTreeItem) {
                KafkaTopic topic = (KafkaTopic) selectedTopicTreeItem.getValue();
                if (ViewUtil.confirmAlert("Purge Topic", "Are you sure to delete all data in the topic " + topic.name() + " ?", "Yes", "Cancel")) {
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

    private MenuItem createAddTopicActionMenuItem() {
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

    private MenuItem createDeleteActionMenuItem() {
        MenuItem deleteTopicItem = new MenuItem("Delete");
        deleteTopicItem.setOnAction(ae -> {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedTopicTreeItem) {
                KafkaTopic topic = (KafkaTopic) selectedTopicTreeItem.getValue();
                if (ViewUtil.confirmAlert("Delete Topic", "Are you sure to delete " + topic.name() + " ?", "Yes", "Cancel")) {
                    clusterManager.deleteTopic(topic.cluster().getName(), topic.name());
                    selectedTopicTreeItem.getParent().getChildren().remove(selectedTopicTreeItem);
                }
            }
        });
        return deleteTopicItem;
    }

    private MenuItem createAddConnectionActionMenuItem() {
        MenuItem addNewConnectionItem = new MenuItem("Add New Connection");
        addNewConnectionItem.setOnAction(ae -> {
            try {
                KafkaCluster newConnection;
                while (true) {
                    AtomicReference<Object> modelRef = new AtomicReference<>();
                    ViewUtil.showPopUpModal("add-connection-modal.fxml", "Add New Connection", modelRef, Map.of(), stage);
                    newConnection = (KafkaCluster) modelRef.get();

                    if (newConnection != null && (StringUtils.isBlank(newConnection.getName()) || StringUtils.isBlank(newConnection.getBootstrapServer()) || isClusterNameExistedInTree(clusterTree, newConnection.getName()))) {
                        String clusterName = newConnection.getName();
                        log.warn("User enter an invalid cluster name {} or bootstrap server", clusterName);
                        ViewUtil.showAlertDialog(Alert.AlertType.WARNING, "Cluster name " + clusterName + " or bootstrap server is invalid, please try again. Please note that cluster name need to be unique", "Invalid Or Duplicated Connection", ButtonType.OK);
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
        });
        return addNewConnectionItem;
    }

    private MenuItem createEditConnectionActionMenuItem() {
        MenuItem editConnectionItem = new MenuItem("Edit Connection");
        editConnectionItem.setOnAction(ae -> {

            TreeItem<KafkaCluster> selectedItem = (TreeItem<KafkaCluster>) clusterTree.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                KafkaCluster oldConnection = selectedItem.getValue();
                KafkaCluster newConnection;
                try {
                    while (true) {
                        AtomicReference<Object> modelRef = new AtomicReference<>();
                        ViewUtil.showPopUpModal("add-connection-modal.fxml", "Edit Connection", modelRef,
//                                Map.of("clusterNameTextField", oldConnection.getName(),
//                                        "bootstrapServerTextField", oldConnection.getBootstrapServer(),
//                                        "schemaRegistryTextField", oldConnection.getSchemaRegistryUrl() != null ? oldConnection.getSchemaRegistryUrl() : "")
                                Map.of("objectProperty", oldConnection), stage);
                        newConnection = (KafkaCluster) modelRef.get();
                        if (newConnection != null &&
                                (StringUtils.isBlank(newConnection.getName()) || StringUtils.isBlank(newConnection.getBootstrapServer()) || isClusterNameExistedInParentTree(selectedItem, newConnection.getName()))) {
                            String clusterName = newConnection.getName();
                            log.warn("User enter an invalid cluster name {} or bootstrap server", clusterName);
                            ViewUtil.showAlertDialog(Alert.AlertType.WARNING, "Cluster name " + clusterName + " or bootstrap server is invalid, please try again. Please note that cluster name need to be unique", "Invalid Or Duplicated Connection", ButtonType.OK);
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

    private MenuItem configureDeleteConnectionActionMenuItem() {
        MenuItem deleteConnectionItem = new MenuItem("Delete Connection");
        deleteConnectionItem.setOnAction(ae -> {
            TreeItem<KafkaCluster> selectedItem = (TreeItem<KafkaCluster>) clusterTree.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getParent() != null) {
                // Remove the selected item from its parent's children
                deleteConnection(selectedItem);
            }

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

