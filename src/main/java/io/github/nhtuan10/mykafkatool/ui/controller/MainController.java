package io.github.nhtuan10.mykafkatool.ui.controller;

import com.google.common.collect.ImmutableMap;
import io.github.nhtuan10.mykafkatool.api.PluggableDeserializer;
import io.github.nhtuan10.mykafkatool.api.model.KafkaMessage;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.consumer.KafkaConsumerService;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.manager.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.producer.ProducerUtil;
import io.github.nhtuan10.mykafkatool.serdes.AvroUtil;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import io.github.nhtuan10.mykafkatool.serdes.deserializer.ByteArrayDeserializer;
import io.github.nhtuan10.mykafkatool.serdes.deserializer.SchemaRegistryAvroDeserializer;
import io.github.nhtuan10.mykafkatool.serdes.deserializer.StringDeserializer;
import io.github.nhtuan10.mykafkatool.serdes.serializer.ByteArraySerializer;
import io.github.nhtuan10.mykafkatool.serdes.serializer.SchemaRegistryAvroSerializer;
import io.github.nhtuan10.mykafkatool.serdes.serializer.StringSerializer;
import io.github.nhtuan10.mykafkatool.ui.*;
import io.github.nhtuan10.mykafkatool.ui.cg.ConsumerGroupOffsetTableItem;
import io.github.nhtuan10.mykafkatool.ui.cg.ConsumerGroupTreeItem;
import io.github.nhtuan10.mykafkatool.ui.cluster.KafkaClusterTree;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.control.DateTimePicker;
import io.github.nhtuan10.mykafkatool.ui.control.SchemaEditableTableControl;
import io.github.nhtuan10.mykafkatool.ui.partition.KafkaPartitionTreeItem;
import io.github.nhtuan10.mykafkatool.ui.partition.KafkaPartitionsTableItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicTreeItem;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.fxmisc.richtext.CodeArea;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static io.github.nhtuan10.mykafkatool.constant.AppConstant.DEFAULT_MAX_POLL_RECORDS;
import static io.github.nhtuan10.mykafkatool.constant.AppConstant.DEFAULT_POLL_TIME_MS;

@Slf4j
public class MainController {
    private final ClusterManager clusterManager = ClusterManager.getInstance();

    private final KafkaConsumerService kafkaConsumerService;

    private final ProducerUtil producerUtil;

    private final SerDesHelper serDesHelper;

    private final JsonHighlighter jsonHighlighter;

    private KafkaClusterTree kafkaClusterTree;

    private final Map<TreeItem, MessageTableState> treeMsgTableItemCache = new ConcurrentHashMap<>();

    private final BooleanProperty isPolling = new SimpleBooleanProperty(false);

    private final BooleanProperty isBlockingAppUINeeded = new SimpleBooleanProperty(false);
//    private final SimpleLongProperty totalMessagesInTheTopicProperty = new SimpleLongProperty(0);

    private final SimpleStringProperty totalMessagesInTheTopicStringProperty = new SimpleStringProperty("0 Messages");
    @FXML
    private TreeView clusterTree;

    @FXML
    private ProgressIndicator blockAppProgressInd;

    // Data Tab
    @FXML
    private Tab dataTab;

    @FXML
    private TableView<KafkaMessageTableItem> messageTable;

    private ObservableList<KafkaMessageTableItem> allMsgTableItems = FXCollections.observableArrayList();

    // Poll Options
    @FXML
    private TextField pollTimeTextField;

    @FXML
    private Label noMessagesLabel;

    @FXML
    private SimpleLongProperty noMsgLongProp = new SimpleLongProperty();

    @FXML
    private Label totalMessagesInTheTopicLabel;

    @FXML
    private TextField maxMessagesTextField;

    @FXML
    private DateTimePicker startTimestampPicker;

    @FXML
    private ComboBox<String> keyContentType;

    @FXML
    private ComboBox<String> valueContentType;

    @FXML
    private ComboBox<KafkaConsumerService.MessagePollingPosition> msgPosition;

    @FXML
    private CodeArea schemaTextArea;

    @FXML
    private CheckBox isLiveUpdateCheckBox;

    @FXML
    private TextField filterMsgTextField;

    private StringProperty filterMsgTextProperty = new SimpleStringProperty("");

    @FXML
    private ToggleButton regexFilterToggleBtn;

    @FXML
    private Label endTimestampLabel;

    @FXML
    private DateTimePicker endTimestampPicker;

    // message buttons
    @FXML
    private Button countMessagesBtn;

    @FXML
    private Button pullMessagesBtn;

    @FXML
    private ProgressIndicator isPollingMsgProgressIndicator;

    @FXML
    private SplitPane schemaSplitPane;

    @FXML
    private SplitPane messageSplitPane;

    @FXML
    private SchemaEditableTableControl schemaEditableTableControl;

