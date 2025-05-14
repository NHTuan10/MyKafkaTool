package com.example.mytool.ui.controller;

import com.example.mytool.api.KafkaMessage;
import com.example.mytool.api.PluggableDeserializer;
import com.example.mytool.constant.AppConstant;
import com.example.mytool.consumer.KafkaConsumerService;
import com.example.mytool.manager.ClusterManager;
import com.example.mytool.manager.SchemaRegistryManager;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.model.kafka.SchemaMetadataFromRegistry;
import com.example.mytool.producer.ProducerUtil;
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
import com.example.mytool.ui.control.DateTimePicker;
import com.example.mytool.ui.control.SchemaEditableTableControl;
import com.example.mytool.ui.partition.KafkaPartitionTreeItem;
import com.example.mytool.ui.partition.KafkaPartitionsTableItem;
import com.example.mytool.ui.topic.KafkaTopicTreeItem;
import com.example.mytool.ui.util.ViewUtil;
import com.google.common.collect.ImmutableMap;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.mytool.constant.AppConstant.DEFAULT_MAX_POLL_RECORDS;
import static com.example.mytool.constant.AppConstant.DEFAULT_POLL_TIME_MS;

@Slf4j
public class MainController {
    private final ClusterManager clusterManager = ClusterManager.getInstance();

    private final KafkaConsumerService kafkaConsumerService;

    private final ProducerUtil producerUtil;

    private final SerDesHelper serDesHelper;

    @FXML
    private TreeView clusterTree;

    @FXML
    private TableView<KafkaMessageTableItem> messageTable;

    @FXML
    private TableView<ConsumerGroupOffsetTableItem> consumerGroupOffsetTable;

    @FXML
    private TableView<UIPropertyTableItem> topicConfigTable;

    @FXML
    private TextField pollTimeTextField;

    @FXML
    private Label noMessages;

    @FXML
    private Label totalMessages;

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
    private TextArea schemaTextArea;

    @FXML
    private ProgressIndicator msgTableProgressInd;

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab dataTab;

    @FXML
    private Tab propertiesTab;

    @FXML
    private Tab cgOffsetsTab;

    @FXML
    private Button countMessagesBtn;

    @FXML
    private Button pullMessagesBtn;
    @FXML
    private TableView<KafkaPartitionsTableItem> kafkaPartitionsTable;

    @FXML
    private TitledPane partitionsTitledPane;

    @FXML
    private SplitPane schemaSplitPane;

    @FXML
    private SplitPane messageSplitPane;

    @FXML
    private SchemaEditableTableControl schemaEditableTableControl;

    @FXML
    private TextArea schemaRegistryTextArea;

    private KafkaClusterTree kafkaClusterTree;

    private final Map<TreeItem, ObservableList<KafkaMessageTableItem>> treeMsgTableItemCache = new ConcurrentHashMap<>();

