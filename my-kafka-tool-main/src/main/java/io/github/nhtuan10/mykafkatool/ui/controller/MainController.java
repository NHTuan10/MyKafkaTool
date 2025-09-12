package io.github.nhtuan10.mykafkatool.ui.controller;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.api.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.constant.Theme;
import io.github.nhtuan10.mykafkatool.constant.UIStyleConstant;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.cluster.KafkaClusterTree;
import io.github.nhtuan10.mykafkatool.ui.consumergroup.ConsumerGroupTreeItem;
import io.github.nhtuan10.mykafkatool.ui.consumergroup.ConsumerGroupView;
import io.github.nhtuan10.mykafkatool.ui.event.*;
import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageView;
import io.github.nhtuan10.mykafkatool.ui.schemaregistry.SchemaRegistryView;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaPartitionTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.TopicAndPartitionPropertyView;
import io.github.nhtuan10.mykafkatool.ui.util.ModalUtils;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import io.github.nhtuan10.mykafkatool.userpreference.UserPreference;
import io.github.nhtuan10.mykafkatool.userpreference.UserPreferenceManager;
import jakarta.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private Stage stage;

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

    @FXML
    private TextField clusterTreeSearchTextField;

    public void setStage(Stage stage) {
        this.stage = stage;
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

        this.eventDispatcher.addAppEventSubscriber(kafkaMessageView.getAppReadyEventSubscriber());
        EventSubscriber<SchemaRegistryUIEvent> backgroundSchemaRegistrySub = new EventSubscriber<>() {

            @Override
            protected void handleOnNext(SchemaRegistryUIEvent item) {
                if (SchemaRegistryUIEvent.isBackgroundRefreshEvent(item)) {
                    try {
                        schemaRegistryManager.getAllSubjectMetadata(item.cluster().getName(), false, false);
                    } catch (RestClientException | IOException ex) {
                        log.error("Error when refresh schema registry in background", ex);
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error when refresh schema registry in background", throwable);
            }

            @Override
            public void onComplete() {
                log.info("Refresh schema registry was complete in background");
            }
        };
        this.eventDispatcher.addSchemaRegistryEventSubscriber(backgroundSchemaRegistrySub);

        consumerGroupView.setIsBlockingAppUINeeded(isBlockingAppUINeeded);
        this.eventDispatcher.addConsumerGroupEventSubscriber(consumerGroupView.getConsumerGroupEventSubscriber());

        this.eventDispatcher.addMessageEventSubscriber(kafkaMessageView.getMessageEventSubscriber());

        blockAppProgressInd.visibleProperty().bindBidirectional(isBlockingAppUINeeded);
        this.kafkaClusterTree = new KafkaClusterTree(clusterManager, clusterTree, schemaRegistryManager, eventDispatcher, userPreferenceManager);

        allTabs = Set.of(dataTab, propertiesTab, cgOffsetsTab);

        kafkaClusterTree.configureClusterTreeActionMenu();
        configureClusterTreeSelectedItemChanged();

        this.darkModeMenuItem.textProperty().bind(MyKafkaToolApplication.themeProperty.map(theme -> theme == Theme.DARK ? "✅ " + UIStyleConstant.DARK_MODE : UIStyleConstant.DARK_MODE));
        this.lightModeMenuItem.textProperty().bind(MyKafkaToolApplication.themeProperty.map(theme -> theme == Theme.LIGHT ? "✅ " + UIStyleConstant.LIGHT_MODE : UIStyleConstant.LIGHT_MODE));

        clusterTreeSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            kafkaClusterTree.setTreeItemFilterPredicate(item -> item != null && item.toString().toLowerCase().contains(newValue.toLowerCase()));
        });
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
                eventDispatcher.publishEvent(TopicUIEvent.newRefreshTopicEvent(topic));
//                this.consumerGroupView.loadCG(topic);
                this.eventDispatcher.publishEvent(ConsumerGroupUIEvent.newRefreshConsumerGroupEven(topic.cluster().getName(), null, topic));
                if (!schemaRegistryManager.isSchemaCachedForCluster(topic.cluster().getName())){
                    eventDispatcher.publishEvent(SchemaRegistryUIEvent.newBackgroundRefreshEven(topic.cluster()));
                }
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
                this.eventDispatcher.publishEvent(SchemaRegistryUIEvent.newRefreshEvent(cluster, true));
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
    void addTopic() throws IOException, ExecutionException, InterruptedException {
        kafkaClusterTree.addTopic();
    }

    @FXML
    void addNewConnection() {
        kafkaClusterTree.addNewConnection();
    }

    @FXML
    void exit() {
        MyKafkaToolApplication.exit();
    }

    @FXML
    void switchToDarkMode() {
        if (MyKafkaToolApplication.getCurrentTheme() != Theme.DARK) {
            MyKafkaToolApplication.changeTheme(this.menuBar.getScene(), Theme.DARK);
        }
    }

    @FXML
    void switchToLightMode() {
        MyKafkaToolApplication.changeTheme(this.menuBar.getScene(), Theme.LIGHT);
    }

    @FXML
    void showConfigFileInFileBrowser() throws IOException {
        File file = new File(userPreferenceManager.getUserPrefFilePath());
        openAndSelectFileInFileExplore(file);
    }

    @FXML
    void showLogsInFileBrowser() throws IOException {
        File file = new File(MyKafkaToolApplication.getLogsPath());
        openAndSelectFileInFileExplore(file);
    }

    private void openAndSelectFileInFileExplore(File file) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            Runtime.getRuntime().exec("explorer /select, %s".formatted(file.getAbsolutePath()));
        } else {
            Desktop.getDesktop().browseFileDirectory(file);
        }
    }

    @FXML
    void importConfigFile() {
        try {
            Path path = ViewUtils.openFile("Import Configuration File", AppConstant.USER_PREF_FILENAME, new StageHolder(stage)
                    , new FileChooser.ExtensionFilter("JSON Files", "*.json")
                    , new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            if (path != null) {
                if (ModalUtils.confirmAlert("Importing configuration confirmation", "Do you want to import the new configuration file %s? It will overwrite your existing configuration".formatted(path.getFileName()), "Yes", "No")) {
                    UserPreference pref = userPreferenceManager.saveUserPreference(Files.readString(path));
                    kafkaClusterTree.addAllConnectionsFromUserPreference(pref);
                    MyKafkaToolApplication.applyThemeFromCurrentUserPreference(stage.getScene());
                }
            }
        } catch (IOException e) {
            ModalUtils.showAlertDialog(Alert.AlertType.ERROR, "Importing configuration file error: " + e.getMessage(), "Error", ButtonType.OK);
        }
    }

    @FXML
    protected void aboutDialog() throws IOException {
        Alert about = ModalUtils.buildHelpDialog("about.txt", "About MyKafkaTool");
        about.showAndWait();

    }
}