    @FXML
    private CodeArea schemaRegistryTextArea;

    // Consumer Groups
    @FXML
    private TableView<ConsumerGroupOffsetTableItem> consumerGroupOffsetTable;

    @FXML
    private Tab cgOffsetsTab;

    // Topic/Partition properties
    @FXML
    private TableView<UIPropertyTableItem> topicConfigTable;

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab propertiesTab;

    @FXML
    private TableView<KafkaPartitionsTableItem> kafkaPartitionsTable;

    @FXML
    private TitledPane partitionsTitledPane;

    public MainController() {
        StringSerializer stringSerializer = new StringSerializer();
        StringDeserializer stringDeserializer = new StringDeserializer();
        ByteArraySerializer byteArraySerializer = new ByteArraySerializer();
        ByteArrayDeserializer byteArrayDeserializer = new ByteArrayDeserializer();
        SchemaRegistryAvroSerializer schemaRegistryAvroSerializer = new SchemaRegistryAvroSerializer();
        SchemaRegistryAvroDeserializer schemaRegistryAvroDeserializer = new SchemaRegistryAvroDeserializer();
        this.serDesHelper = new SerDesHelper(
                ImmutableMap.of(stringSerializer.getName(), stringSerializer,
                        byteArraySerializer.getName(), byteArraySerializer,
                        schemaRegistryAvroSerializer.getName(), schemaRegistryAvroSerializer),
                ImmutableMap.of(stringDeserializer.getName(), stringDeserializer,
                        byteArrayDeserializer.getName(), byteArrayDeserializer,
                        schemaRegistryAvroDeserializer.getName(), schemaRegistryAvroDeserializer
                )
        );
        this.producerUtil = new ProducerUtil(this.serDesHelper);
        this.kafkaConsumerService = new KafkaConsumerService(this.serDesHelper);
        this.jsonHighlighter = new JsonHighlighter();
    }

    @FXML
    public void initialize() {

        blockAppProgressInd.visibleProperty().bindBidirectional(isBlockingAppUINeeded);
        partitionsTitledPane.setVisible(false);
        initPollingOptionsUI();
        this.filterMsgTextField.textProperty().bindBidirectional(filterMsgTextProperty);
        this.kafkaClusterTree = new KafkaClusterTree(clusterManager, clusterTree, schemaEditableTableControl, SchemaRegistryManager.getInstance());
        kafkaClusterTree.configureClusterTreeActionMenu();
        configureClusterTreeSelectedItemChanged();
        configureTableView();
        schemaRegistryTextArea.textProperty().addListener((obs, oldText, newText) -> {
            ViewUtil.highlightJsonInCodeArea(newText, schemaRegistryTextArea, true, AvroUtil.OBJECT_MAPPER, jsonHighlighter);
        });
        totalMessagesInTheTopicLabel.textProperty().bind(totalMessagesInTheTopicStringProperty
//                totalMessagesInTheTopicProperty.asString("%,d Messages")
        );
        isPollingMsgProgressIndicator.visibleProperty().bindBidirectional(isPolling);
        isPollingMsgProgressIndicator.managedProperty().bindBidirectional(isPolling);
//        pullMessagesBtn.textProperty().bind(isPolling.map((isPolling) ->
//                isPolling ? AppConstant.STOP_POLLING_TEXT : AppConstant.POLL_MESSAGES_TEXT));
        isPolling.addListener((observable, oldValue, newValue) -> {
            pullMessagesBtn.setText(newValue ? AppConstant.STOP_POLLING_TEXT : AppConstant.POLL_MESSAGES_TEXT);
        });
        noMessagesLabel.textProperty().bind(noMsgLongProp.asString().concat(" Messages"));
    }

    private void configureTableView() {
        configureMessageTable(messageTable, serDesHelper);
        TableViewConfigurer.configureTableView(ConsumerGroupOffsetTableItem.class, consumerGroupOffsetTable);
        TableViewConfigurer.configureTableView(KafkaPartitionsTableItem.class, kafkaPartitionsTable);
        TableViewConfigurer.configureTableView(UIPropertyTableItem.class, topicConfigTable);
        schemaEditableTableControl.addEventHandler(SchemaEditableTableControl.SelectedSchemaEvent.SELECTED_SCHEMA_EVENT_TYPE,
                (event) -> schemaRegistryTextArea.replaceText(event.getData().getValue()));
        // Use a change listener to respond to a selection within
        // a tree view
//        clusterTree.getSelectionModel().selectedItemProperty().addListener((ChangeListener<TreeItem<String>>) (changed, oldVal, newVal) -> {
//
//
//        });


//        TreeView<String> tree = new TreeView<String> (rootItem);

//        clusterTree.setEditable(true);
//        clusterTree.setCellFactory((Callback<TreeView<String>, TreeCell<String>>) p -> new TextFieldTreeCellImpl());
    }

