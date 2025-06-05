package io.github.nhtuan10.mykafkatool.ui.controller;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.constant.Theme;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.manager.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.cg.ConsumerGroupTreeItem;
import io.github.nhtuan10.mykafkatool.ui.cluster.KafkaClusterTree;
import io.github.nhtuan10.mykafkatool.ui.control.ConsumerGroupTable;
import io.github.nhtuan10.mykafkatool.ui.control.KafkaMessageView;
import io.github.nhtuan10.mykafkatool.ui.control.SchemaRegistryControl;
import io.github.nhtuan10.mykafkatool.ui.control.TopicAndPartitionPropertyView;
import io.github.nhtuan10.mykafkatool.ui.event.*;
import io.github.nhtuan10.mykafkatool.ui.partition.KafkaPartitionTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicTreeItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SubmissionPublisher;

// TODO: refactor this class

@Slf4j
public class MainController {
    public static final int SUBSCRIBER_MAX_BUFFER_CAPACITY = 1000;
    private final ClusterManager clusterManager;
    private KafkaClusterTree kafkaClusterTree;
    private final EventDispatcher eventDispatcher;

    private final BooleanProperty isBlockingAppUINeeded = new SimpleBooleanProperty(false);

    @FXML
    private TreeView clusterTree;

    @FXML
    private ProgressIndicator blockAppProgressInd;

    // Data Tab
    @FXML
    private Tab dataTab;

    @FXML
    private KafkaMessageView kafkaMessageView;

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
    private TopicAndPartitionPropertyView topicAndPartitionPropertyView;

    @FXML
    private SchemaRegistryControl schemaRegistryControl;

    @FXML
    private MenuBar menuBar;

    private Set<Tab> allTabs;

    public void setStage(Stage stage) {
        this.kafkaClusterTree.setStage(stage);
        this.schemaRegistryControl.setStage(stage);
        this.topicAndPartitionPropertyView.setStage(stage);
    }

    public MainController() {
        this.clusterManager = ClusterManager.getInstance();
        this.eventDispatcher = new EventDispatcher(new SubmissionPublisher<>()
                , new SubmissionPublisher<>(), new SubmissionPublisher<>());
    }

    @FXML
    public void initialize() {
        topicAndPartitionPropertyView.setProperties(isBlockingAppUINeeded, this.propertiesTab.selectedProperty());
        this.eventDispatcher.addTopicEventSubscriber(topicAndPartitionPropertyView.getTopicEventSubscriber());
        this.eventDispatcher.addTopicEventSubscriber(kafkaMessageView.getTopicEventSubscriber());

        this.eventDispatcher.addPartitionEventSubscriber(topicAndPartitionPropertyView.getPartitionEventSubscriber());
        this.eventDispatcher.addPartitionEventSubscriber(kafkaMessageView.getPartitionEventSubscriber());

        schemaRegistryControl.setIsBlockingAppUINeeded(isBlockingAppUINeeded);
        this.eventDispatcher.addSchemaRegistryEventSubscriber(schemaRegistryControl.getSchemaRegistryEventSubscriber());

        blockAppProgressInd.visibleProperty().bindBidirectional(isBlockingAppUINeeded);
        this.kafkaClusterTree = new KafkaClusterTree(clusterManager, clusterTree, SchemaRegistryManager.getInstance(), eventDispatcher);

        allTabs = Set.of(dataTab, propertiesTab, cgOffsetsTab);

        kafkaClusterTree.configureClusterTreeActionMenu();
        configureClusterTreeSelectedItemChanged();
    }

    private void configureClusterTreeSelectedItemChanged() {
        clusterTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null && oldValue != newValue && (oldValue instanceof KafkaTopicTreeItem<?> || oldValue instanceof KafkaPartitionTreeItem<?>)) {
                kafkaMessageView.cacheMessages((TreeItem) oldValue);
            }
            if (newValue instanceof KafkaTopicTreeItem<?> selectedItem) {

                kafkaMessageView.switchTopicOrPartition((TreeItem) newValue);
                KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
                // Enable the data tab and show/hide titled panes in the tab
                setVisibleTabs(dataTab, propertiesTab);
                schemaRegistryControl.setVisible(false);
                kafkaMessageView.setVisible(true);
                eventDispatcher.publishEvent(TopicUIEvent.newRefreshTopicEven(topic));
            } else if (newValue instanceof KafkaPartitionTreeItem<?> selectedItem) {
                kafkaMessageView.switchTopicOrPartition((TreeItem) newValue);
                setVisibleTabs(dataTab, propertiesTab);
                schemaRegistryControl.setVisible(false);
                kafkaMessageView.setVisible(true);
                KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
//                this.topicAndPartitionPropertyView.loadPartitionConfig(partition);
                eventDispatcher.publishEvent(PartitionUIEvent.newRefreshPartitionEven(partition));
            } else if (newValue instanceof ConsumerGroupTreeItem selected) {
                setVisibleTabs(cgOffsetsTab);
                this.consumerGroupOffsetTable.loadCG(selected, this.isBlockingAppUINeeded);
            } else if (newValue instanceof TreeItem<?> selectedItem && AppConstant.TREE_ITEM_SCHEMA_REGISTRY_DISPLAY_NAME.equals(selectedItem.getValue())) {
                setVisibleTabs(dataTab);
                schemaRegistryControl.setVisible(true);
                kafkaMessageView.setVisible(false);
                KafkaCluster cluster = (KafkaCluster) selectedItem.getParent().getValue();
                this.eventDispatcher.publishEvent(new SchemaRegistryUIEvent(cluster, UIEvent.Action.REFRESH_SCHEMA_REGISTRY));
//                try {
//                    schemaRegistryControl.loadAllSchema(cluster, isBlockingAppUINeeded);

//                } catch (ExecutionException | InterruptedException e) {
//                    throw new RuntimeException(e);
//                }

            } else {
                allTabs.forEach(tab -> tab.setDisable(true));
            }
        });
    }

    private void setVisibleTabs(Tab... tabs) {
        for (Tab tab : tabs) {
            if (!tabPane.getTabs().contains(tab)) {
                tabPane.getTabs().add(tab);
            }
            tab.setDisable(false);
        }
        if (Arrays.stream(tabs).noneMatch(t -> t == tabPane.getSelectionModel().getSelectedItem())) {
            tabPane.getSelectionModel().select(tabs[0]);
        }
        allTabs.stream().filter(tab -> !Arrays.asList(tabs).contains(tab)).forEach(tab -> tabPane.getTabs().remove(tab));
//        tabPane.getTabs().remove(cgOffsetsTab);
//        if (!tabPane.getTabs().contains(dataTab)) {
//            tabPane.getTabs().add(dataTab);
//        }
//        if (!tabPane.getTabs().contains(propertiesTab)) {
//            tabPane.getTabs().add(propertiesTab);
//        }

//        dataTab.setDisable(false);
//        propertiesTab.setDisable(false);
    }

    @FXML
    protected void addTopic() throws IOException, ExecutionException, InterruptedException {
        kafkaClusterTree.addTopic();
    }

    @FXML
    protected void addNewConnection() {
        kafkaClusterTree.addNewConnection();
    }

    @FXML
    protected void exit() {
        MyKafkaToolApplication.exit();
    }

    @FXML
    protected void switchToDarkMode() {
        MyKafkaToolApplication.changeTheme(this.menuBar.getScene(), Theme.DARK);
    }

    @FXML
    protected void switchToLightMode() {
        MyKafkaToolApplication.changeTheme(this.menuBar.getScene(), Theme.LIGHT);
    }

}