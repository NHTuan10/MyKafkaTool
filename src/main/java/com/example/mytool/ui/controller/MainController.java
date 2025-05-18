package com.example.mytool.ui.controller;

import com.example.mytool.api.PluggableDeserializer;
import com.example.mytool.api.model.KafkaMessage;
import com.example.mytool.constant.AppConstant;
import com.example.mytool.consumer.KafkaConsumerService;
import com.example.mytool.manager.ClusterManager;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.producer.ProducerUtil;
import com.example.mytool.serdes.AvroUtil;
import com.example.mytool.serdes.SerDesHelper;
import com.example.mytool.serdes.deserializer.ByteArrayDeserializer;
import com.example.mytool.serdes.deserializer.SchemaRegistryAvroDeserializer;
import com.example.mytool.serdes.deserializer.StringDeserializer;
import com.example.mytool.serdes.deserializer.deprecation.DeprecatedSchemaRegistryAvroDeserializer;
import com.example.mytool.serdes.serializer.ByteArraySerializer;
import com.example.mytool.serdes.serializer.SchemaRegistryAvroSerializer;
import com.example.mytool.serdes.serializer.StringSerializer;
import com.example.mytool.ui.*;
import com.example.mytool.ui.cg.ConsumerGroupOffsetTableItem;
import com.example.mytool.ui.cg.ConsumerGroupTreeItem;
import com.example.mytool.ui.codehighlighting.JsonHighlighter;
import com.example.mytool.ui.control.DateTimePicker;
import com.example.mytool.ui.control.SchemaEditableTableControl;
import com.example.mytool.ui.partition.KafkaPartitionTreeItem;
import com.example.mytool.ui.partition.KafkaPartitionsTableItem;
import com.example.mytool.ui.topic.KafkaTopicTreeItem;
import com.example.mytool.ui.util.ViewUtil;
import com.google.common.collect.ImmutableMap;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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

import static com.example.mytool.constant.AppConstant.DEFAULT_MAX_POLL_RECORDS;
import static com.example.mytool.constant.AppConstant.DEFAULT_POLL_TIME_MS;

@Slf4j
public class MainController {
    private final ClusterManager clusterManager = ClusterManager.getInstance();

    private final KafkaConsumerService kafkaConsumerService;

    private final ProducerUtil producerUtil;

    private final SerDesHelper serDesHelper;

    private final JsonHighlighter jsonHighlighter;

    private KafkaClusterTree kafkaClusterTree;

    private final Map<TreeItem, ObservableList<KafkaMessageTableItem>> treeMsgTableItemCache = new ConcurrentHashMap<>();

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

    @FXML
    private TextField pollTimeTextField;

    @FXML
    private Label noMessages;

    @FXML
    private Label totalMessagesInTheTopicLabel;

    @FXML
    private TextField maxMessagesTextField;

    @FXML
    private DateTimePicker timestampPicker;

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
        DeprecatedSchemaRegistryAvroDeserializer deprecatedSchemaRegistryAvroDeserializer = new DeprecatedSchemaRegistryAvroDeserializer();
        this.serDesHelper = new SerDesHelper(
                ImmutableMap.of(stringSerializer.getName(), stringSerializer,
                        byteArraySerializer.getName(), byteArraySerializer,
                        schemaRegistryAvroSerializer.getName(), schemaRegistryAvroSerializer),
                ImmutableMap.of(stringDeserializer.getName(), stringDeserializer,
                        byteArrayDeserializer.getName(), byteArrayDeserializer,
                        schemaRegistryAvroDeserializer.getName(), schemaRegistryAvroDeserializer,
                        deprecatedSchemaRegistryAvroDeserializer.getName(), deprecatedSchemaRegistryAvroDeserializer)
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

        this.kafkaClusterTree = new KafkaClusterTree(clusterManager, clusterTree, schemaEditableTableControl);
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
//        pullMessagesBtn.textProperty().bind(isPolling.map((isPolling) ->
//                isPolling ? AppConstant.STOP_POLLING_TEXT : AppConstant.POLL_MESSAGES_TEXT));
        isPolling.addListener((observable, oldValue, newValue) -> {
            pullMessagesBtn.setText(newValue ? AppConstant.STOP_POLLING_TEXT : AppConstant.POLL_MESSAGES_TEXT);
        });
    }