    public void configureMessageTable(TableView<KafkaMessageTableItem> messageTable, SerDesHelper serDesHelper) {
        TableViewConfigurer.configureTableView(KafkaMessageTableItem.class, messageTable, false);
        messageTable.setRowFactory(tv -> {
            TableRow<KafkaMessageTableItem> row = new TableRow<>() {
                @Override
                protected void updateItem(KafkaMessageTableItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty && item != null) {
                        String color;
                        if (isSelected()) {
                            color = item.isErrorItem() ? "#C06666" : "lightgray";
                        } else {
                            color = item.isErrorItem() ? "lightcoral" : "transparent";
                        }
                        setStyle("-fx-background-color: %s; -fx-border-color: transparent transparent lightgray transparent;".formatted(color));

//                        if (!isSelected()) {
//                            if (item.isErrorItem()) {
//                                setStyle("-fx-background-color: %s; -fx-border-color: transparent transparent lightgray transparent;".formatted(color));
//                            } else {
//                                setStyle("-fx-background-color: %s; -fx-border-color: transparent transparent lightgray transparent;".formatted(color));
//                            }
//                        }
                    } else {
                        setStyle("");
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    KafkaMessageTableItem rowData = row.getItem();
                    log.debug("Double click on: {}", rowData.getKey());
                    Map<String, Object> msgModalFieldMap = Map.of(
                            "valueContentType", rowData.getValueContentType(),
                            "serDesHelper", serDesHelper,
                            "keyTextArea", rowData.getKey(),
                            "valueTextArea", rowData.getValue(),
                            "valueContentTypeComboBox", FXCollections.observableArrayList(rowData.getValueContentType()),
                            "headerTable",
                            FXCollections.observableArrayList(
                                    Arrays.stream(rowData.getHeaders().toArray()).map(header -> new UIPropertyTableItem(header.key(), new String(header.value()))).toList()));
                    try {
                        ViewUtil.showPopUpModal(AppConstant.ADD_MESSAGE_MODAL_FXML, "View Message", new AtomicReference<>(), msgModalFieldMap, false, true);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

//                    System.out.println("Double click on: "+rowData.getKey());
                }
            });
            return row;
        });
        messageTable.itemsProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> noMsgLongProp.set(newValue.size()));
        });
        allMsgTableItems.addListener((ListChangeListener<KafkaMessageTableItem>) change -> {
            Platform.runLater(() -> noMsgLongProp.set(messageTable.getItems().size()));
        });
        messageTable.getItems().addListener((ListChangeListener<KafkaMessageTableItem>) change -> {
            Platform.runLater(() -> noMsgLongProp.set(messageTable.getItems().size()));
        });
