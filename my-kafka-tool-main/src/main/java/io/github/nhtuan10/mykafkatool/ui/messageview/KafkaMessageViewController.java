package io.github.nhtuan10.mykafkatool.ui.messageview;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.api.serdes.PluggableDeserializer;
import io.github.nhtuan10.mykafkatool.configuration.annotation.RichTextFxObjectMapper;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.consumer.KafkaConsumerService;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.producer.ProducerUtil;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.StageHolder;
import io.github.nhtuan10.mykafkatool.ui.UIErrorHandler;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.control.DateTimePicker;
import io.github.nhtuan10.mykafkatool.ui.event.*;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaPartitionTreeItem;
import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicTreeItem;
import io.github.nhtuan10.mykafkatool.ui.util.ModalUtils;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import io.github.nhtuan10.mykafkatool.userpreference.UserPreferenceRepo;
import io.github.nhtuan10.mykafkatool.util.PersistableConcurrentHashMap;
import io.github.nhtuan10.mykafkatool.util.Utils;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.TopicPartition;
import org.fxmisc.richtext.CodeArea;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static io.github.nhtuan10.mykafkatool.constant.AppConstant.DEFAULT_MAX_POLL_RECORDS;
import static io.github.nhtuan10.mykafkatool.constant.AppConstant.DEFAULT_POLL_TIME_MS;

@Slf4j
//TODO: consider to refactor this class
public class KafkaMessageViewController {
    //    @Inject
    private final ClusterManager clusterManager;
    //    @Inject
    private final KafkaConsumerService kafkaConsumerService;
    //    @Inject
    private final ProducerUtil producerUtil;
    //    @Inject
    private final SerDesHelper serDesHelper;
    //    @Inject
    private final JsonHighlighter jsonHighlighter;

    private final ObjectMapper objectMapper;

    private final StageHolder stageHolder;

    private final BooleanProperty isPolling;

    private final BooleanProperty isBlockingAppUINeeded;

    private TreeItem selectedTreeItem;

    private KafkaTopic kafkaTopic;

    private KafkaPartition kafkaPartition;

    private final EventDispatcher eventDispatcher;

    @Getter
    private TopicEventSubscriber topicEventSubscriber;

    @Getter
    private PartitionEventSubscriber partitionEventSubscriber;

    @Getter
    private EventSubscriber<ApplicationUIEvent> appReadyEventSubscriber;

    private final Map<String, KafkaMessageView.MessageTableState> treeItemToMessageTableStateMap;

//    private final Map<String, KafkaMessageView.MessageTableState> persitableMsgTableStateMap;

    @Getter
    private MessageEventSubscriber messageEventSubscriber;

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

    @FXML
    private CodeArea valueTextArea;

    @FXML
    private Pane valueSchemaContainer;

    //    @Inject

    @Inject
    public KafkaMessageViewController(ClusterManager clusterManager, SerDesHelper serDesHelper, ProducerUtil producerUtil,
                                      KafkaConsumerService kafkaConsumerService, JsonHighlighter jsonHighlighter,
                                      @RichTextFxObjectMapper ObjectMapper objectMapper, EventDispatcher eventDispatcher) {
//        clusterManager = ClusterManager.getInstance();
//        serDesHelper = initSerDeserializer();
        this.clusterManager = clusterManager;
        this.serDesHelper = serDesHelper;
        this.producerUtil = producerUtil;
        this.kafkaConsumerService = kafkaConsumerService;
        this.jsonHighlighter = jsonHighlighter;
        this.objectMapper = objectMapper;
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

        this.appReadyEventSubscriber = new EventSubscriber<>() {
            @Override
            protected void handleOnNext(ApplicationUIEvent item) {
                if (ApplicationUIEvent.isApplicationReadyEvent(item)) {
                    log.info("Application is ready");
                }
            }
        };

        this.eventDispatcher = eventDispatcher;
        isPolling = new SimpleBooleanProperty(false);
        isBlockingAppUINeeded = new SimpleBooleanProperty(false);
        treeItemToMessageTableStateMap = new PersistableConcurrentHashMap<>(UserPreferenceRepo.getDefaultUserPrefDir() + "/message-view-user-pref.data");
//        treeItemToMessageTableStateMap = new ConcurrentHashMap<>();
//        persitableMsgTableStateMap = new PersistableConcurrentHashMap<>(UserPreferenceRepo.getDefaultUserPrefDir() + "/message-view-user-pref.data");
    }

