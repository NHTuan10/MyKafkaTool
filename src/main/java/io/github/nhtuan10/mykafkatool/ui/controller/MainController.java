package io.github.nhtuan10.mykafkatool.ui.controller;

import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.manager.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.cg.ConsumerGroupTreeItem;
import io.github.nhtuan10.mykafkatool.ui.cluster.KafkaClusterTree;
import io.github.nhtuan10.mykafkatool.ui.control.ConsumerGroupTable;
import io.github.nhtuan10.mykafkatool.ui.control.MessageView;
import io.github.nhtuan10.mykafkatool.ui.control.SchemaRegistryControl;
import io.github.nhtuan10.mykafkatool.ui.control.TopicOrPartitionPropertyView;
import io.github.nhtuan10.mykafkatool.ui.partition.KafkaPartitionTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicTreeItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

// TODO: refactor this class

@Slf4j
public class MainController {
    private final ClusterManager clusterManager = ClusterManager.getInstance();

    private KafkaClusterTree kafkaClusterTree;

    private final BooleanProperty isBlockingAppUINeeded = new SimpleBooleanProperty(false);

    @FXML
    private TreeView clusterTree;

    @FXML
    private ProgressIndicator blockAppProgressInd;

    // Data Tab
    @FXML
    private Tab dataTab;

    @FXML
    private MessageView messageView;

    // Consumer Groups
    @FXML
    private ConsumerGroupTable consumerGroupOffsetTable;

    @FXML
    private Tab cgOffsetsTab;

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab propertiesTab;

    @FXML
    private TopicOrPartitionPropertyView topicOrPartitionPropertyView;

    @FXML
    private SchemaRegistryControl schemaRegistryControl;

    public void setStage(Stage stage) {
        this.kafkaClusterTree.setStage(stage);
        this.schemaRegistryControl.setStage(stage);
        this.topicOrPartitionPropertyView.setStage(stage);
    }

    public MainController() {
    }

    @FXML
    public void initialize() {

        blockAppProgressInd.visibleProperty().bindBidirectional(isBlockingAppUINeeded);
        this.kafkaClusterTree = new KafkaClusterTree(clusterManager, clusterTree, schemaRegistryControl, SchemaRegistryManager.getInstance());
        kafkaClusterTree.configureClusterTreeActionMenu();
        configureClusterTreeSelectedItemChanged();
    }

    private void configureClusterTreeSelectedItemChanged() {
        clusterTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null && oldValue != newValue && (oldValue instanceof KafkaTopicTreeItem<?> || oldValue instanceof KafkaPartitionTreeItem<?>)) {
                messageView.cacheMessages((TreeItem) oldValue);
            }
            if (newValue instanceof KafkaTopicTreeItem<?> selectedItem) {

                messageView.switchTopicOrPartition((TreeItem) newValue);
                KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
                // Enable the data tab and show/hide titled panes in the tab
                tabPane.getTabs().remove(cgOffsetsTab);
                if (!tabPane.getTabs().contains(dataTab)) {
                    tabPane.getTabs().add(dataTab);
                }
                if (!tabPane.getTabs().contains(propertiesTab)) {
                    tabPane.getTabs().add(propertiesTab);
                }
                if (tabPane.getSelectionModel().getSelectedItem() != dataTab && tabPane.getSelectionModel().getSelectedItem() != propertiesTab) {
                    tabPane.getSelectionModel().select(dataTab);
                }
                dataTab.setDisable(false);
                propertiesTab.setDisable(false);
                schemaRegistryControl.setVisible(false);
                messageView.setVisible(true);

                this.topicOrPartitionPropertyView.loadTopicConfig(topic, isBlockingAppUINeeded);
                this.topicOrPartitionPropertyView.loadTopicPartitions(topic, this.isBlockingAppUINeeded);
            } else if (newValue instanceof KafkaPartitionTreeItem<?> selectedItem) {
                messageView.switchTopicOrPartition((TreeItem) newValue);
                tabPane.getTabs().remove(cgOffsetsTab);
                if (!tabPane.getTabs().contains(dataTab)) {
                    tabPane.getTabs().add(dataTab);
                }
                if (!tabPane.getTabs().contains(propertiesTab)) {
                    tabPane.getTabs().add(propertiesTab);
                }
                if (tabPane.getSelectionModel().getSelectedItem() != dataTab && tabPane.getSelectionModel().getSelectedItem() != propertiesTab) {
                    tabPane.getSelectionModel().select(dataTab);
                }
                dataTab.setDisable(false);
                propertiesTab.setDisable(false);
                schemaRegistryControl.setVisible(false);
                messageView.setVisible(true);
                KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
                this.topicOrPartitionPropertyView.loadPartitionConfig(partition, isBlockingAppUINeeded);
            } else if (newValue instanceof ConsumerGroupTreeItem selected) {
                if (!tabPane.getTabs().contains(cgOffsetsTab)) {
                    tabPane.getTabs().add(cgOffsetsTab);
                }
                cgOffsetsTab.setDisable(false);
                tabPane.getTabs().remove(dataTab);
                tabPane.getTabs().remove(propertiesTab);
                tabPane.getSelectionModel().select(cgOffsetsTab);
                blockAppProgressInd.setVisible(true);
                this.consumerGroupOffsetTable.loadCG(selected, this.isBlockingAppUINeeded);
            } else if (newValue instanceof TreeItem<?> selectedItem && AppConstant.TREE_ITEM_SCHEMA_REGISTRY_DISPLAY_NAME.equals(selectedItem.getValue())) {
//                blockAppProgressInd.setVisible(true);
                if (!tabPane.getTabs().contains(dataTab)) {
                    tabPane.getTabs().add(dataTab);
                }
                tabPane.getSelectionModel().select(dataTab);
                tabPane.getTabs().remove(cgOffsetsTab);
                tabPane.getTabs().remove(propertiesTab);
                dataTab.setDisable(false);
                schemaRegistryControl.setVisible(true);
                messageView.setVisible(false);
                KafkaCluster cluster = (KafkaCluster) selectedItem.getParent().getValue();
                try {
                    schemaRegistryControl.loadAllSchema(cluster, isBlockingAppUINeeded);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

            } else {
                cgOffsetsTab.setDisable(true);
                dataTab.setDisable(true);
                propertiesTab.setDisable(true);
            }
        });
    }

    @FXML
    protected void addTopic() throws IOException, ExecutionException, InterruptedException {
        kafkaClusterTree.addTopic();
    }

}