//        configureErrorMessageRow((TableColumn<KafkaMessageTableItem, Object>) messageTable.getColumns().get(3));
        messageTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            log.info("Selected item in msg table: {}", newValue);
        });
    }

    private void initPollingOptionsUI() {
        pollTimeTextField.setText(String.valueOf(DEFAULT_POLL_TIME_MS));
        maxMessagesTextField.setText(String.valueOf(DEFAULT_MAX_POLL_RECORDS));
        startTimestampPicker.setDayCellFactory(param -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
        keyContentType.setItems(FXCollections.observableArrayList(serDesHelper.getSupportedKeyDeserializer()));
        keyContentType.getSelectionModel().selectFirst();
//        valueContentType.setItems(SerdeUtil.SUPPORT_VALUE_CONTENT_TYPES);
        valueContentType.setItems(FXCollections.observableArrayList(serDesHelper.getSupportedValueDeserializer()));
//        valueContentType.setValue(SerdeUtil.SERDE_STRING);
        valueContentType.getSelectionModel().selectFirst();
        msgPosition.setItems(FXCollections.observableArrayList(KafkaConsumerService.MessagePollingPosition.values()));
        msgPosition.setValue(KafkaConsumerService.MessagePollingPosition.LAST);
        valueContentType.setOnAction(event -> {
            PluggableDeserializer deserializer = serDesHelper.getPluggableDeserialize(valueContentType.getValue());
            schemaTextArea.setDisable(!deserializer.mayNeedUserInputForSchema());
            isPolling.set(false);
        });
        schemaTextArea.setDisable(!serDesHelper.getPluggableDeserialize(valueContentType.getValue()).mayNeedUserInputForSchema());
        schemaTextArea.textProperty().addListener((obs, oldText, newText) -> {
            ViewUtil.highlightJsonInCodeArea(newText, schemaTextArea, false, AvroUtil.OBJECT_MAPPER, jsonHighlighter);
//                    if (valueDisplayTypeComboBox.getValue() == DisplayType.JSON) {
////                       && !newText.equals(oldText)){
//                        textArea.setStyleSpans(0, json.highlight(newText));
//                    } else if (valueDisplayTypeComboBox.getValue() == DisplayType.TEXT) {
//                        textArea.clearStyle(0, newText.length() - 1);
//                    }
        });
        isLiveUpdateCheckBox.setOnAction(event -> {
            if (!isLiveUpdateCheckBox.isSelected() && isPolling.get()) {
                isPolling.set(false);
//                pullMessagesBtn.setText(AppConstant.POLL_MESSAGES_TEXT);
            }
        });
        isLiveUpdateCheckBox.disableProperty()
                .bind(msgPosition.valueProperty().map(
                        v -> v != KafkaConsumerService.MessagePollingPosition.LAST));
        endTimestampLabel.setVisible(false);
        endTimestampLabel.setManaged(false);
        endTimestampPicker.setVisible(false);
        endTimestampPicker.setManaged(false);
    }

    private void configureClusterTreeSelectedItemChanged() {
        clusterTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Display the selection and its complete path from the root.
            if (newValue != null && newValue != oldValue) {
                // disable/hide UI tab and titled
//                cgOffsetsTab.setDisable(true);
//                dataTab.setDisable(true);
//                propertiesTab.setDisable(true);

                partitionsTitledPane.setVisible(false);
                if (oldValue != null && oldValue != newValue && (oldValue instanceof KafkaTopicTreeItem<?> || newValue instanceof KafkaPartitionTreeItem<?>)) {
                    TreeItem oldSelectedTreeItem = (TreeItem) oldValue;
                    treeMsgTableItemCache.put(oldSelectedTreeItem, MessageTableState.builder()
                            .items(FXCollections.observableArrayList(allMsgTableItems))
                            .filter(new Filter(filterMsgTextProperty.get(), regexFilterToggleBtn.isSelected()))
                            .build());
                }


                if (!(newValue instanceof ConsumerGroupTreeItem)) {
                    consumerGroupOffsetTable.setItems(FXCollections.emptyObservableList());
                }
                if (!(newValue instanceof KafkaTopicTreeItem<?> || newValue instanceof KafkaPartitionTreeItem<?>)) {
                    topicConfigTable.setItems(FXCollections.emptyObservableList());
                }
            }

            if (newValue instanceof KafkaTopicTreeItem<?> selectedItem) {
                isPolling.set(false);
                // if some clear msg table
                if (treeMsgTableItemCache.containsKey(newValue)) {
                    MessageTableState messageTableState = treeMsgTableItemCache.get(newValue);
                    ObservableList<KafkaMessageTableItem> msgItems = messageTableState.getItems();
                    allMsgTableItems.setAll(msgItems);
                    configureSortAndFilterForMessageTable(messageTableState.getFilter());
                } else {
//                    messageTable.setItems(FXCollections.emptyObservableList());
                    allMsgTableItems.setAll(FXCollections.emptyObservableList());
                    configureSortAndFilterForMessageTable(new Filter("", false));
                }

                KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
                ObservableList<UIPropertyTableItem> config = FXCollections.observableArrayList();
                String clusterName = topic.cluster().getName();
                String topicName = topic.name();
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
                partitionsTitledPane.setVisible(true);
                schemaSplitPane.setVisible(false);
                messageSplitPane.setVisible(true);
                Callable<Void> getTopicAndPartitionProperties = () -> {
                    try {
                        // topic config table
                        Collection<ConfigEntry> configEntries = clusterManager.getTopicConfig(clusterName, topicName);
                        configEntries.forEach(entry -> config.add(new UIPropertyTableItem(entry.name(), entry.value())));
                        topicConfigTable.setItems(config);
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        log.error("Error when get topic config properties", e);
//                            topicConfigTable.setItems(FXCollections.emptyObservableList());
//                            throw new RuntimeException(e);
                    }
                    return null;
                };
                Consumer<Void> onSuccess = (val) -> {
                    log.info("Successfully get topic config & partitions properties for cluster {} and topic {}", clusterName, topicName);
                };
                Consumer<Throwable> onFailure = (exception) -> {
                    log.error("Error when getting topic config & partitions properties for cluster {} and topic {}", clusterName, topicName, exception);
                };
                ViewUtil.runBackgroundTask(getTopicAndPartitionProperties, onSuccess, onFailure);

                refreshPartitionsTbl(clusterName, topicName);

            } else if (newValue instanceof KafkaPartitionTreeItem<?> selectedItem) {
                isPolling.set(false);
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
//                tabPane.getTabs().add(dataTab);
                dataTab.setDisable(false);
                propertiesTab.setDisable(false);
//                tabPane.getSelectionModel().select(dataTab);
                schemaSplitPane.setVisible(false);
                messageSplitPane.setVisible(true);
                KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
                TreeItem<?> topicTreeItem = selectedItem.getParent();
                if (treeMsgTableItemCache.containsKey(newValue)) {
                    MessageTableState messageTableState = treeMsgTableItemCache.get(newValue);
                    ObservableList<KafkaMessageTableItem> msgItems = messageTableState.getItems();
                    allMsgTableItems.setAll(msgItems);
//                    allMsgTableItems.setAll(msgItems);
                    Filter filter = messageTableState.getFilter();
                    this.filterMsgTextProperty.set(filter.getFilterText());
                    this.regexFilterToggleBtn.setSelected(filter.isRegexFilter());
//        this.allMsgTableItems.setAll(list);
//        Comparator defaultComparator = Comparator.comparing(KafkaMessageTableItem::getTimestamp).reversed();
                    ObservableList<KafkaMessageTableItem> filteredList = this.allMsgTableItems
                            .filtered(item -> item.getPartition() == partition.id())
                            .filtered(isMsgTableItemMatched(filter));
                    SortedList<KafkaMessageTableItem> sortedList = new SortedList<>(filteredList);
                    sortedList.comparatorProperty().bind(messageTable.comparatorProperty());
                    messageTable.setItems(sortedList);
//                    configureSortAndFilterForMessageTable(messageTableState.getFilterText());
                } else if (treeMsgTableItemCache.containsKey(topicTreeItem)) {
                    ObservableList<KafkaMessageTableItem> observableList = treeMsgTableItemCache.get(topicTreeItem).getItems().filtered(item -> item.getPartition() == partition.id());
                    messageTable.setItems(observableList);
                }
                final String clusterName = partition.topic().cluster().getName();
                final String topic = partition.topic().name();
                Callable<Void> getPartitionInfo = () -> {
                    try {

                        Pair<Long, Long> partitionOffsetsInfo = clusterManager.getPartitionOffsetInfo(clusterName, new TopicPartition(topic, partition.id()), null);
                        ObservableList<UIPropertyTableItem> list = FXCollections.observableArrayList(
                                new UIPropertyTableItem(UIPropertyTableItem.START_OFFSET, partitionOffsetsInfo.getLeft().toString())
                                , new UIPropertyTableItem(UIPropertyTableItem.END_OFFSET, partitionOffsetsInfo.getRight().toString())
                                , new UIPropertyTableItem(UIPropertyTableItem.NO_MESSAGES, String.valueOf(partitionOffsetsInfo.getRight() - partitionOffsetsInfo.getLeft())));

                        TopicPartitionInfo partitionInfo = clusterManager.getTopicPartitionInfo(clusterName, topic, partition.id());
                        list.addAll(getPartitionInfoForUI(partitionInfo));

                        topicConfigTable.setItems(list);
                        return null;
                    } catch (ExecutionException | InterruptedException e) {
                        log.error("Error when get partition info", e);
                        throw new RuntimeException(e);
                    }
                };
                Consumer<Void> onSuccess = (val) -> {
                    log.info("Successfully get topic config & partitions properties for cluster {}, topic {} and partition", clusterName, topic, partition.id());
                };
                Consumer<Throwable> onFailure = (exception) -> {
                    log.error("Error when getting topic config & partitions properties for cluster {} and topic {} and partition", clusterName, topic, partition.id(), exception);
                };
                ViewUtil.runBackgroundTask(getPartitionInfo, onSuccess, onFailure);

            } else if (newValue instanceof ConsumerGroupTreeItem selected) {
                if (!tabPane.getTabs().contains(cgOffsetsTab)) {
                    tabPane.getTabs().add(cgOffsetsTab);
                }
                cgOffsetsTab.setDisable(false);
                tabPane.getTabs().remove(dataTab);
                tabPane.getTabs().remove(propertiesTab);
                tabPane.getSelectionModel().select(cgOffsetsTab);
                blockAppProgressInd.setVisible(true);
                ViewUtil.runBackgroundTask(() -> {
                    try {
//                        dataTab.setDisable(true);
//                        tabPane.getSelectionModel().select(cgOffsetsTab);
                        consumerGroupOffsetTable.setItems(FXCollections.observableArrayList(clusterManager.listConsumerGroupOffsets(selected.getClusterName(), selected.getConsumerGroupId())));

                    } catch (ExecutionException | InterruptedException e) {
                        blockAppProgressInd.setVisible(false);
                        log.error("Error when get consumer group offsets", e);
                        throw new RuntimeException(e);
                    }
                    return null;
                }, (e) -> blockAppProgressInd.setVisible(false), (e) -> {
                    blockAppProgressInd.setVisible(false);
                    throw ((RuntimeException) e);
                });

            } else if (newValue instanceof TreeItem<?> selectedItem && AppConstant.TREE_ITEM_SCHEMA_REGISTRY_DISPLAY_NAME.equals(selectedItem.getValue())) {
//                blockAppProgressInd.setVisible(true);
                if (!tabPane.getTabs().contains(dataTab)) {
                    tabPane.getTabs().add(dataTab);
                }
                tabPane.getSelectionModel().select(dataTab);
                tabPane.getTabs().remove(cgOffsetsTab);
                tabPane.getTabs().remove(propertiesTab);
                dataTab.setDisable(false);
                schemaSplitPane.setVisible(true);
                messageSplitPane.setVisible(false);
                KafkaCluster clusterName = (KafkaCluster) selectedItem.getParent().getValue();
                try {
                    schemaEditableTableControl.loadAllSchemas(clusterName,
                            (e) -> blockAppProgressInd.setVisible(false),
                            (e) -> {
                                blockAppProgressInd.setVisible(false);
                                throw ((RuntimeException) e);
                            }, isBlockingAppUINeeded);
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


    private static List<UIPropertyTableItem> getPartitionInfoForUI(TopicPartitionInfo partitionInfo) {
        List<UIPropertyTableItem> list = new ArrayList<>();
        Node leader = partitionInfo.leader();
        list.add(new UIPropertyTableItem(leader.host() + ":" + leader.port(), UIPropertyTableItem.LEADER));
        list.addAll(partitionInfo.replicas().stream().filter(r -> r != leader).map(replica -> {
            if (partitionInfo.isr().contains(replica)) {
                return new UIPropertyTableItem(replica.host() + ":" + replica.port(), UIPropertyTableItem.REPLICA_IN_SYNC);
            } else {
                return new UIPropertyTableItem(replica.host() + ":" + replica.port(), UIPropertyTableItem.REPLICA_NOT_IN_SYNC);
            }
        }).toList());
        return list;
    }

    @FXML
    protected void pollMessages() {
        if (isPolling.get()) {
            isPolling.set(false);
//            pullMessagesBtn.setText(AppConstant.POLL_MESSAGES_TEXT);
            return;
        }
        TreeItem selectedTreeItem = (TreeItem) clusterTree.getSelectionModel().getSelectedItem();
        if (!(selectedTreeItem instanceof KafkaTopicTreeItem<?>)
                && !(selectedTreeItem instanceof KafkaPartitionTreeItem<?>)) {
            ViewUtil.showAlertDialog(Alert.AlertType.WARNING, "Please choose a topic or partition to poll messages", null, ButtonType.OK);
            return;
        }
        String schema = schemaTextArea.getText();
        allMsgTableItems.clear();
        // clear message cache for partitions
        treeMsgTableItemCache.forEach((treeItem, state) -> {
            if (treeItem instanceof KafkaPartitionTreeItem<?> && treeItem.getParent() == selectedTreeItem) {
                treeMsgTableItemCache.remove(treeItem);
            }
        });
        ObservableList<KafkaMessageTableItem> list = allMsgTableItems;
//        allMsgTableItems =  list;
//        filterMsgTextField.setOnKeyPressed(e -> {
//            if (e.getCode().equals(KeyCode.ENTER)) {
//                configureSortAndFilterForMessageTable(list, filterMsgTextProperty.get());
//            }
//        });
        filterMsgTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Filter filter = new Filter(filterMsgTextField.getText(), regexFilterToggleBtn.isSelected());
                configureSortAndFilterForMessageTable(filter);
                Optional.ofNullable(treeMsgTableItemCache.get(clusterTree.getSelectionModel().getSelectedItem())).ifPresent(t -> t.setFilter(filter));
            }
        });
        regexFilterToggleBtn.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Filter filter = new Filter(filterMsgTextField.getText(), regexFilterToggleBtn.isSelected());

                configureSortAndFilterForMessageTable(filter);
                Optional.ofNullable(treeMsgTableItemCache.get(clusterTree.getSelectionModel().getSelectedItem())).ifPresent(t -> t.setFilter(filter));
            }
        });
        configureSortAndFilterForMessageTable(new Filter(filterMsgTextProperty.get(), regexFilterToggleBtn.isSelected()));