    public void viewOrReproduceMessage(SerDesHelper serDesHelper, KafkaMessageTableItem rowData, boolean reproduce, boolean withHeaders) {
//        TreeItem selectedTreeItemBeforeAdding = this.selectedTreeItem;
//        String valueContentType = rowData.getValueContentType();
        String valueContentType = serDesHelper.getPluggableDeserialize(rowData.getValueContentType()).getCorrespondingSerializerName(Utils.convertKafkaMessage(rowData));
        Map<String, Object> msgModalFieldMap = new HashMap<>();
        msgModalFieldMap.put("serDesHelper", serDesHelper);
        msgModalFieldMap.put("keyTextArea", rowData.getKey());
        msgModalFieldMap.put("valueTextArea", rowData.getValue());
        // TODO: choose serializer depends on the deserializer
//        msgModalFieldMap.put("valueContentType", rowData.getValueContentType());
        msgModalFieldMap.put("schemaTextArea", rowData.getSchema());
        msgModalFieldMap.put("schemaList", rowData.getSchemaList());
        msgModalFieldMap.put("kafkaTopic", kafkaTopic);
        msgModalFieldMap.put("kafkaPartition", kafkaPartition);
        msgModalFieldMap.put("valueContentType", valueContentType);
        if (reproduce) {
            if ( withHeaders) {
                msgModalFieldMap.put("headerTable", FXCollections.observableArrayList(KafkaMessageTable.mapToMsgHeaderTableItem(rowData.getHeaders())));
            }
        }
        else {
            msgModalFieldMap.put("headerTable", FXCollections.observableArrayList(KafkaMessageTable.mapToMsgHeaderTableItem(rowData.getHeaders())));
            msgModalFieldMap.put("valueContentTypeComboBox", FXCollections.observableArrayList(valueContentType));
        }

        try {
            AtomicReference<Object> count = new AtomicReference<>(0);
            ModalUtils.showPopUpModal(AppConstant.ADD_MESSAGE_MODAL_FXML, reproduce ? "Add Message" : "View Message", count, msgModalFieldMap, reproduce, true, stageHolder.getStage(), false);
//            msgPollingPrompt(count, selectedTreeItemBeforeAdding);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    private SerDesHelper initSerDeserializer() {
//        StringSerializer stringSerializer = new StringSerializer();
//        StringDeserializer stringDeserializer = new StringDeserializer();
//        ByteArraySerializer byteArraySerializer = new ByteArraySerializer();
//        ByteArrayDeserializer byteArrayDeserializer = new ByteArrayDeserializer();
//        SchemaRegistryAvroSerializer schemaRegistryAvroSerializer = new SchemaRegistryAvroSerializer();
//        SchemaRegistryAvroDeserializer schemaRegistryAvroDeserializer = new SchemaRegistryAvroDeserializer();
//        return new SerDesHelper(
//                ImmutableMap.of(stringSerializer.getName(), stringSerializer,
//                        byteArraySerializer.getName(), byteArraySerializer,
//                        schemaRegistryAvroSerializer.getName(), schemaRegistryAvroSerializer),
//                ImmutableMap.of(stringDeserializer.getName(), stringDeserializer,
//                        byteArrayDeserializer.getName(), byteArrayDeserializer,
//                        schemaRegistryAvroDeserializer.getName(), schemaRegistryAvroDeserializer
//                )
//        );
//    }

    @FXML
    void initialize() {
        // TODO: need to think about pagination for message table, may implement it if it's a good option
        initPollingOptionsUI();
        kafkaMessageTable.setParentController(this);
        kafkaMessageTable.configureMessageTable(serDesHelper);
        isPollingMsgProgressIndicator.visibleProperty().bindBidirectional(isPolling);
        isPollingMsgProgressIndicator.managedProperty().bindBidirectional(isPolling);
        isPolling.addListener((observable, oldValue, newValue) -> {
            pollMessagesBtn.setText(newValue ? AppConstant.STOP_POLLING_TEXT : AppConstant.POLL_MESSAGES_TEXT);
        });
        kafkaMessageTable.addFilterListener((filter) -> {
            if (selectedTreeItem instanceof KafkaTopicTreeItem<?> topicTreeItem) {
                treeItemToMessageTableStateMap.forEach((treeItem, state) -> {
                    if (isChildPartition(treeItem, topicTreeItem)) {
                        state.setFilter(filter);
                    }
                });
            }
        });
        this.messageEventSubscriber = new MessageEventSubscriber(valueTextArea, objectMapper, jsonHighlighter);
    }

    private boolean isChildPartition(String partitionTreeItemKey, KafkaTopicTreeItem<?> topicTreeItem) {
        for (Object child : topicTreeItem.getChildren()) {
            String childKey = buildKeyForState((TreeItem<?>) child);
            if (child instanceof KafkaPartitionTreeItem<?> && childKey != null && childKey.equals(partitionTreeItemKey)) {
                return true;
            }
        }
        return false;
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
            valueSchemaContainer.setVisible(deserializer.mayNeedUserInputForSchema());
            isPolling.set(false);
        });
        valueSchemaContainer.setVisible(serDesHelper.getPluggableDeserialize(valueContentType.getValue()).mayNeedUserInputForSchema());
        schemaTextArea.textProperty().addListener((obs, oldText, newText) -> {
            ViewUtils.setValueAndHighlightJsonInCodeArea(newText, schemaTextArea, false, objectMapper, jsonHighlighter);
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
        String key = buildKeyForState(oldValue);
        if (key != null) {
            treeItemToMessageTableStateMap.put(key, KafkaMessageView.MessageTableState.builder()
                    .items(kafkaMessageTable.getItems())
                    .filter(kafkaMessageTable.getFilter().copy())
                    .pollingOptions(getPollingOptionsBuilder().build())
                    .build());
        }
    }

    public String buildKeyForState(TreeItem treeItem) {
        if (treeItem instanceof KafkaTopicTreeItem<?>) {
            KafkaTopic topic = (KafkaTopic) treeItem.getValue();
            return "topic:" + topic.cluster().getId() + ":" + topic.name();
        } else if (treeItem instanceof KafkaPartitionTreeItem<?>) {
            KafkaPartition partition = (KafkaPartition) treeItem.getValue();
            KafkaTopic topic = partition.topic();
            return "partition:" + topic.cluster().getId() + topic.name() + partition.id();
        }
        return null;
    }

    public void switchTopicOrPartition(TreeItem newValue) {
        if (!(newValue instanceof KafkaTopicTreeItem<?> || newValue instanceof KafkaPartitionTreeItem<?>)) {
            return;
        }
        ViewUtils.setValueAndHighlightJsonInCodeArea("", valueTextArea, false, objectMapper, jsonHighlighter);
        this.selectedTreeItem = newValue;
        KafkaConsumerService.MessagePollingPosition messagePollingPosition = msgPollingPosition.getValue();
        isPolling.set(false);
        String key = buildKeyForState(newValue);
        if (newValue instanceof KafkaTopicTreeItem<?>) {
            this.kafkaTopic = (KafkaTopic) newValue.getValue();
            this.kafkaPartition = null;
            // if some clear msg table
            if (treeItemToMessageTableStateMap.containsKey(key)) {
                KafkaMessageView.MessageTableState messageTableState = treeItemToMessageTableStateMap.get(key);
                if (messageTableState.getItems() == null)
                    messageTableState.setItems(FXCollections.observableArrayList());
                ObservableList<KafkaMessageTableItem> msgItems = FXCollections.observableArrayList(messageTableState.getItems());
                kafkaMessageTable.setItems(msgItems, false);
                kafkaMessageTable.configureSortAndFilterForMessageTable(messageTableState.getFilter(), messagePollingPosition);
                fillPollingOptionControls(messageTableState.getPollingOptions());
            } else {
                kafkaMessageTable.setItems(FXCollections.observableArrayList(), false);
                kafkaMessageTable.configureSortAndFilterForMessageTable(new Filter(), messagePollingPosition);
                fillPollingOptionControlsWithDefaultValues();
            }
            countMessages();
        } else if (newValue instanceof KafkaPartitionTreeItem<?>) {
            KafkaPartition partition = (KafkaPartition) newValue.getValue();
            this.kafkaPartition = partition;
            this.kafkaTopic = partition.topic();
            Predicate<KafkaMessageTableItem> partitionPredicate = item -> item.getPartition() == partition.id();
            TreeItem<?> topicTreeItem = newValue.getParent();
            ObservableList<KafkaMessageTableItem> msgItems = FXCollections.observableArrayList();
            KafkaMessageView.MessageTableState messageTableState = null;
            if (treeItemToMessageTableStateMap.containsKey(key)) {
                messageTableState = treeItemToMessageTableStateMap.get(key);
                if (messageTableState.getItems() == null)
                    messageTableState.setItems(FXCollections.observableArrayList());
                msgItems = FXCollections.observableArrayList(messageTableState.getItems());
            } else if (treeItemToMessageTableStateMap.containsKey(buildKeyForState(topicTreeItem))) {
                String topicKey = buildKeyForState(topicTreeItem);
                messageTableState = treeItemToMessageTableStateMap.get(topicKey);
                if (messageTableState.getItems() == null)
                    messageTableState.setItems(FXCollections.observableArrayList());
                msgItems = FXCollections.observableArrayList(messageTableState.getItems());
            }
            Filter filter = new Filter();
            kafkaMessageTable.setItems(msgItems, false);
            if (messageTableState != null) {
                filter = messageTableState.getFilter();
                fillPollingOptionControls(messageTableState.getPollingOptions());
            } else {
                fillPollingOptionControlsWithDefaultValues();
            }
            kafkaMessageTable.configureSortAndFilterForMessageTable(filter, messagePollingPosition, partitionPredicate);
            countMessages();
        }
    }

    @FXML
    void pollMessages() {
        if (isPolling.get()) {
            isPolling.set(false);
            return;
        }
        cacheMessages(this.selectedTreeItem);
        if (!(selectedTreeItem instanceof KafkaTopicTreeItem<?>)
                && !(selectedTreeItem instanceof KafkaPartitionTreeItem<?>)) {
            ModalUtils.showAlertDialog(Alert.AlertType.WARNING, "Please choose a topic or partition to poll messages", null, ButtonType.OK);
            return;
        }
//        String schema = schemaTextArea.getText();
        ObservableList<KafkaMessageTableItem> list = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        kafkaMessageTable.setItems(list);
        // clear message cache for partitions
        treeItemToMessageTableStateMap.forEach((treeItem, state) -> {
            if (selectedTreeItem instanceof KafkaTopicTreeItem<?> kafkaTopicTreeItem && isChildPartition(treeItem, kafkaTopicTreeItem)) {
//            if (treeItem instanceof KafkaPartitionTreeItem<?> && treeItem.getParent() == selectedTreeItem) {
                treeItemToMessageTableStateMap.remove(treeItem);
            }
        });
//        BooleanProperty firstPoll = new SimpleBooleanProperty(true);
        KafkaTopic kafkaTopic;
        if (selectedTreeItem instanceof KafkaPartitionTreeItem<?> selectedItem) {
            KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
            kafkaTopic = partition.topic();
        } else {
            KafkaTopicTreeItem<?> selectedItem = (KafkaTopicTreeItem<?>) selectedTreeItem;
            kafkaTopic = (KafkaTopic) selectedItem.getValue();
        }
        KafkaConsumerService.PollingOptions pollingOptions = getPollingOptionsBuilder()
                .kafkaTopic(kafkaTopic)
                .pollCallback(() -> {
//                            if (firstPoll.get()) {
//                                Platform.runLater(() -> kafkaMessageTable.resizeColumn());
//                            }
                    Platform.runLater(() -> {
                        isBlockingAppUINeeded.set(false);
                        kafkaMessageTable.handleNumOfMsgChanged(kafkaMessageTable.getShownItems().size());
                    });
                    return new KafkaConsumerService.PollCallback(list, isPolling);
                }).build();
//        KafkaConsumerService.MessagePollingPosition messagePollingPosition = msgPollingPosition.getValue();
//        KafkaConsumerService.PollingOptions pollingOptions =
//                KafkaConsumerService.PollingOptions.builder()
//                        .pollTime(DEFAULT_POLL_TIME_MS)
//                        .noMessages(StringUtils.isBlank(maxMessagesTextField.getText()) ? Integer.MAX_VALUE : Integer.parseInt(maxMessagesTextField.getText()))
//                        .startTimestamp(ViewUtils.getTimestamp(this.startTimestampPicker))
//                        .endTimestamp(this.endTimestampPicker.isDisabled() ? null : ViewUtils.getTimestamp(this.endTimestampPicker))
//                        .pollingPosition(messagePollingPosition)
//                        .valueContentType(valueContentType.getValue())
//                        .schema(schema)
//                        .pollCallback(() -> {
////                            if (firstPoll.get()) {
////                                Platform.runLater(() -> kafkaMessageTable.resizeColumn());
////                            }
//                            Platform.runLater(() -> {
//                                isBlockingAppUINeeded.set(false);
//                                kafkaMessageTable.handleNumOfMsgChanged(kafkaMessageTable.getShownItems().size());
//                            });
//                            return new KafkaConsumerService.PollCallback(list, isPolling);
//                        })
//                        .isLiveUpdate(!isLiveUpdateCheckBox.isDisabled() && isLiveUpdateCheckBox.isSelected())
//                        .build();

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

    public KafkaConsumerService.PollingOptions.PollingOptionsBuilder getPollingOptionsBuilder() {
        return KafkaConsumerService.PollingOptions.builder()
                .pollTime(DEFAULT_POLL_TIME_MS)
                .noMessages(StringUtils.isBlank(maxMessagesTextField.getText()) ? Integer.MAX_VALUE : Integer.parseInt(maxMessagesTextField.getText()))
                .startTimestamp(ViewUtils.getTimestamp(this.startTimestampPicker))
                .endTimestamp(this.endTimestampPicker.isDisabled() ? null : ViewUtils.getTimestamp(this.endTimestampPicker))
                .pollingPosition(msgPollingPosition.getValue())
                .valueContentType(valueContentType.getValue())
                .schema(schemaTextArea.getText())
                .isLiveUpdate(!isLiveUpdateCheckBox.isDisabled() && isLiveUpdateCheckBox.isSelected());
    }

    private void fillPollingOptionControls(KafkaConsumerService.PollingOptions pollingOptions) {
        maxMessagesTextField.setText((pollingOptions.noMessages() == null || pollingOptions.noMessages() == Integer.MAX_VALUE) ? "" : pollingOptions.noMessages().toString());
        setDateTimePickerValue(startTimestampPicker, pollingOptions.startTimestamp());
        setDateTimePickerValue(endTimestampPicker, pollingOptions.endTimestamp());
        msgPollingPosition.setValue(pollingOptions.pollingPosition());
        valueContentType.setValue(pollingOptions.valueContentType());
//        schemaTextArea.replaceText(pollingOptions.schema());
        ViewUtils.setValueAndHighlightJsonInCodeArea(pollingOptions.schema(), schemaTextArea, true, objectMapper, jsonHighlighter);
        isLiveUpdateCheckBox.setSelected(pollingOptions.isLiveUpdate());
    }

    private KafkaConsumerService.PollingOptions defaultPollingOptions() {
        return KafkaConsumerService.PollingOptions.builder()
                .noMessages(DEFAULT_MAX_POLL_RECORDS)
                .pollingPosition(msgPollingPosition.getValue())
                .valueContentType(valueContentType.getItems().get(0))
                .pollingPosition(KafkaConsumerService.MessagePollingPosition.LAST).build();
    }

    private void fillPollingOptionControlsWithDefaultValues() {
        fillPollingOptionControls(defaultPollingOptions());
    }

    private void setDateTimePickerValue(DateTimePicker dateTimePicker, Long epochMillis) {
        dateTimePicker.setDateTimeValue(epochMillis != null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()) : null);
    }

    public KafkaTopic getTopic() {
        if (selectedTreeItem instanceof KafkaPartitionTreeItem<?> selectedItem) {
            return ((KafkaPartition) selectedItem.getValue()).topic();
        } else {
            return (KafkaTopic) ((KafkaTopicTreeItem<?>) selectedTreeItem).getValue();
        }
    }

    @FXML
    void addMessage() throws Exception {
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
        TreeItem selectedTreeItemBeforeAdding = this.selectedTreeItem;
        AtomicReference<Object> ref = new AtomicReference<>(0);
        Map<String, Object> propertiesMap = new HashMap<>();
        propertiesMap.put("serDesHelper", serDesHelper);
        propertiesMap.put("valueContentType", valueContentType);
//        propertiesMap.put("valueContentTypeComboBox", FXCollections.observableArrayList(serDesHelper.getSupportedValueSerializer()));
        propertiesMap.put("schemaTextArea", schemaTextArea.getText());
        propertiesMap.put("kafkaTopic", kafkaTopic);
        propertiesMap.put("kafkaPartition", partition);
        ModalUtils.showPopUpModal(AppConstant.ADD_MESSAGE_MODAL_FXML, "Add New Message", ref,
                propertiesMap, true, true, stageHolder.getStage(), true);
        msgPollingPrompt(ref, selectedTreeItemBeforeAdding);
    }

    private void msgPollingPrompt(AtomicReference<Object> ref, TreeItem selectedTreeItemBeforeAdding) {
        Integer noNewMsgs = (Integer) ref.get();
        // if the same topic and partition is selected after user close the dialog, then ask users if they want to poll the messages
        if (noNewMsgs != null && noNewMsgs > 0 && selectedTreeItemBeforeAdding == this.selectedTreeItem) {
//            if (isLiveUpdateCheckBox.isSelected() && isPolling.get()) {
//                ModalUtils.showAlertDialog(Alert.AlertType.INFORMATION, "Added message successfully! Live-update is on, polling the messages", "Added message successfully!",
//                        ButtonType.OK);
//            } else {
            if (ModalUtils.confirmAlert("Added message successfully!", "Added %s message successfully! Do you want to poll to get the new messages?".formatted(noNewMsgs), "Yes", "No")) {
                if (!isPolling.get()) pollMessages();
            }
//            }

        }
    }

    public EventDispatcher getTopicEventDispatcher() {
        return this.eventDispatcher;
    }

    // Count message with end offset
    @FXML
    void countMessages() {
        Callable<Long> callable = () -> {
            try {
                Long startTimestamp = ViewUtils.getTimestamp(this.startTimestampPicker);
                Long endTimestamp = this.endTimestampPicker.isDisabled() ? null : ViewUtils.getTimestamp(this.endTimestampPicker);
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

    @Slf4j
    @RequiredArgsConstructor
    public static class MessageEventSubscriber extends EventSubscriber<MessageUIEvent> {
        private final CodeArea valueTextArea;
        private final ObjectMapper objectMapper;
        private final JsonHighlighter jsonHighlighter;

        @Override
        public void handleOnNext(MessageUIEvent item) {
            if (MessageUIEvent.isMessageSelectionEvent(item)) {
                Platform.runLater(() -> {
                    ViewUtils.setValueAndHighlightJsonInCodeArea(item.message().getValue(), valueTextArea, true, objectMapper, jsonHighlighter);
                });
            }
        }


        @Override
        public void onError(Throwable throwable) {
            log.error("Error when select message", throwable);
        }

        @Override
        public void onComplete() {
            log.info("Message selection is complete");
        }
    }
}