    private void configureTableView() {

        TableViewConfigurer.configureMessageTable(messageTable, serDesHelper);
        TableViewConfigurer.configureTableView(ConsumerGroupOffsetTableItem.class, consumerGroupOffsetTable, true);
        TableViewConfigurer.configureTableView(KafkaPartitionsTableItem.class, kafkaPartitionsTable, true);
        TableViewConfigurer.configureTableView(UIPropertyTableItem.class, topicConfigTable, true);
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


    private void initPollingOptionsUI() {
        pollTimeTextField.setText(String.valueOf(DEFAULT_POLL_TIME_MS));
        maxMessagesTextField.setText(String.valueOf(DEFAULT_MAX_POLL_RECORDS));
        timestampPicker.setDayCellFactory(param -> new DateCell() {
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
    }

    private void configureClusterTreeSelectedItemChanged() {
        clusterTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Display the selection and its complete path from the root.
            if (newValue != null && newValue != oldValue) {
                isPolling.set(false);
                // disable/hide UI tab and titled
                cgOffsetsTab.setDisable(true);
//                dataTab.setDisable(true);
                propertiesTab.setDisable(true);

                partitionsTitledPane.setVisible(false);

                // if some clear msg table
                if (treeMsgTableItemCache.containsKey(newValue)) {
                    messageTable.setItems(treeMsgTableItemCache.get(newValue));
                } else {
                    messageTable.setItems(FXCollections.emptyObservableList());
                }

                if (!(newValue instanceof ConsumerGroupTreeItem)) {
                    consumerGroupOffsetTable.setItems(FXCollections.emptyObservableList());
                }
                if (!(newValue instanceof KafkaTopicTreeItem<?> || newValue instanceof KafkaPartitionTreeItem<?>)) {
                    topicConfigTable.setItems(FXCollections.emptyObservableList());
                }
            }

            if (newValue instanceof KafkaTopicTreeItem<?> selectedItem) {
                KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
                ObservableList<UIPropertyTableItem> config = FXCollections.observableArrayList();
                String clusterName = topic.cluster().getName();
                String topicName = topic.name();
                // Enable the data tab and show/hide titled panes in the tab
                dataTab.setDisable(false);
                propertiesTab.setDisable(false);
                partitionsTitledPane.setVisible(true);
                tabPane.getSelectionModel().select(dataTab);
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
                dataTab.setDisable(false);
                propertiesTab.setDisable(false);
                tabPane.getSelectionModel().select(dataTab);
                schemaSplitPane.setVisible(false);
                messageSplitPane.setVisible(true);
                KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
                TreeItem<?> topicTreeItem = selectedItem.getParent();
                if (treeMsgTableItemCache.containsKey(topicTreeItem)) {
                    ObservableList<KafkaMessageTableItem> observableList = treeMsgTableItemCache.get(topicTreeItem).filtered(item -> item.getPartition() == partition.id());
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
                blockAppProgressInd.setVisible(true);
                ViewUtil.runBackgroundTask(() -> {
                    try {
                        cgOffsetsTab.setDisable(false);
                        dataTab.setDisable(true);
                        tabPane.getSelectionModel().select(cgOffsetsTab);
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
                dataTab.setDisable(false);
                schemaSplitPane.setVisible(true);
                messageSplitPane.setVisible(false);
                String clusterName = selectedItem.getParent().getValue().toString();
                schemaEditableTableControl.loadAllSchemas(clusterName, (e) -> blockAppProgressInd.setVisible(false), (e) -> {
                    blockAppProgressInd.setVisible(false);
                    throw ((RuntimeException) e);
                }, isBlockingAppUINeeded);

//                ViewUtil.runBackgroundTask(() -> {
//                    try {
//                        List<SchemaMetadataFromRegistry> schemaMetadataList = SchemaRegistryManager.getInstance().getAllSubjectMetadata(clusterName);
//                        schemaEditableTableControl.setItems(schemaMetadataList, clusterName);
//                        blockAppProgressInd.setVisible(false);
//                    } catch (RestClientException | IOException e) {
//                        log.error("Error when get schema registry subject metadata", e);
//                        blockAppProgressInd.setVisible(false);
//                        throw new RuntimeException(e);
//                    }
//                    return null;
//                }, (e) -> blockAppProgressInd.setVisible(false), (e) -> {
//                    blockAppProgressInd.setVisible(false);
//                    throw ((RuntimeException) e);
//                });
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
        String valueContentTypeStr = valueContentType.getValue();
        Long timestampMs = getPollStartTimestamp();
        String schema = schemaTextArea.getText();
//        }
//        if (!validateSchema(valueContentTypeStr, schema)) {
//            return;
//        }
        ObservableList<KafkaMessageTableItem> list = FXCollections.synchronizedObservableList(FXCollections.<KafkaMessageTableItem>observableArrayList());
        String filterText = filterMsgTextField.getText();
        filterMsgTextField.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                configureSortAndFilterForMessageTable(list);
            }
        });
        configureSortAndFilterForMessageTable(list);
//        messageTable.setItems(list);
        treeMsgTableItemCache.put(selectedTreeItem, list);
        blockAppProgressInd.setVisible(true);
        isPolling.set(true);
//        isPollingMsgProgressIndicator.setVisible(true);
//        pullMessagesBtn.setText(AppConstant.STOP_POLLING_TEXT);
        Callable<Void> pollMsgTask = () -> {
                KafkaConsumerService.PollingOptions pollingOptions =
                        KafkaConsumerService.PollingOptions.builder()
                                .pollTime(Integer.parseInt(pollTimeTextField.getText()))
                                .noMessages(Integer.parseInt(maxMessagesTextField.getText()))
                                .timestamp(timestampMs)
                                .pollingPosition(msgPosition.getValue())
                                .valueContentType(valueContentTypeStr)
                                .schema(schema)
                                .pollCallback(() -> {
                                    blockAppProgressInd.setVisible(false);
                                    Platform.runLater(() -> noMessages.setText(list.size() + " Messages"));
//
                                    return new KafkaConsumerService.PollCallback(list, isPolling);
                                })
                                .isLiveUpdate(isLiveUpdateCheckBox.isSelected())
                                .build();
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
            noMessages.setText(list.size() + " Messages");
        };
        Consumer<Throwable> onFailure = (exception) -> {
            blockAppProgressInd.setVisible(false);
            isPolling.set(false);
            log.error("Error when poll messages", exception);
            UIErrorHandler.showError(Thread.currentThread(), exception);
        };
        ViewUtil.runBackgroundTask(pollMsgTask, onSuccess, onFailure);
    }

    private void configureSortAndFilterForMessageTable(ObservableList<KafkaMessageTableItem> list) {
        Comparator defaultComparator = Comparator.comparing(KafkaMessageTableItem::getTimestamp).reversed();
        ObservableList<KafkaMessageTableItem> FilteredList = list.filtered((item) -> isMsgTableItemMatched(item, filterMsgTextField.getText()));
        SortedList<KafkaMessageTableItem> sortedList = new SortedList<>(FilteredList, defaultComparator);
        sortedList.comparatorProperty().bind(messageTable.comparatorProperty());
        messageTable.setItems(sortedList);
    }

    private boolean isMsgTableItemMatched(KafkaMessageTableItem item, String filterText) {
        return (item != null && item.getKey().toLowerCase().contains(filterText.toLowerCase()))
                || (item != null && item.getValue().toLowerCase().contains(filterText.toLowerCase()));
    }

    private void displayNotPollingMessage() {
//        isPollingMsgProgressIndicator.setVisible(false);
//        pullMessagesBtn.setText(AppConstant.POLL_MESSAGES_TEXT);
//        blockAppProgressInd.setVisible(false);
//        isPolling.set(false);
    }

    private Long getPollStartTimestamp() {
        return timestampPicker.getValue() != null ? ZonedDateTime.of(timestampPicker.getDateTimeValue(), ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
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
                        "schemaTextArea", schemaTextArea.getText()));
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
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaPartitionTreeItem<?> selectedItem) {
                KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
                Pair<Long, Long> partitionInfo = clusterManager.getPartitionOffsetInfo(partition.topic().cluster().getName(), new TopicPartition(partition.topic().name(), partition.id()), getPollStartTimestamp());
                noMessages.setText((partitionInfo.getLeft() >= 0 ? partitionInfo.getRight() - partitionInfo.getLeft() : 0) + " Messages");
            } else if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedItem) {
                KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
                long count = clusterManager.getAllPartitionOffsetInfo(topic.cluster().getName(), topic.name(), getPollStartTimestamp()).values()
                        .stream().mapToLong(t -> t.getLeft() >= 0 ? t.getRight() - t.getLeft() : 0).sum();
                noMessages.setText(count + " Messages");
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("Error when count messages", e);
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void refreshPartitionsTblAction() throws ExecutionException, InterruptedException, TimeoutException {
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
}