package io.github.nhtuan10.mykafkatool.ui.controller;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.constant.Theme;
import io.github.nhtuan10.mykafkatool.constant.UIStyleConstant;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.manager.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.cluster.KafkaClusterTree;
import io.github.nhtuan10.mykafkatool.ui.consumergroup.ConsumerGroupTreeItem;
import io.github.nhtuan10.mykafkatool.ui.consumergroup.ConsumerGroupView;
import io.github.nhtuan10.mykafkatool.ui.event.*;
import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageView;
import io.github.nhtuan10.mykafkatool.ui.schemaregistry.SchemaRegistryView;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaPartitionTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.TopicAndPartitionPropertyView;
import io.github.nhtuan10.mykafkatool.userpreference.UserPreferenceManager;
import jakarta.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MainController {
    //    public static final int SUBSCRIBER_MAX_BUFFER_CAPACITY = 1000;
    private final ClusterManager clusterManager;
    private final EventDispatcher eventDispatcher;
    private final UserPreferenceManager userPreferenceManager;
    private final SchemaRegistryManager schemaRegistryManager;
    private final BooleanProperty isBlockingAppUINeeded = new SimpleBooleanProperty(false);
    private KafkaClusterTree kafkaClusterTree;
    private Set<Tab> allTabs;

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
//    @FXML
//    private ConsumerGroupIDTable consumerGroupOffsetTable;

    @FXML
    private ConsumerGroupView consumerGroupView;

    @FXML
    private Tab cgOffsetsTab;

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab propertiesTab;

    @FXML
    private TopicAndPartitionPropertyView topicAndPartitionPropertyView;

    @FXML
    private SchemaRegistryView schemaRegistryView;

    @FXML
    private MenuBar menuBar;

    @FXML
    private MenuItem darkModeMenuItem;

    @FXML
    private MenuItem lightModeMenuItem;

    public void setStage(Stage stage) {
        this.kafkaClusterTree.setStage(stage);
        this.schemaRegistryView.setStage(stage);
        this.topicAndPartitionPropertyView.setStage(stage);
    }

//    @Inject
//    public MainController(ClusterManager clusterManager, EventDispatcher eventDispatcher) {
////        this.clusterManager = ClusterManager.getInstance();
//        this.clusterManager = clusterManager;
//        this.eventDispatcher = eventDispatcher;

    /// /        this.eventDispatcher = new EventDispatcher(new SubmissionPublisher<>()
    /// /                , new SubmissionPublisher<>(), new SubmissionPublisher<>());
//    }

    @FXML
    public void initialize() {
        topicAndPartitionPropertyView.setProperties(isBlockingAppUINeeded, this.propertiesTab.selectedProperty());
        this.eventDispatcher.addTopicEventSubscriber(topicAndPartitionPropertyView.getTopicEventSubscriber());
        this.eventDispatcher.addTopicEventSubscriber(kafkaMessageView.getTopicEventSubscriber());

        this.eventDispatcher.addPartitionEventSubscriber(topicAndPartitionPropertyView.getPartitionEventSubscriber());
        this.eventDispatcher.addPartitionEventSubscriber(kafkaMessageView.getPartitionEventSubscriber());

        schemaRegistryView.setIsBlockingAppUINeeded(isBlockingAppUINeeded);
        this.eventDispatcher.addSchemaRegistryEventSubscriber(schemaRegistryView.getSchemaRegistryEventSubscriber());

        consumerGroupView.setIsBlockingAppUINeeded(isBlockingAppUINeeded);
        this.eventDispatcher.addConsumerGroupEventSubscriber(consumerGroupView.getConsumerGroupEventSubscriber());
        blockAppProgressInd.visibleProperty().bindBidirectional(isBlockingAppUINeeded);
        this.kafkaClusterTree = new KafkaClusterTree(clusterManager, clusterTree, schemaRegistryManager, eventDispatcher, userPreferenceManager);

        allTabs = Set.of(dataTab, propertiesTab, cgOffsetsTab);

        kafkaClusterTree.configureClusterTreeActionMenu();
        configureClusterTreeSelectedItemChanged();

        this.darkModeMenuItem.textProperty().bind(MyKafkaToolApplication.themeProperty.map(theme -> theme == Theme.DARK ? "✅ " + UIStyleConstant.DARK_MODE : UIStyleConstant.DARK_MODE));
        this.lightModeMenuItem.textProperty().bind(MyKafkaToolApplication.themeProperty.map(theme -> theme == Theme.LIGHT ? "✅ " + UIStyleConstant.LIGHT_MODE : UIStyleConstant.LIGHT_MODE));

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
                setVisibleTabs(dataTab, propertiesTab, cgOffsetsTab);
                schemaRegistryView.setVisible(false);
                kafkaMessageView.setVisible(true);
                eventDispatcher.publishEvent(TopicUIEvent.newRefreshTopicEven(topic));
//                this.consumerGroupView.loadCG(topic);
                this.eventDispatcher.publishEvent(ConsumerGroupUIEvent.newRefreshConsumerGroupEven(topic.cluster().getName(), null, topic));
            } else if (newValue instanceof KafkaPartitionTreeItem<?> selectedItem) {
                kafkaMessageView.switchTopicOrPartition((TreeItem) newValue);
                setVisibleTabs(dataTab, propertiesTab);
                schemaRegistryView.setVisible(false);
                kafkaMessageView.setVisible(true);
                KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
//                this.topicAndPartitionPropertyView.loadPartitionConfig(partition);
                eventDispatcher.publishEvent(PartitionUIEvent.newRefreshPartitionEven(partition));
            } else if (newValue instanceof ConsumerGroupTreeItem selected) {
                setVisibleTabs(cgOffsetsTab);
//                this.consumerGroupView.loadCG(selected.getClusterName(), List.of(selected.getConsumerGroupId()));
                this.eventDispatcher.publishEvent(ConsumerGroupUIEvent.newRefreshConsumerGroupEven(selected.getClusterName(), List.of(selected.getConsumerGroupId()), null));
            } else if (newValue instanceof TreeItem<?> selectedItem && AppConstant.SCHEMA_REGISTRY_TREE_ITEM_DISPLAY_NAME.equals(selectedItem.getValue())) {
                setVisibleTabs(dataTab);
                schemaRegistryView.setVisible(true);
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
//        for (Tab tab : tabs) {
//            if (!tabPane.getTabs().contains(tab)) {
//                tabPane.getTabs().add(tab);
//            }
//            tab.setDisable(false);
//        }
        Arrays.stream(tabs).forEach(t -> t.setDisable(false));
        tabPane.getTabs().setAll(tabs);
        Tab currentSelectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (Arrays.stream(tabs).noneMatch(t -> t == currentSelectedTab)) {
            tabPane.getSelectionModel().select(tabs[0]);
        }
//        allTabs.stream().filter(tab -> !Arrays.asList(tabs).contains(tab)).forEach(tab -> tabPane.getTabs().remove(tab));
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
        if (MyKafkaToolApplication.getCurrentTheme() != Theme.DARK) {
            MyKafkaToolApplication.changeTheme(this.menuBar.getScene(), Theme.DARK);
        }
    }

    @FXML
    protected void switchToLightMode() {
        MyKafkaToolApplication.changeTheme(this.menuBar.getScene(), Theme.LIGHT);
    }

    @FXML
    protected void showConfigFileInFileBrowser() {
        Desktop.getDesktop().browseFileDirectory(new File(userPreferenceManager.getUserPrefFilePath()));
    }

    @FXML
    protected void showLogsInFileBrowser() {
        Desktop.getDesktop().browseFileDirectory(new File(MyKafkaToolApplication.getLogsPath()));
    }

}