package io.github.nhtuan10.mykafkatool.ui.messageview;

import com.google.common.collect.ImmutableMap;
import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.api.model.KafkaMessage;
import io.github.nhtuan10.mykafkatool.api.serdes.PluggableDeserializer;
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
import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.UIErrorHandler;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.control.DateTimePicker;
import io.github.nhtuan10.mykafkatool.ui.event.*;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaPartitionTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicTreeItem;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.TopicPartition;
import org.fxmisc.richtext.CodeArea;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
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
public class KafkaMessageView extends SplitPane {
    private final ClusterManager clusterManager;

    private final KafkaConsumerService kafkaConsumerService;

    private final ProducerUtil producerUtil;

    private final SerDesHelper serDesHelper;

    private final JsonHighlighter jsonHighlighter;

    private StageHolder stageHolder;

    private final BooleanProperty isPolling = new SimpleBooleanProperty(false);

    private final BooleanProperty isBlockingAppUINeeded = new SimpleBooleanProperty(false);

    TreeItem selectedTreeItem;

    @Getter
    private final TopicEventSubscriber topicEventSubscriber;

    @Getter
    private final PartitionEventSubscriber partitionEventSubscriber;

    private final Map<TreeItem, MessageTableState> treeMsgTableItemCache = new ConcurrentHashMap<>();

    @FXML
    private TextField maxMessagesTextField;

    @FXML
    private DateTimePicker startTimestampPicker;

    @FXML
    private DateTimePicker endTimestampPicker;

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
    private KafkaMessageTable kafkaMessageTable;

    public KafkaMessageView() {
        clusterManager = ClusterManager.getInstance();
        serDesHelper = initSerDeserializer();
        this.producerUtil = new ProducerUtil(this.serDesHelper);
        this.kafkaConsumerService = new KafkaConsumerService(this.serDesHelper);
        this.jsonHighlighter = new JsonHighlighter();
        this.stageHolder = new StageHolder();
        this.topicEventSubscriber = new TopicEventSubscriber() {
            @Override
            public void handleOnNext(TopicUIEvent item) {
                if (TopicUIEvent.isRefreshTopicEvent(item)) {
                    Platform.runLater(() -> countMessages());
                }
            }
        };

        this.partitionEventSubscriber = new PartitionEventSubscriber() {
            @Override
            public void handleOnNext(PartitionUIEvent item) {
                if (PartitionUIEvent.isRefreshPartitionEvent(item)) {
                    Platform.runLater(() -> countMessages());
                }
            }
        };
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
        // TODO: need to think about pagination for message table, may implement it if it's a good option
        initPollingOptionsUI();
        kafkaMessageTable.configureMessageTable(serDesHelper);
        isPollingMsgProgressIndicator.visibleProperty().bindBidirectional(isPolling);
        isPollingMsgProgressIndicator.managedProperty().bindBidirectional(isPolling);
        isPolling.addListener((observable, oldValue, newValue) -> {
            pollMessagesBtn.setText(newValue ? AppConstant.STOP_POLLING_TEXT : AppConstant.POLL_MESSAGES_TEXT);
        });
        kafkaMessageTable.addFilterListener((filter) -> {
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
        List.of(startTimestampPicker, endTimestampPicker).forEach(picker -> picker.setDayCellFactory(param -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        }));
        List.of(startTimestampPicker, endTimestampPicker).forEach(picker -> picker.valueProperty().addListener(event -> countMessages()));
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
            ViewUtils.highlightJsonInCodeArea(newText, schemaTextArea, false, AvroUtil.OBJECT_MAPPER, jsonHighlighter);
        });
        isLiveUpdateCheckBox.setOnAction(event -> {
            if (!isLiveUpdateCheckBox.isSelected() && isPolling.get()) {
                isPolling.set(false);
            }
        });
        isLiveUpdateCheckBox.disableProperty()
                .bind(msgPollingPosition.valueProperty().map(
                        v -> v != KafkaConsumerService.MessagePollingPosition.LAST));