    private final AtomicBoolean isPolling = new AtomicBoolean(false);

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
    }

    @FXML
    public void initialize() {

        msgTableProgressInd.setVisible(false);
        partitionsTitledPane.setVisible(false);
        schemaTextArea.setDisable(true);
        initPollingOptionsUI();

        this.kafkaClusterTree = new KafkaClusterTree(clusterManager, clusterTree, schemaEditableTableControl);
        kafkaClusterTree.configureClusterTreeActionMenu();
        configureClusterTreeSelectedItemChanged();
        configureTableView();
        ViewUtil.enableCopyDataFromTableToClipboard(messageTable);

    }

    private void configureTableView() {

        TableViewConfigurer.configureMessageTable(messageTable, serDesHelper);
        TableViewConfigurer.configureTableView(ConsumerGroupOffsetTableItem.class, consumerGroupOffsetTable);
        TableViewConfigurer.configureTableView(KafkaPartitionsTableItem.class, kafkaPartitionsTable);
        TableViewConfigurer.configureTableView(UIPropertyTableItem.class, topicConfigTable);
        schemaEditableTableControl.addEventHandler(SchemaEditableTableControl.SelectedSchemaEvent.SELECTED_SCHEMA_EVENT_TYPE, (event) -> schemaRegistryTextArea.textProperty().bindBidirectional(event.getData()));
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
            schemaTextArea.setDisable(!deserializer.isUserSchemaInputRequired());

        });
    }

    private void configureClusterTreeSelectedItemChanged() {
        clusterTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Display the selection and its complete path from the root.
            if (newValue != null && newValue != oldValue) {
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
                try {
                    // topic config table
                    Collection<ConfigEntry> configEntries = clusterManager.getTopicConfig(clusterName, topicName);
                    configEntries.forEach(entry -> config.add(new UIPropertyTableItem(entry.name(), entry.value())));
                    topicConfigTable.setItems(config);
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    log.error("Error when get topic config properties", e);
                    topicConfigTable.setItems(FXCollections.emptyObservableList());
                    throw new RuntimeException(e);
                }
                try {
                    // partitions table
                    refreshPartitionsTbl(clusterName, topicName);
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    log.error("Error when get partitions properties for Partitions table", e);
                    kafkaPartitionsTable.setItems(FXCollections.emptyObservableList());
                    throw new RuntimeException(e);
                }
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
                try {
                    String clusterName = partition.topic().cluster().getName();
                    String topic = partition.topic().name();
                    Pair<Long, Long> partitionOffsetsInfo = clusterManager.getPartitionOffsetInfo(clusterName, new TopicPartition(topic, partition.id()), null);
                    ObservableList<UIPropertyTableItem> list = FXCollections.observableArrayList(
                            new UIPropertyTableItem(UIPropertyTableItem.START_OFFSET, partitionOffsetsInfo.getLeft().toString())
                            , new UIPropertyTableItem(UIPropertyTableItem.END_OFFSET, partitionOffsetsInfo.getRight().toString())
                            , new UIPropertyTableItem(UIPropertyTableItem.NO_MESSAGES, String.valueOf(partitionOffsetsInfo.getRight() - partitionOffsetsInfo.getLeft())));

                    TopicPartitionInfo partitionInfo = clusterManager.getTopicPartitionInfo(clusterName, topic, partition.id());
                    list.addAll(getPartitionInfoForUI(partitionInfo));

                    topicConfigTable.setItems(list);
                } catch (ExecutionException | InterruptedException e) {
                    log.error("Error when get partition info", e);
                    throw new RuntimeException(e);
                }

            } else if (newValue instanceof ConsumerGroupTreeItem selected) {
                try {
                    cgOffsetsTab.setDisable(false);
                    dataTab.setDisable(true);
                    tabPane.getSelectionModel().select(cgOffsetsTab);
                    consumerGroupOffsetTable.setItems(FXCollections.observableArrayList(clusterManager.listConsumerGroupOffsets(selected.getClusterName(), selected.getConsumerGroupId())));
                } catch (ExecutionException | InterruptedException e) {
                    log.error("Error when get consumer group offsets", e);
                    throw new RuntimeException(e);
                }

            } else if (newValue instanceof TreeItem<?> selectedItem && AppConstant.TREE_ITEM_SCHEMA_REGISTRY_DISPLAY_NAME.equals(selectedItem.getValue())) {
                dataTab.setDisable(false);
                schemaSplitPane.setVisible(true);
                messageSplitPane.setVisible(false);
                String clusterName = selectedItem.getParent().getValue().toString();
                try {
                    List<SchemaMetadataFromRegistry> schemaMetadataList = SchemaRegistryManager.getInstance().getAllSubjectMetadata(clusterName);
                    schemaEditableTableControl.setItems(schemaMetadataList, clusterName);
                } catch (RestClientException | IOException e) {
                    log.error("Error when get schema registry subject metadata", e);
                    throw new RuntimeException(e);
                }
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
    protected void retrieveMessages() {
        if (isPolling.get()) {
            isPolling.set(false);
            pullMessagesBtn.setText(AppConstant.POLL_MESSAGES_TEXT);
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
        ObservableList<KafkaMessageTableItem> list = FXCollections.observableArrayList();
        messageTable.setItems(list);
        treeMsgTableItemCache.put(selectedTreeItem, list);
//        msgTableProgressInd.setVisible(true);
        pullMessagesBtn.setText(AppConstant.STOP_POLLING_TEXT);
        Task<Void> pollMsgTask = new Task<>() {
            @Override
            protected Void call() {
                isPolling.set(true);
                KafkaConsumerService.PollingOptions pollingOptions =
                        KafkaConsumerService.PollingOptions.builder()
                                .pollTime(Integer.parseInt(pollTimeTextField.getText()))
                                .noMessages(Integer.parseInt(maxMessagesTextField.getText()))
                                .timestamp(timestampMs)
                                .pollingPosition(msgPosition.getValue())
                                .valueContentType(valueContentTypeStr)
                                .schema(schema)
                                .pollCallback(() -> {
//                                    msgTableProgressInd.setVisible(false);
//                                    noMessages.setText(list.size() + " Messages");
                                    return new KafkaConsumerService.PollCallback(list, isPolling);
                                })
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
            }
        };
        pollMsgTask.setOnSucceeded(event -> {
//            msgTableProgressInd.setVisible(false);
            isPolling.set(false);
            pullMessagesBtn.setText(AppConstant.POLL_MESSAGES_TEXT);
            noMessages.setText(list.size() + " Messages");
        });
        pollMsgTask.setOnFailed(event -> {
//            msgTableProgressInd.setVisible(false);
            isPolling.set(false);
            pullMessagesBtn.setText(AppConstant.POLL_MESSAGES_TEXT);
            Throwable e = event.getSource().getException();
            log.error("Error when poll messages", e);
            UIErrorHandler.showError(Thread.currentThread(), e);
        });

        new Thread(pollMsgTask).start();
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
        // TODO: don't send message with key to Kafka if it's empty

        AtomicReference<Object> ref = new AtomicReference<>();
        ViewUtil.showPopUpModal(AppConstant.ADD_MESSAGE_MODAL_FXML, "Add New Message", ref,
                Map.of("serDesHelper", serDesHelper, "valueContentTypeComboBox", FXCollections.observableArrayList(serDesHelper.getSupportedValueSerializer()),
                        "schemaTextArea", schemaTextArea.getText()));
        KafkaMessage newMsg = (KafkaMessage) ref.get();
        if (newMsg != null) {
            producerUtil.sendMessage(kafkaTopic, partition, newMsg);
            retrieveMessages();
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

            refreshPartitionsTbl(topic.cluster().getName(), topic.name());
        }
    }

    public void refreshPartitionsTbl(String clusterName, String topicName) throws ExecutionException, InterruptedException, TimeoutException {
        ObservableList<KafkaPartitionsTableItem> partitionsTableItems = FXCollections.observableArrayList();
        kafkaPartitionsTable.setItems(partitionsTableItems);
        List<TopicPartitionInfo> topicPartitionInfos = clusterManager.getTopicPartitions(clusterName, topicName);
        topicPartitionInfos.forEach(partitionInfo -> {
            try {
                Pair<Long, Long> partitionOffsetsInfo = clusterManager.getPartitionOffsetInfo(clusterName, new TopicPartition(topicName, partitionInfo.partition()), null);
                KafkaPartitionsTableItem partitionsTableItem = ViewUtil.mapToUIPartitionTableItem(partitionInfo, partitionOffsetsInfo);
                partitionsTableItems.add(partitionsTableItem);
            } catch (ExecutionException | InterruptedException e) {
                log.error("Error when get partition offset info", e);
                throw new RuntimeException(e);
            }
        });
        long totalMsg = partitionsTableItems.stream().mapToLong(KafkaPartitionsTableItem::getNoMessage).sum();
        totalMessages.setText(totalMsg + " Messages");
    }
}