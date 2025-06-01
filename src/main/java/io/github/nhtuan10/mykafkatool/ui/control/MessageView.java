package io.github.nhtuan10.mykafkatool.ui.control;

import com.google.common.collect.ImmutableMap;
import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.api.PluggableDeserializer;
import io.github.nhtuan10.mykafkatool.api.model.KafkaMessage;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.consumer.KafkaConsumerService;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
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
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.KafkaMessageTableItem;
import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.UIErrorHandler;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.partition.KafkaPartitionTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicTreeItem;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.TopicPartition;
import org.fxmisc.richtext.CodeArea;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
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
public class MessageView extends SplitPane {
    private final ClusterManager clusterManager;

    private final KafkaConsumerService kafkaConsumerService;

    private final ProducerUtil producerUtil;

    private final SerDesHelper serDesHelper;

    private final JsonHighlighter jsonHighlighter;

    private StageHolder stageHolder;

    private final BooleanProperty isPolling = new SimpleBooleanProperty(false);

    private final BooleanProperty isBlockingAppUINeeded = new SimpleBooleanProperty(false);

    @FXML
    private TextField maxMessagesTextField;

    @FXML
    private DateTimePicker startTimestampPicker;

    @FXML
    private ComboBox<String> keyContentType;

    @FXML
    private ComboBox<String> valueContentType;

    @FXML
    private ComboBox<KafkaConsumerService.MessagePollingPosition> msgPollingPosition;

    @FXML
    private CodeArea schemaTextArea;

    @FXML
    private CheckBox isLiveUpdateCheckBox;

    // message buttons
    @FXML
    private Button countMessagesBtn;

    @FXML
    private Button pollMessagesBtn;

    @FXML
    private ProgressIndicator isPollingMsgProgressIndicator;

    @FXML
    private SplitPane messageSplitPane;

    @FXML
    private MessageTable messageTable;

    TreeItem selectedTreeItem;

    private final Map<TreeItem, MessageTableState> treeMsgTableItemCache = new ConcurrentHashMap<>();