        endTimestampPicker.disableProperty().bind(isLiveUpdateCheckBox.selectedProperty());
        endTimestampPicker.disableProperty().addListener((obs, oldValue, newValue) -> {
            countMessages();
        });
//        endTimestampLabel.setVisible(false);
//        endTimestampLabel.setManaged(false);
//        endTimestampPicker.setVisible(false);
//        endTimestampPicker.setManaged(false);
    }

    public void setStage(Stage stage) {
        this.stageHolder.setStage(stage);
    }

    public void cacheMessages(TreeItem oldValue) {
        treeMsgTableItemCache.put(oldValue, KafkaMessageView.MessageTableState.builder()
                .items(kafkaMessageTable.getItems())
                .filter(kafkaMessageTable.getFilter())
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
                KafkaMessageView.MessageTableState messageTableState = treeMsgTableItemCache.get(newValue);
                ObservableList<KafkaMessageTableItem> msgItems = FXCollections.observableArrayList(messageTableState.getItems());
                kafkaMessageTable.setItems(msgItems, false);
                kafkaMessageTable.configureSortAndFilterForMessageTable(messageTableState.getFilter(), messagePollingPosition);
            } else {
                kafkaMessageTable.setItems(FXCollections.observableArrayList(), false);
                kafkaMessageTable.configureSortAndFilterForMessageTable(new Filter("", false), messagePollingPosition);
            }
            countMessages();
        } else if (newValue instanceof KafkaPartitionTreeItem<?>) {
            KafkaPartition partition = (KafkaPartition) newValue.getValue();
            Predicate<KafkaMessageTableItem> partitionPredicate = item -> item.getPartition() == partition.id();
            TreeItem<?> topicTreeItem = newValue.getParent();
            ObservableList<KafkaMessageTableItem> msgItems = FXCollections.observableArrayList();
            KafkaMessageView.MessageTableState messageTableState = null;
            if (treeMsgTableItemCache.containsKey(newValue)) {
                messageTableState = treeMsgTableItemCache.get(newValue);
                msgItems = FXCollections.observableArrayList(messageTableState.getItems());
            } else if (treeMsgTableItemCache.containsKey(topicTreeItem)) {
                messageTableState = treeMsgTableItemCache.get(topicTreeItem);
                msgItems = FXCollections.observableArrayList(treeMsgTableItemCache.get(topicTreeItem).getItems());
            }
            Filter filter = new Filter("", false);
            kafkaMessageTable.setItems(msgItems, false);
            if (messageTableState != null) {
                filter = messageTableState.getFilter();
            }
            kafkaMessageTable.configureSortAndFilterForMessageTable(filter, messagePollingPosition, partitionPredicate);
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
            ViewUtils.showAlertDialog(Alert.AlertType.WARNING, "Please choose a topic or partition to poll messages", null, ButtonType.OK);
            return;
        }
        String schema = schemaTextArea.getText();
        ObservableList<KafkaMessageTableItem> list = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        kafkaMessageTable.setItems(list);
        // clear message cache for partitions
        treeMsgTableItemCache.forEach((treeItem, state) -> {
            if (treeItem instanceof KafkaPartitionTreeItem<?> && treeItem.getParent() == selectedTreeItem) {
                treeMsgTableItemCache.remove(treeItem);
            }
        });
        BooleanProperty firstPoll = new SimpleBooleanProperty(true);
        KafkaConsumerService.MessagePollingPosition messagePollingPosition = msgPollingPosition.getValue();
        KafkaConsumerService.PollingOptions pollingOptions =
                KafkaConsumerService.PollingOptions.builder()
                        .pollTime(DEFAULT_POLL_TIME_MS)
                        .noMessages(StringUtils.isBlank(maxMessagesTextField.getText()) ? Integer.MAX_VALUE : Integer.parseInt(maxMessagesTextField.getText()))
                        .startTimestamp(getTimestamp(this.startTimestampPicker))
                        .endTimestamp(this.endTimestampPicker.isDisabled() ? null : getTimestamp(this.endTimestampPicker))
                        .pollingPosition(messagePollingPosition)
                        .valueContentType(valueContentType.getValue())
                        .schema(schema)
                        .pollCallback(() -> {
                            if (firstPoll.get()) {
                                Platform.runLater(() -> kafkaMessageTable.resizeColumn());
                            }
                            Platform.runLater(() -> {
                                isBlockingAppUINeeded.set(false);
                                kafkaMessageTable.handleNumOfMsgChanged(kafkaMessageTable.getShownItems().size());
                            });
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
            kafkaMessageTable.handleNumOfMsgChanged(kafkaMessageTable.getShownItems().size());
            getTopicEventDispatcher().publishEvent(TopicUIEvent.newRefreshTopicEven(getTopic()));
        };
        Consumer<Throwable> onFailure = (exception) -> {
            isBlockingAppUINeeded.set(false);
            isPolling.set(false);
            log.error("Error when polling messages", exception);
            UIErrorHandler.showError(Thread.currentThread(), exception);
        };
        ViewUtils.runBackgroundTask(pollMsgTask, onSuccess, onFailure);
    }

    public KafkaTopic getTopic() {
        if (selectedTreeItem instanceof KafkaPartitionTreeItem<?> selectedItem) {
            return ((KafkaPartition) selectedItem.getValue()).topic();
        } else {
            return (KafkaTopic) ((KafkaTopicTreeItem<?>) selectedTreeItem).getValue();
        }
    }

    private Long getTimestamp(DateTimePicker dateTimePicker) {
        return (dateTimePicker.getValue() != null && dateTimePicker.getDateTimeValue() != null) ? ZonedDateTime.of(dateTimePicker.getDateTimeValue(), ZoneId.systemDefault()).toInstant().toEpochMilli() : null;

    }

    @FXML
    protected void addMessage() throws Exception {
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


    public void addMessage(@NonNull KafkaTopic kafkaTopic, KafkaPartition partition, String keyContentType, String valueContentType, String schema) throws Exception {

        AtomicReference<Object> ref = new AtomicReference<>();
        ViewUtils.showPopUpModal(AppConstant.ADD_MESSAGE_MODAL_FXML, "Add New Message", ref,
                Map.of("serDesHelper", serDesHelper, "valueContentType", valueContentType, "valueContentTypeComboBox", FXCollections.observableArrayList(serDesHelper.getSupportedValueSerializer()),
                        "schemaTextArea", schemaTextArea.getText()), true, true, stageHolder.getStage());
        KafkaMessage newMsg = (KafkaMessage) ref.get();
        if (newMsg != null) {
            producerUtil.sendMessage(kafkaTopic, partition, newMsg);
            getTopicEventDispatcher().publishEvent(TopicUIEvent.newRefreshTopicEven(kafkaTopic));
            if (partition != null) {
                getTopicEventDispatcher().publishEvent(PartitionUIEvent.newRefreshPartitionEven(partition));
            }
            if (isLiveUpdateCheckBox.isSelected() && isPolling.get()) {
                ViewUtils.showAlertDialog(Alert.AlertType.INFORMATION, "Added message successfully! Live-update is on, polling the messages", "Added message successfully!",
                        ButtonType.OK);
            } else {
                if (ViewUtils.confirmAlert("Added message successfully!", "Added message successfully! Do you want to poll the messages?", "Yes", "No")) {
                    if (!isPolling.get()) pollMessages();

                }
            }

        }
    }

    public EventDispatcher getTopicEventDispatcher() {
        return topicEventSubscriber.getEventDispatcher();
    }

    // Count message with end offset
    @FXML
    public void countMessages() {
        Callable<Long> callable = () -> {
            try {
                Long startTimestamp = getTimestamp(this.startTimestampPicker);
                Long endTimestamp = this.endTimestampPicker.isDisabled() ? null : getTimestamp(this.endTimestampPicker);
                if (startTimestamp != null && endTimestamp != null && endTimestamp <= startTimestamp) {
                    return 0L;
                }

                long count;
                if (selectedTreeItem instanceof KafkaPartitionTreeItem<?>) {
                    KafkaPartition partition = (KafkaPartition) selectedTreeItem.getValue();
                    String clusterName = partition.topic().cluster().getName();
                    Pair<Long, Long> partitionInfo = clusterManager.getPartitionOffsetInfo(clusterName, new TopicPartition(partition.topic().name(), partition.id()), startTimestamp, endTimestamp);
                    count = partitionInfo.getLeft() >= 0 ? (partitionInfo.getRight() - partitionInfo.getLeft()) : 0;
                } else if (selectedTreeItem instanceof KafkaTopicTreeItem<?>) {
                    KafkaTopic topic = (KafkaTopic) selectedTreeItem.getValue();
                    String clusterName = topic.cluster().getName();
                    count = clusterManager.getAllPartitionOffsetInfo(clusterName, topic.name(), startTimestamp, endTimestamp).values()
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
        ViewUtils.runBackgroundTask(callable, (count) -> countMessagesBtn.setText("Count: " + count), (e) -> {
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