//        messageTable.setItems(list);
//        if (maxMessagesTextField.getText().isEmpty()) {
//            maxMessagesTextField.setText(String.valueOf(DEFAULT_MAX_POLL_RECORDS));
//        }
        KafkaConsumerService.PollingOptions pollingOptions =
                KafkaConsumerService.PollingOptions.builder()
                        .pollTime(Integer.parseInt(pollTimeTextField.getText()))
                        .noMessages(StringUtils.isBlank(maxMessagesTextField.getText()) ? Integer.MAX_VALUE : Integer.parseInt(maxMessagesTextField.getText()))
                        .startTimestamp(getPollStartTimestamp())
                        .pollingPosition(msgPosition.getValue())
                        .valueContentType(valueContentType.getValue())
                        .schema(schema)
                        .pollCallback(() -> {
                            blockAppProgressInd.setVisible(false);
                            Platform.runLater(() -> noMsgLongProp.set(messageTable.getItems().size()));
//
                            return new KafkaConsumerService.PollCallback(list, isPolling);
                        })
                        .isLiveUpdate(!isLiveUpdateCheckBox.isDisabled() && isLiveUpdateCheckBox.isSelected())
                        .build();

//        treeMsgTableItemCache.put(selectedTreeItem, MessageTableState.builder()
//                .items(list)
//                .filterText(filterMsgTextProperty.get())
//                .pollingOptions(pollingOptions)
//                .build());
        blockAppProgressInd.setVisible(true);
        isPolling.set(true);
