package com.example.mytool;

import com.example.mytool.constant.AppConstant;
import com.example.mytool.manager.ClusterManager;
import com.example.mytool.manager.ProducerCreator;
import com.example.mytool.manager.UserPreferenceManager;
import com.example.mytool.model.kafka.KafkaCluster;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.model.kafka.KafkaTopicConfig;
import com.example.mytool.serde.Util;
import com.example.mytool.service.KafkaConsumerService;
import com.example.mytool.ui.*;
import com.example.mytool.ui.util.DateTimePicker;
import com.example.mytool.ui.util.ViewUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;


public class MainController {
    private final ClusterManager clusterManager = ClusterManager.getInstance();
    //    @FXML
//    private Label welcomeText;
//    private Tuple2<String, String> newMsg;
    private KafkaConsumerService kafkaConsumerService;
    private Tuple2<String, String> newConnection;

    @FXML
    private TreeView clusterTree;

    @FXML
    private TableView<KafkaMessageTableItem> messageTable;

    @FXML
    private TableView<ConsumerGroupOffsetTableItem> consumerGroupOffsetTable;

    @FXML
    private TableView<KafkaTopicConfig> topicConfigTable;

    @FXML
    private TextField pollTimeTextField;

    @FXML
    private Label noMessages;

    @FXML
    private TextField maxMessagesTextField;

    @FXML
    private DateTimePicker timestampPicker;

    @FXML
    private ComboBox<String> keyContentType;

    @FXML
    private ComboBox<String> valueContentType;

    public MainController() {
        this.kafkaConsumerService = new KafkaConsumerService();
    }

