package com.example.mytool.ui.controller;

import com.example.mytool.constant.AppConstant;
import com.example.mytool.consumer.KafkaConsumerService;
import com.example.mytool.manager.ClusterManager;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.producer.KafkaMessage;
import com.example.mytool.producer.ProducerUtil;
import com.example.mytool.serde.AvroUtil;
import com.example.mytool.serde.SerdeUtil;
import com.example.mytool.ui.KafkaClusterTree;
import com.example.mytool.ui.KafkaMessageTableItem;
import com.example.mytool.ui.TableViewConfigurer;
import com.example.mytool.ui.UIPropertyItem;
import com.example.mytool.ui.cg.ConsumerGroupOffsetTableItem;
import com.example.mytool.ui.cg.ConsumerGroupTreeItem;
import com.example.mytool.ui.partition.KafkaPartitionTreeItem;
import com.example.mytool.ui.partition.KafkaPartitionsTableItem;
import com.example.mytool.ui.topic.KafkaTopicTreeItem;
import com.example.mytool.ui.util.DateTimePicker;
import com.example.mytool.ui.util.ViewUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
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

    private KafkaConsumerService kafkaConsumerService;

    @FXML
    private TreeView clusterTree;

    @FXML
    private TableView<KafkaMessageTableItem> messageTable;

    @FXML
    private TableView<ConsumerGroupOffsetTableItem> consumerGroupOffsetTable;

    @FXML
    private TableView<UIPropertyItem> topicConfigTable;

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
    private Tab messagesTab;

    @FXML
    private Tab propertiesTab;

    @FXML
    private Tab cgOffsetsTab;

    @FXML
    private Button countMessagesBtn;

    @FXML
    private Button pullMessagesBtn;
    @FXML
    private TableView kafkaPartitionsTable;

    @FXML
    private TitledPane partitionsTitledPane;

    private KafkaClusterTree kafkaClusterTree;

    private Map<TreeItem, ObservableList<KafkaMessageTableItem>> treeMsgTableItemCache = new ConcurrentHashMap<>();

    private AtomicBoolean isPolling = new AtomicBoolean(false);

    public MainController() {
        this.kafkaConsumerService = new KafkaConsumerService();
    }

    @FXML
    public void initialize() {

        msgTableProgressInd.setVisible(false);
        partitionsTitledPane.setVisible(false);
        schemaTextArea.setDisable(true);
        initPollingOptionsUI();

        this.kafkaClusterTree = new KafkaClusterTree(clusterManager, clusterTree);
        kafkaClusterTree.configureClusterTreeActionMenu();
        configureClusterTreeSelectedItemChanged();
        configureTableView();
        ViewUtil.enableCopyDataFromTableToClipboard(messageTable);

    }

    private void configureTableView() {
        TableViewConfigurer.configureTableView(KafkaMessageTableItem.class, messageTable);
        messageTable.setRowFactory(tv -> {
            TableRow<KafkaMessageTableItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    KafkaMessageTableItem rowData = row.getItem();
                    log.debug("Double click on: " + rowData.getKey());
                    Map<String, Object> msgModalFieldMap = Map.of(
                            "keyTextArea", rowData.getKey(),
                            "valueTextArea", rowData.getValue(),
                            "valueContentTypeComboBox", FXCollections.observableArrayList(rowData.getValueContentType()),
                            "headerTable",
                            FXCollections.observableArrayList(
                                    Arrays.stream(rowData.getHeaders().toArray()).map(header -> new UIPropertyItem(header.key(), new String(header.value()))).toList()));
                    try {
                        ViewUtil.showPopUpModal(AppConstant.ADD_MESSAGE_MODAL_FXML, "View Message", new AtomicReference<>(), msgModalFieldMap, false);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

//                    System.out.println("Double click on: "+rowData.getKey());
                }
            });
            return row;
        });
        TableViewConfigurer.configureTableView(ConsumerGroupOffsetTableItem.class, consumerGroupOffsetTable);
        TableViewConfigurer.configureTableView(KafkaPartitionsTableItem.class, kafkaPartitionsTable);
        TableViewConfigurer.configureTableView(UIPropertyItem.class, topicConfigTable);

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
                setDisable(empty || date.compareTo(LocalDate.now()) > 0);
            }
        });
        keyContentType.setItems(FXCollections.observableArrayList(SerdeUtil.SERDE_STRING));
        keyContentType.setValue(SerdeUtil.SERDE_STRING);
        valueContentType.setItems(SerdeUtil.SUPPORT_VALUE_CONTENT_TYPES);
        valueContentType.setValue(SerdeUtil.SERDE_STRING);
        msgPosition.setItems(FXCollections.observableArrayList(KafkaConsumerService.MessagePollingPosition.values()));
        msgPosition.setValue(KafkaConsumerService.MessagePollingPosition.LAST);
        valueContentType.setOnAction(event -> {
            if (valueContentType.getValue().equals(SerdeUtil.SERDE_AVRO)) {
                schemaTextArea.setDisable(false);
            } else {
                schemaTextArea.setDisable(true);
            }
        });
    }

    private void configureClusterTreeSelectedItemChanged() {
        clusterTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Display the selection and its complete path from the root.
            if (newValue != null && newValue != oldValue) {
                // disable/hide UI tab & titled
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
                ObservableList<UIPropertyItem> config = FXCollections.observableArrayList();
                String clusterName = topic.getCluster().getName();
                String topicName = topic.getName();
                // Enable  datatabs and show/hide titled panes in tab
                messagesTab.setDisable(false);
                propertiesTab.setDisable(false);
                partitionsTitledPane.setVisible(true);
                tabPane.getSelectionModel().select(messagesTab);
                try {
                    // topic config table
                    Collection<ConfigEntry> configEntries = clusterManager.getTopicConfig(clusterName, topicName);
                    configEntries.forEach(entry -> config.add(new UIPropertyItem(entry.name(), entry.value())));
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
                messagesTab.setDisable(false);
                propertiesTab.setDisable(false);
                tabPane.getSelectionModel().select(messagesTab);
                KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
                TreeItem topicTreeItem = selectedItem.getParent();
                if (treeMsgTableItemCache.containsKey(topicTreeItem)) {
                    ObservableList observableList = treeMsgTableItemCache.get(topicTreeItem).filtered(item -> item.getPartition() == partition.getId());
                    messageTable.setItems(observableList);
                }
                try {
                    String clusterName = partition.getTopic().getCluster().getName();
                    String topic = partition.getTopic().getName();
                    Pair<Long, Long> partitionOffsetsInfo = clusterManager.getPartitionOffsetInfo(clusterName, new TopicPartition(topic, partition.getId()), null);
                    ObservableList<UIPropertyItem> list = FXCollections.observableArrayList(
                            new UIPropertyItem(UIPropertyItem.START_OFFSET, partitionOffsetsInfo.getLeft().toString())
                            , new UIPropertyItem(UIPropertyItem.END_OFFSET, partitionOffsetsInfo.getRight().toString())
                            , new UIPropertyItem(UIPropertyItem.NO_MESSAGES, String.valueOf(partitionOffsetsInfo.getRight() - partitionOffsetsInfo.getLeft())));

                    TopicPartitionInfo partitionInfo = clusterManager.getTopicPartitionInfo(clusterName, topic, partition.getId());
                    list.addAll(getPartitionInfoForUI(partitionInfo));

                    topicConfigTable.setItems(list);
                } catch (ExecutionException | InterruptedException e) {
                    log.error("Error when get partition info", e);
                    throw new RuntimeException(e);
                }

            } else if (newValue instanceof ConsumerGroupTreeItem selected) {
                try {
                    cgOffsetsTab.setDisable(false);
                    messagesTab.setDisable(true);
                    tabPane.getSelectionModel().select(cgOffsetsTab);
                    consumerGroupOffsetTable.setItems(FXCollections.observableArrayList(clusterManager.listConsumerGroupOffsets(selected.getClusterName(), selected.getConsumerGroupId())));
                } catch (ExecutionException | InterruptedException e) {
                    log.error("Error when get consumer group offsets", e);
                    throw new RuntimeException(e);
                }

            }
        });
    }


    private static List<UIPropertyItem> getPartitionInfoForUI(TopicPartitionInfo partitionInfo) {
        List<UIPropertyItem> list = new ArrayList<>();
        Node leader = partitionInfo.leader();
        list.add(new UIPropertyItem(leader.host() + ":" + leader.port(), UIPropertyItem.LEADER));
        list.addAll(partitionInfo.replicas().stream().filter(r -> r != leader).map(replica -> {
            if (partitionInfo.isr().contains(replica)) {
                return new UIPropertyItem(replica.host() + ":" + replica.port(), UIPropertyItem.REPLICA_IN_SYNC);
            } else {
                return new UIPropertyItem(replica.host() + ":" + replica.port(), UIPropertyItem.REPLICA_NOT_IN_SYNC);
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
        Task<Void> pollMsgTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
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
                } else if (selectedTreeItem instanceof KafkaTopicTreeItem<?> selectedItem) {
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

        });

        new Thread(pollMsgTask).start();
    }

    private Long getPollStartTimestamp() {
        Long timestampMs = timestampPicker.getValue() != null ? ZonedDateTime.of(timestampPicker.getDateTimeValue(), ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
        return timestampMs;
    }

    @FXML
    protected void addTopic() throws IOException, ExecutionException, InterruptedException {
        kafkaClusterTree.addTopic();
    }

    @FXML
    protected void addMessage() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaPartitionTreeItem<?> selectedItem) {
            KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
            KafkaTopic topic = partition.getTopic();
            addMessage(topic, partition, keyContentType.getValue(), valueContentType.getValue(), schemaTextArea.getText());
        } else if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedItem) {
            KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
            addMessage(topic, null, keyContentType.getValue(), valueContentType.getValue(), schemaTextArea.getText());
        } else {
            new Alert(Alert.AlertType.WARNING, "Please choose a topic or partition to add messages", ButtonType.OK)
                    .show();
        }
    }


    public void addMessage(@NonNull KafkaTopic kafkaTopic, KafkaPartition partition, String keyContentType, String valueContentType, String schema) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        // TODO: don't send message with key to Kafka if it's empty

        AtomicReference<Object> ref = new AtomicReference<>();
        ViewUtil.showPopUpModal(AppConstant.ADD_MESSAGE_MODAL_FXML, "Add New Message", ref,
                Map.of("valueContentTypeComboBox", SerdeUtil.SUPPORT_VALUE_CONTENT_TYPES,
                        "schemaTextArea", schemaTextArea.getText()));
        KafkaMessage newMsg = (KafkaMessage) ref.get();
        if (newMsg != null) {
//            String inputKey = newMsg.key();
//            String inputValue = newMsg.value();
//            schema = newMsg.schema();
//            valueContentType = newMsg.valueContentType();
            ProducerUtil.sendMessage(kafkaTopic, partition, newMsg);
            retrieveMessages();
            ViewUtil.showAlertDialog(Alert.AlertType.INFORMATION, "Added message successfully! Pulling the messages", "Added message successfully!",
                    ButtonType.OK);
        }
    }


    public static boolean validateSchema(String valueContentType, String schema) {
        boolean valid = true;
        if (SerdeUtil.SERDE_AVRO.equals(valueContentType)) {
            try {
                if (StringUtils.isNotBlank(schema) &&
                        AvroUtil.parseSchema(schema) != null) {
                    valid = true;
                } else {
                    valid = false;
                }
            } catch (Exception e) {
                log.warn("Error when parse schema", e);
                valid = false;
            }
        }
        if (!valid) {
            ViewUtil.showAlertDialog(Alert.AlertType.WARNING, "Schema is invalid", null,
                    ButtonType.OK);
        }
        return valid;
    }

    //    public void setNewMsg(Tuple2<String, String> newMsg) {
//        this.newMsg = newMsg;
//    }

//    public void setNewConnection(Pair<String, String> newConnection) {
//        this.newConnection = newConnection;
//    }

    @FXML
    protected void countMessages() throws IOException {
        try {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaPartitionTreeItem<?> selectedItem) {
                KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
                Pair<Long, Long> partitionInfo = clusterManager.getPartitionOffsetInfo(partition.getTopic().getCluster().getName(), new TopicPartition(partition.getTopic().getName(), partition.getId()), getPollStartTimestamp());
                noMessages.setText((partitionInfo.getLeft() >= 0 ? partitionInfo.getRight() - partitionInfo.getLeft() : 0) + " Messages");
            } else if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedItem) {
                KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
                long count = clusterManager.getAllPartitionOffsetInfo(topic.getCluster().getName(), topic.getName(), getPollStartTimestamp()).values()
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

            refreshPartitionsTbl(topic.getCluster().getName(), topic.getName());
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