//        isPollingMsgProgressIndicator.setVisible(true);
//        pullMessagesBtn.setText(AppConstant.STOP_POLLING_TEXT);
        Callable<Void> pollMsgTask = () -> {

            if (selectedTreeItem instanceof KafkaPartitionTreeItem<?> selectedItem) {
                KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
//                    list.addAll(kafkaConsumerService.consumeMessages(partition, pollingOptions));
                kafkaConsumerService.consumeMessages(partition, pollingOptions);
            } else {
                KafkaTopicTreeItem<?> selectedItem = (KafkaTopicTreeItem<?>) selectedTreeItem;
                KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
                try {
//                        list.addAll(kafkaConsumerService.consumeMessages(topic, pollingOptions));
                    kafkaConsumerService.consumeMessages(topic, pollingOptions);
                } catch (Exception e) {
                    log.error("Error when poll messages", e);
                    throw new RuntimeException(e);
                }
            }

            return null;
        };
        Consumer<Void> onSuccess = (val) -> {
            blockAppProgressInd.setVisible(false);
            isPolling.set(false);
            noMsgLongProp.set(messageTable.getItems().size());
//            allMsgTableItems.setAll(list);
        };
        Consumer<Throwable> onFailure = (exception) -> {
            blockAppProgressInd.setVisible(false);
            isPolling.set(false);
            log.error("Error when poll messages", exception);
            UIErrorHandler.showError(Thread.currentThread(), exception);
        };
        ViewUtil.runBackgroundTask(pollMsgTask, onSuccess, onFailure);
    }

    private void configureSortAndFilterForMessageTable(Filter filter) {
        String filterText = filter.getFilterText();
        this.filterMsgTextProperty.set(filterText);
        this.regexFilterToggleBtn.setSelected(filter.isRegexFilter());
//        this.allMsgTableItems.setAll(list);
//        Comparator defaultComparator = Comparator.comparing(KafkaMessageTableItem::getTimestamp).reversed();
        ObservableList<KafkaMessageTableItem> filteredList = this.allMsgTableItems.filtered(isMsgTableItemMatched(filter));
        SortedList<KafkaMessageTableItem> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(messageTable.comparatorProperty());
        ObservableList<TableColumn<KafkaMessageTableItem, ?>> sortOrder = messageTable.getSortOrder();
        if (sortOrder == null || sortOrder.isEmpty()) {
            TableColumn<KafkaMessageTableItem, ?> timestampColumn = messageTable.getColumns().get(4);
            timestampColumn.setSortType(msgPosition.getValue() == KafkaConsumerService.MessagePollingPosition.FIRST
                    ? TableColumn.SortType.ASCENDING
                    : TableColumn.SortType.DESCENDING);
            messageTable.getSortOrder().add(timestampColumn);
            messageTable.sort();
        }
        messageTable.setItems(sortedList);

    }

    private Predicate<KafkaMessageTableItem> isMsgTableItemMatched(Filter filter) {
        return Filter.buildFilterPredicate(filter, KafkaMessageTableItem::getKey, KafkaMessageTableItem::getValue);
//        return (item != null && item.getKey().toLowerCase().contains(filterText.toLowerCase()))
//                || (item != null && item.getValue().toLowerCase().contains(filterText.toLowerCase()));
    }

    private void displayNotPollingMessage() {
//        isPollingMsgProgressIndicator.setVisible(false);
//        pullMessagesBtn.setText(AppConstant.POLL_MESSAGES_TEXT);
//        blockAppProgressInd.setVisible(false);
//        isPolling.set(false);
    }

    private Long getPollStartTimestamp() {
        return startTimestampPicker.getValue() != null ? ZonedDateTime.of(startTimestampPicker.getDateTimeValue(), ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    @FXML
    protected void addTopic() throws IOException, ExecutionException, InterruptedException {
        kafkaClusterTree.addTopic();
    }

    @FXML
    protected void addMessage() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaPartitionTreeItem<?> selectedItem) {
            KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
            KafkaTopic topic = partition.topic();
            addMessage(topic, partition, keyContentType.getValue(), valueContentType.getValue(), schemaTextArea.getText());
        } else if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedItem) {
            KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
            addMessage(topic, null, keyContentType.getValue(), valueContentType.getValue(), schemaTextArea.getText());
        } else {
            new Alert(Alert.AlertType.WARNING, "Please choose a topic or partition to add messages", ButtonType.OK)
                    .show();
        }
    }


    public void addMessage(@NonNull KafkaTopic kafkaTopic, KafkaPartition partition, String keyContentType, String valueContentType, String schema) throws IOException, ExecutionException, InterruptedException {

        AtomicReference<Object> ref = new AtomicReference<>();
        ViewUtil.showPopUpModal(AppConstant.ADD_MESSAGE_MODAL_FXML, "Add New Message", ref,
                Map.of("serDesHelper", serDesHelper, "valueContentType", valueContentType, "valueContentTypeComboBox", FXCollections.observableArrayList(serDesHelper.getSupportedValueSerializer()),
                        "schemaTextArea", schemaTextArea.getText()), true, true);
        KafkaMessage newMsg = (KafkaMessage) ref.get();
        if (newMsg != null) {
            producerUtil.sendMessage(kafkaTopic, partition, newMsg);
            if (!isPolling.get())
                pollMessages();
            ViewUtil.showAlertDialog(Alert.AlertType.INFORMATION, "Added message successfully! Pulling the messages", "Added message successfully!",
                    ButtonType.OK);
        }
    }

    @FXML
    protected void countMessages() {
        try {
            long count = 0;
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaPartitionTreeItem<?> selectedItem) {
                KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
                Pair<Long, Long> partitionInfo = clusterManager.getPartitionOffsetInfo(partition.topic().cluster().getName(), new TopicPartition(partition.topic().name(), partition.id()), getPollStartTimestamp());
                count = partitionInfo.getLeft() >= 0 ? (partitionInfo.getRight() - partitionInfo.getLeft()) : 0;
            } else if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedItem) {
                KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
                count = clusterManager.getAllPartitionOffsetInfo(topic.cluster().getName(), topic.name(), getPollStartTimestamp()).values()
                        .stream().mapToLong(t -> t.getLeft() >= 0 ? t.getRight() - t.getLeft() : 0).sum();
            }
            countMessagesBtn.setText("Count: " + count);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("Error when count messages", e);
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void refreshPartitionsTblAction() {
        if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> topicTreeItem) {
            KafkaTopic topic = (KafkaTopic) topicTreeItem.getValue();
            blockAppProgressInd.setVisible(true);
            refreshPartitionsTbl(topic.cluster().getName(), topic.name());
        }
    }

    public void refreshPartitionsTbl(String clusterName, String topicName) {
        Callable<Long> task = () -> {
            ObservableList<KafkaPartitionsTableItem> partitionsTableItems = FXCollections.observableArrayList();
            kafkaPartitionsTable.setItems(partitionsTableItems);
            List<TopicPartitionInfo> topicPartitionInfos = null;
            try {
                topicPartitionInfos = clusterManager.getTopicPartitions(clusterName, topicName);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                log.error("Error when get partition info for cluster {} and topic {}", clusterName, topicName, e);
                throw new RuntimeException(e);
            }
            topicPartitionInfos.forEach(partitionInfo -> {
                try {
                    Pair<Long, Long> partitionOffsetsInfo = clusterManager.getPartitionOffsetInfo(clusterName, new TopicPartition(topicName, partitionInfo.partition()), null);
                    KafkaPartitionsTableItem partitionsTableItem = ViewUtil.mapToUIPartitionTableItem(partitionInfo, partitionOffsetsInfo);
                    partitionsTableItems.add(partitionsTableItem);
                } catch (ExecutionException | InterruptedException e) {
                    log.error("Error when get partitions  offset info for Partitions table of cluster {} and topic {}", clusterName, topicName, e);
                    throw new RuntimeException(e);
                }
            });
            long totalMsg = partitionsTableItems.stream().mapToLong(KafkaPartitionsTableItem::getNoMessage).sum();
//        totalMessagesInTheTopicProperty.set(totalMsg);
            return totalMsg;
        };
        Consumer<Long> onSuccess = (val) -> {
            totalMessagesInTheTopicStringProperty.set(val + " Messages");
            if (blockAppProgressInd.isVisible())
                blockAppProgressInd.setVisible(false);
            log.info("Successfully get partitions properties for cluster {} and topic {}", clusterName, topicName);
        };
        Consumer<Throwable> onFailure = (exception) -> {
            if (blockAppProgressInd.isVisible())
                blockAppProgressInd.setVisible(false);
            throw new RuntimeException(exception);
        };
        ViewUtil.runBackgroundTask(task, onSuccess, onFailure);
//        ;
//        totalMessagesInTheTopicLabel.setText(totalMsg + " Messages");
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class MessageTableState {
        ObservableList<KafkaMessageTableItem> items;
        Filter filter;
        //        Comparator<KafkaMessageTableItem> comparator;
        KafkaConsumerService.PollingOptions pollingOptions;
    }
}