    @FXML
    public void initialize() {
        timestampPicker.setDayCellFactory(param -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.compareTo(LocalDate.now()) > 0);
            }
        });
        MenuItem addNewConnectionItem = new MenuItem("Add New Connection");
        addNewConnectionItem.setOnAction(ae -> {
            try {
//                TreeItem treeItem = (TreeItem) clusterTree.getSelectionModel().getSelectedItem();
//                if (treeItem != null && treeItem.getParent() == null && AppConstant.TREE_ITEM_CLUSTERS_DISPLAY_NAME.equalsIgnoreCase((String) treeItem.getValue())){
                showAddModal("add-connection-modal.fxml", new AtomicReference<>());
                if (newConnection != null) {
                    KafkaCluster cluster = new KafkaCluster(newConnection.getT1(), newConnection.getT2());
                    ViewUtil.addClusterConnIntoClusterTreeView(clusterTree, cluster);
                    UserPreferenceManager.addClusterToUserPreference(cluster);
                    newConnection = null;
                }
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        MenuItem blankItem = new MenuItem("");
//        blankItem.setDisable(true);
        blankItem.setVisible(false);

        MenuItem deleteTopicItem = new MenuItem("Delete");
        deleteTopicItem.setOnAction(ae -> {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedTopicTreeItem) {
                KafkaTopic topic = (KafkaTopic) selectedTopicTreeItem.getValue();
                if (ViewUtil.confirmAlert("Delete Topic", "Are you sure to delete " + topic.getName() + " ?", "Yes", "Cancel")) {
                    clusterManager.deleteTopic(topic.getCluster().getName(), topic.getName());
                }
            }
        });

        MenuItem addNewTopicItem = new MenuItem("Add New Topic");
        addNewTopicItem.setOnAction(ae -> {
            try {
                addTopic();

            } catch (IOException | ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        MenuItem purgeTopicItem = new MenuItem("Purge Topic");
        purgeTopicItem.setOnAction(ae -> {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedTopicTreeItem) {
                KafkaTopic topic = (KafkaTopic) selectedTopicTreeItem.getValue();
                if (ViewUtil.confirmAlert("Purge Topic", "Are you sure to delete all data in the topic " + topic.getName() + " ?", "Yes", "Cancel")) {
                    try {
                        clusterManager.purgeTopic(topic);
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        MenuItem purgePartitionItem = new MenuItem("Purge Partition");
        purgePartitionItem.setOnAction(ae -> {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaPartitionTreeItem<?> selectedPartitionTreeItem) {
                KafkaPartition partition = (KafkaPartition) selectedPartitionTreeItem.getValue();
                if (ViewUtil.confirmAlert("Purge Partition", "Are you sure to delete all data in the partition " + partition.getId() + " ?", "Yes", "Cancel")) {
                    try {
                        clusterManager.purgePartition(partition);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        ContextMenu clusterTreeContextMenu = new ContextMenu(blankItem);

        clusterTreeContextMenu.setOnShowing(ae -> {
            TreeItem treeItem = (TreeItem) clusterTree.getSelectionModel().getSelectedItem();
            if ((treeItem == null) || (treeItem.getParent() == null && AppConstant.TREE_ITEM_CLUSTERS_DISPLAY_NAME.equalsIgnoreCase((String) treeItem.getValue()))) {
                clusterTreeContextMenu.getItems().setAll(addNewConnectionItem);
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
                clusterTreeContextMenu.getItems().setAll(addNewTopicItem, refreshItem);
            } else {
                clusterTreeContextMenu.getItems().setAll(blankItem);

            }
        });
        clusterTree.setContextMenu(clusterTreeContextMenu);

        clusterTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Display the selection and its complete path from the root.
            if (newValue != null && newValue != oldValue) {
                messageTable.setItems(FXCollections.emptyObservableList());
                if (!(newValue instanceof ConsumerGroupTreeItem)) {
                    consumerGroupOffsetTable.setItems(FXCollections.emptyObservableList());
                }
                if (!(newValue instanceof KafkaTopicTreeItem<?> || newValue instanceof KafkaPartitionTreeItem<?>)) {
                    topicConfigTable.setItems(FXCollections.emptyObservableList());
                }
            }

            if (newValue instanceof KafkaTopicTreeItem<?> selectedItem) {
                KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
                ObservableList<KafkaTopicConfig> list = FXCollections.observableArrayList();
                try {
                    Collection<ConfigEntry> configs = clusterManager.getTopicConfig(topic.getCluster().getName(), topic.getName());
                    configs.forEach(entry -> list.add(new KafkaTopicConfig(entry.name(), entry.value())));
                    topicConfigTable.setItems(list);
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    e.printStackTrace();
                }
            } else if (newValue instanceof KafkaPartitionTreeItem<?> selectedItem) {
                KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
                try {
                    String clusterName = partition.getTopic().getCluster().getName();
                    String topic = partition.getTopic().getName();
                    Tuple2<Long, Long> partitionOffsetsInfo = clusterManager.getPartitionOffsetInfo(clusterName, new TopicPartition(topic, partition.getId()));
                    ObservableList<KafkaTopicConfig> list = FXCollections.observableArrayList(
                            new KafkaTopicConfig("Start Offset", partitionOffsetsInfo.getT1().toString())
                            , new KafkaTopicConfig("End Offset", partitionOffsetsInfo.getT2().toString())
                            , new KafkaTopicConfig("Number of Messages", String.valueOf(partitionOffsetsInfo.getT2() - partitionOffsetsInfo.getT1())));

                    TopicPartitionInfo partitionInfo = clusterManager.getTopicPartitionInfo(clusterName, topic, partition.getId());
                    Node leader = partitionInfo.leader();
                    list.add(new KafkaTopicConfig(leader.host() + ":" + leader.port(), "Leader"));
                    list.addAll(partitionInfo.replicas().stream().filter(r -> r != leader).map(replica -> {
                        if (partitionInfo.isr().contains(replica)) {
                            return new KafkaTopicConfig(replica.host() + ":" + replica.port(), "Replica [In-Sync]");
                        } else {
                            return new KafkaTopicConfig(replica.host() + ":" + replica.port(), "Replica [Not-In-Sync]");
                        }
                    }).toList());

                    topicConfigTable.setItems(list);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }

            } else if (newValue instanceof ConsumerGroupTreeItem selected) {
                try {
                    consumerGroupOffsetTable.setItems(FXCollections.observableArrayList(clusterManager.listConsumerGroupOffsets(selected.getClusterName(), selected.getConsumerGroupId())));
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        keyContentType.setItems(FXCollections.observableArrayList(Util.SERDE_STRING));
        keyContentType.setValue(Util.SERDE_STRING);
        valueContentType.setItems(FXCollections.observableArrayList(Util.SERDE_STRING, Util.SERDE_AVRO));
        valueContentType.setValue(Util.SERDE_STRING);
    }

    @FXML
    protected void retrieveMessages() throws ExecutionException, InterruptedException, TimeoutException {
        Long timestampMs = null;
        if (timestampPicker.getValue() != null) {
            timestampMs = ZonedDateTime.of(timestampPicker.getDateTimeValue(), ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        ObservableList<KafkaMessageTableItem> list = FXCollections.observableArrayList();
        messageTable.setItems(list);
        if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaPartitionTreeItem<?> selectedItem) {
            KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
            list.addAll(kafkaConsumerService.consumeMessages(partition, Integer.parseInt(pollTimeTextField.getText()), Integer.parseInt(maxMessagesTextField.getText()), timestampMs));
        } else if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedItem) {
            KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
            list.addAll(kafkaConsumerService.consumeMessages(topic, Integer.parseInt(pollTimeTextField.getText()), Integer.parseInt(maxMessagesTextField.getText()), timestampMs));
        }
        noMessages.setText(list.size() + " Messages");
    }

    @FXML
    protected void addTopic() throws IOException, ExecutionException, InterruptedException {
        if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicListTreeItem<?> topicListTreeItem) {
            String clusterName = ((KafkaTopicListTreeItem.KafkaTopicListTreeItemValue) topicListTreeItem.getValue()).getCluster().getName();
            AtomicReference modelRef = new AtomicReference<>();
            showAddModal("add-topic-modal.fxml", modelRef);
            NewTopic newTopic = (NewTopic) modelRef.get();
            CreateTopicsResult result = clusterManager.addTopic(clusterName, newTopic);
            result.all().get();
            topicListTreeItem.reloadChildren();
        }
    }

    @FXML
    protected void addMessage() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaPartitionTreeItem<?> selectedItem) {
            KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
            addMessage(null, partition, keyContentType.getValue(), valueContentType.getValue());
        } else if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedItem) {
            KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
            addMessage(topic, null, keyContentType.getValue(), valueContentType.getValue());
        }
    }


    public void addMessage(KafkaTopic kafkaTopic, KafkaPartition partition, String keyContentType, String valueContentType) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        if (kafkaTopic == null && partition == null)
            return;
//        Tuple2<String, String> newMsg = showAddModal();
        // TODO: don't send message with key to Kafka if it's empty
        AtomicReference ref = new AtomicReference<>();
        showAddModal("add-message-modal.fxml", ref);
        Tuple3<String, String, String> newMsg = (Tuple3<String, String, String>) ref.get();
        if (newMsg != null) {
            KafkaProducer producer;
            ProducerRecord record;
            if (partition != null) {
                ProducerCreator.ProducerCreatorConfig producerConfig = ProducerCreator.ProducerCreatorConfig.builder()
                        .cluster(partition.getTopic().getCluster())
                        .keySerializer(Util.getSerdeClass(keyContentType))
                        .valueSerializer(Util.getSerdeClass(valueContentType))
                        .build();
                producer = clusterManager.getProducer(producerConfig);
                producer.flush();
                String key = StringUtils.isBlank(newMsg.getT1()) ? null : newMsg.getT1();
                Object value = Util.convert(valueContentType, newMsg.getT2(), newMsg.getT3());
                record = new ProducerRecord<>(partition.getTopic().getName(), partition.getId(), key, value);
            } else {
                ProducerCreator.ProducerCreatorConfig producerConfig = ProducerCreator.ProducerCreatorConfig.builder()
                        .cluster(kafkaTopic.getCluster())
                        .keySerializer(Util.getSerdeClass(keyContentType))
                        .valueSerializer(Util.getSerdeClass(valueContentType))
                        .build();
                producer = clusterManager.getProducer(producerConfig);
                String key = StringUtils.isBlank(newMsg.getT1()) ? null : newMsg.getT1();
                Object value = Util.convert(valueContentType, newMsg.getT2(), newMsg.getT3());
                record = new ProducerRecord<>(kafkaTopic.getName(), key, value);
            }
            try {
                RecordMetadata metadata = (RecordMetadata) producer.send(record).get();
                System.out.println("record sent with key " + newMsg.getT1() + " to partition " + metadata.partition()
                        + " with offset " + metadata.offset());

            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Error in sending record");
                e.printStackTrace();
            }
            retrieveMessages();
            newMsg = null;
        }
    }

    //    private Tuple2<String, String> showAddMsgModalAndGetResult() throws IOException {
    private void showAddModal(String modalFxml, AtomicReference<Object> modelRef) throws IOException {
        Stage stage = new Stage();
//        FXMLLoader addMsgModalLoader = new FXMLLoader(
//                AddMessageModalController.class.getResource("add-message-modal.fxml"));

        FXMLLoader modalLoader = new FXMLLoader(
                MainController.class.getResource(modalFxml));
        stage.setScene(new Scene(modalLoader.load()));
//        AddMessageModalController addMessageModalController =  modalLoader.getController();
        ModalController modalController = modalLoader.getController();
        modalController.setMainController(this);
        modalController.setModelRef(modelRef);
        stage.setTitle("Add Message");
        stage.initModality(Modality.WINDOW_MODAL);
//        ActionEvent event
//        stage.initOwner(
//                ((Node)event.getSource()).getScene().getWindow() );
        stage.showAndWait();
//        return modelRef.get();
    }

//    public void setNewMsg(Tuple2<String, String> newMsg) {
//        this.newMsg = newMsg;
//    }

    public void setNewConnection(Tuple2<String, String> newConnection) {
        this.newConnection = newConnection;
    }

    @FXML
    protected void countMessages() throws IOException {
        try {
            if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaPartitionTreeItem<?> selectedItem) {
                KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
                Tuple2<Long, Long> partitionInfo = clusterManager.getPartitionOffsetInfo(partition.getTopic().getCluster().getName(), new TopicPartition(partition.getTopic().getName(), partition.getId()));
                noMessages.setText((partitionInfo.getT2() - partitionInfo.getT1()) + " Messages");
            } else if (clusterTree.getSelectionModel().getSelectedItem() instanceof KafkaTopicTreeItem<?> selectedItem) {
                KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
                long count = clusterManager.getAllPartitionOffsetInfo(topic.getCluster().getName(), topic.getName()).values()
                        .stream().mapToLong(t -> t.getT2() - t.getT1()).sum();
                noMessages.setText(count + " Messages");
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}