    public MessageView() {
        clusterManager = ClusterManager.getInstance();
        serDesHelper = initSerDeserializer();
        this.producerUtil = new ProducerUtil(this.serDesHelper);
        this.kafkaConsumerService = new KafkaConsumerService(this.serDesHelper);
        this.jsonHighlighter = new JsonHighlighter();
        this.stageHolder = new StageHolder();

        FXMLLoader fxmlLoader = new FXMLLoader(MyKafkaToolApplication.class.getResource(
                "message-view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

    }

    private SerDesHelper initSerDeserializer() {
        StringSerializer stringSerializer = new StringSerializer();
        StringDeserializer stringDeserializer = new StringDeserializer();
        ByteArraySerializer byteArraySerializer = new ByteArraySerializer();
        ByteArrayDeserializer byteArrayDeserializer = new ByteArrayDeserializer();
        SchemaRegistryAvroSerializer schemaRegistryAvroSerializer = new SchemaRegistryAvroSerializer();
        SchemaRegistryAvroDeserializer schemaRegistryAvroDeserializer = new SchemaRegistryAvroDeserializer();
        return new SerDesHelper(
                ImmutableMap.of(stringSerializer.getName(), stringSerializer,
                        byteArraySerializer.getName(), byteArraySerializer,
                        schemaRegistryAvroSerializer.getName(), schemaRegistryAvroSerializer),
                ImmutableMap.of(stringDeserializer.getName(), stringDeserializer,
                        byteArrayDeserializer.getName(), byteArrayDeserializer,
                        schemaRegistryAvroDeserializer.getName(), schemaRegistryAvroDeserializer
                )
        );
    }

    @FXML
    private void initialize() {
        initPollingOptionsUI();
        messageTable.configureMessageTable(serDesHelper);
        isPollingMsgProgressIndicator.visibleProperty().bindBidirectional(isPolling);
        isPollingMsgProgressIndicator.managedProperty().bindBidirectional(isPolling);
        isPolling.addListener((observable, oldValue, newValue) -> {
            pollMessagesBtn.setText(newValue ? AppConstant.STOP_POLLING_TEXT : AppConstant.POLL_MESSAGES_TEXT);
        });
        messageTable.addFilterListener((filter) -> {
            if (selectedTreeItem instanceof KafkaTopicTreeItem<?>) {
                treeMsgTableItemCache.forEach((treeItem, state) -> {
                    if (treeItem instanceof KafkaPartitionTreeItem<?> && treeItem.getParent() == selectedTreeItem) {
                        state.setFilter(filter);
                    }
                });
            }
        });
    }

    private void initPollingOptionsUI() {
        maxMessagesTextField.setText(String.valueOf(DEFAULT_MAX_POLL_RECORDS));
        startTimestampPicker.setDayCellFactory(param -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
        startTimestampPicker.valueProperty().addListener(event -> countMessages());
        keyContentType.setItems(FXCollections.observableArrayList(serDesHelper.getSupportedKeyDeserializer()));
        keyContentType.getSelectionModel().selectFirst();
        valueContentType.setItems(FXCollections.observableArrayList(serDesHelper.getSupportedValueDeserializer()));
        valueContentType.getSelectionModel().selectFirst();
        msgPollingPosition.setItems(FXCollections.observableArrayList(KafkaConsumerService.MessagePollingPosition.values()));
        msgPollingPosition.setValue(KafkaConsumerService.MessagePollingPosition.LAST);
        valueContentType.setOnAction(event -> {
            PluggableDeserializer deserializer = serDesHelper.getPluggableDeserialize(valueContentType.getValue());
            schemaTextArea.setDisable(!deserializer.mayNeedUserInputForSchema());
            isPolling.set(false);
        });
        schemaTextArea.setDisable(!serDesHelper.getPluggableDeserialize(valueContentType.getValue()).mayNeedUserInputForSchema());
        schemaTextArea.textProperty().addListener((obs, oldText, newText) -> {
            ViewUtil.highlightJsonInCodeArea(newText, schemaTextArea, false, AvroUtil.OBJECT_MAPPER, jsonHighlighter);
        });
        isLiveUpdateCheckBox.setOnAction(event -> {
            if (!isLiveUpdateCheckBox.isSelected() && isPolling.get()) {
                isPolling.set(false);
            }
        });
        isLiveUpdateCheckBox.disableProperty()
                .bind(msgPollingPosition.valueProperty().map(
                        v -> v != KafkaConsumerService.MessagePollingPosition.LAST));
//        endTimestampLabel.setVisible(false);
//        endTimestampLabel.setManaged(false);
//        endTimestampPicker.setVisible(false);
//        endTimestampPicker.setManaged(false);
    }

    public void setStage(Stage stage) {
        this.stageHolder.setStage(stage);
    }

    public void cacheMessages(TreeItem oldValue) {
        treeMsgTableItemCache.put(oldValue, MessageView.MessageTableState.builder()
                .items(messageTable.getItems())
                .filter(messageTable.getFilter())
                .build());
    }

    public void switchTopicOrPartition(TreeItem newValue) {
        if (!(newValue instanceof KafkaTopicTreeItem<?> || newValue instanceof KafkaPartitionTreeItem<?>)) {
            return;
        }
        this.selectedTreeItem = newValue;
        KafkaConsumerService.MessagePollingPosition messagePollingPosition = msgPollingPosition.getValue();
        isPolling.set(false);
        if (newValue instanceof KafkaTopicTreeItem<?>) {
            // if some clear msg table
            if (treeMsgTableItemCache.containsKey(newValue)) {
                MessageView.MessageTableState messageTableState = treeMsgTableItemCache.get(newValue);
                ObservableList<KafkaMessageTableItem> msgItems = FXCollections.observableArrayList(messageTableState.getItems());
                messageTable.setItems(msgItems, false);
                messageTable.configureSortAndFilterForMessageTable(messageTableState.getFilter(), messagePollingPosition);
            } else {
                messageTable.setItems(FXCollections.observableArrayList(), false);
                messageTable.configureSortAndFilterForMessageTable(new Filter("", false), messagePollingPosition);
            }
            countMessages();
        } else if (newValue instanceof KafkaPartitionTreeItem<?>) {
            KafkaPartition partition = (KafkaPartition) newValue.getValue();
            Predicate<KafkaMessageTableItem> partitionPredicate = item -> item.getPartition() == partition.id();
            TreeItem<?> topicTreeItem = newValue.getParent();
            ObservableList<KafkaMessageTableItem> msgItems = FXCollections.observableArrayList();
            MessageView.MessageTableState messageTableState = null;
            if (treeMsgTableItemCache.containsKey(newValue)) {
                messageTableState = treeMsgTableItemCache.get(newValue);
                msgItems = FXCollections.observableArrayList(messageTableState.getItems());
            } else if (treeMsgTableItemCache.containsKey(topicTreeItem)) {
                messageTableState = treeMsgTableItemCache.get(topicTreeItem);
                msgItems = FXCollections.observableArrayList(treeMsgTableItemCache.get(topicTreeItem).getItems());
            }
            Filter filter = new Filter("", false);
            messageTable.setItems(msgItems, false);
            if (messageTableState != null) {
                filter = messageTableState.getFilter();
            }
            messageTable.configureSortAndFilterForMessageTable(filter, messagePollingPosition, partitionPredicate);
            countMessages();
        }
    }

    @FXML
    protected void pollMessages() {
        if (isPolling.get()) {
            isPolling.set(false);
            return;
        }
        if (!(selectedTreeItem instanceof KafkaTopicTreeItem<?>)
                && !(selectedTreeItem instanceof KafkaPartitionTreeItem<?>)) {
            ViewUtil.showAlertDialog(Alert.AlertType.WARNING, "Please choose a topic or partition to poll messages", null, ButtonType.OK);
            return;
        }
        String schema = schemaTextArea.getText();
        ObservableList<KafkaMessageTableItem> list = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        messageTable.setItems(list);
        // clear message cache for partitions
        treeMsgTableItemCache.forEach((treeItem, state) -> {
            if (treeItem instanceof KafkaPartitionTreeItem<?> && treeItem.getParent() == selectedTreeItem) {
                treeMsgTableItemCache.remove(treeItem);
            }
        });

        KafkaConsumerService.MessagePollingPosition messagePollingPosition = msgPollingPosition.getValue();
        KafkaConsumerService.PollingOptions pollingOptions =
                KafkaConsumerService.PollingOptions.builder()
                        .pollTime(DEFAULT_POLL_TIME_MS)
                        .noMessages(StringUtils.isBlank(maxMessagesTextField.getText()) ? Integer.MAX_VALUE : Integer.parseInt(maxMessagesTextField.getText()))
                        .startTimestamp(getPollStartTimestamp())
                        .pollingPosition(messagePollingPosition)
                        .valueContentType(valueContentType.getValue())
                        .schema(schema)
                        .pollCallback(() -> {
                            isBlockingAppUINeeded.set(false);
                            Platform.runLater(() -> messageTable.handleNumOfMsgChanged(messageTable.getShownItems().size()));
                            return new KafkaConsumerService.PollCallback(list, isPolling);
                        })
                        .isLiveUpdate(!isLiveUpdateCheckBox.isDisabled() && isLiveUpdateCheckBox.isSelected())
                        .build();

        isBlockingAppUINeeded.set(true);
        isPolling.set(true);
        Callable<Void> pollMsgTask = () -> {

            if (selectedTreeItem instanceof KafkaPartitionTreeItem<?> selectedItem) {
                KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
                kafkaConsumerService.consumeMessages(partition, pollingOptions);
            } else {
                KafkaTopicTreeItem<?> selectedItem = (KafkaTopicTreeItem<?>) selectedTreeItem;
                KafkaTopic topic = (KafkaTopic) selectedItem.getValue();
                try {
                    kafkaConsumerService.consumeMessages(topic, pollingOptions);
                } catch (Exception e) {
                    log.error("Error when polling messages", e);
                    throw new RuntimeException(e);
                }
            }

            return null;
        };
        Consumer<Void> onSuccess = (val) -> {
            isBlockingAppUINeeded.set(false);
            isPolling.set(false);
            messageTable.handleNumOfMsgChanged(messageTable.getShownItems().size());
        };
        Consumer<Throwable> onFailure = (exception) -> {
            isBlockingAppUINeeded.set(false);
            isPolling.set(false);
            log.error("Error when polling messages", exception);
            UIErrorHandler.showError(Thread.currentThread(), exception);
        };
        ViewUtil.runBackgroundTask(pollMsgTask, onSuccess, onFailure);
    }

    private Long getPollStartTimestamp() {
        return startTimestampPicker.getValue() != null ? ZonedDateTime.of(startTimestampPicker.getDateTimeValue(), ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    @FXML
    protected void addMessage() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        if (selectedTreeItem instanceof KafkaPartitionTreeItem<?>) {
            KafkaPartition partition = (KafkaPartition) selectedTreeItem.getValue();
            KafkaTopic topic = partition.topic();
            addMessage(topic, partition, keyContentType.getValue(), valueContentType.getValue(), schemaTextArea.getText());
        } else if (selectedTreeItem instanceof KafkaTopicTreeItem<?>) {
            KafkaTopic topic = (KafkaTopic) selectedTreeItem.getValue();
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
                        "schemaTextArea", schemaTextArea.getText()), true, true, stageHolder.getStage());
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
    public void countMessages() {
        Callable<Long> callable = () -> {
            try {
                long count;
                if (selectedTreeItem instanceof KafkaPartitionTreeItem<?>) {
                    KafkaPartition partition = (KafkaPartition) selectedTreeItem.getValue();
                    Pair<Long, Long> partitionInfo = clusterManager.getPartitionOffsetInfo(partition.topic().cluster().getName(), new TopicPartition(partition.topic().name(), partition.id()), getPollStartTimestamp());
                    count = partitionInfo.getLeft() >= 0 ? (partitionInfo.getRight() - partitionInfo.getLeft()) : 0;
                } else if (selectedTreeItem instanceof KafkaTopicTreeItem<?>) {
                    KafkaTopic topic = (KafkaTopic) selectedTreeItem.getValue();
                    count = clusterManager.getAllPartitionOffsetInfo(topic.cluster().getName(), topic.name(), getPollStartTimestamp()).values()
                            .stream().mapToLong(t -> t.getLeft() >= 0 ? t.getRight() - t.getLeft() : 0).sum();
                } else {
                    count = 0;
                }
                return count;
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                log.error("Error when count messages", e);
                throw new RuntimeException(e);
            }
        };
        ViewUtil.runBackgroundTask(callable, (count) -> countMessagesBtn.setText("Count: " + count), (e) -> {
            throw ((RuntimeException) e);
        });
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
