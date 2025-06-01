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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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


    private StringProperty filterMsgTextProperty = new SimpleStringProperty("");

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
    }

    private void initPollingOptionsUI() {
//        pollTimeTextField.setText(String.valueOf(DEFAULT_POLL_TIME_MS));
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

    public void switchTopicOrPartition(TreeItem oldValue, TreeItem newValue) {
        treeMsgTableItemCache.put(oldValue, MessageView.MessageTableState.builder()
                .items(messageTable.getItems())
                .filter(messageTable.getFilter())
                .build());
        this.selectedTreeItem = newValue;
        KafkaConsumerService.MessagePollingPosition messagePollingPosition = msgPollingPosition.getValue();
        isPolling.set(false);
        if (newValue instanceof KafkaTopicTreeItem<?> selectedItem) {
            // if some clear msg table
            if (treeMsgTableItemCache.containsKey(newValue)) {
                MessageView.MessageTableState messageTableState = treeMsgTableItemCache.get(newValue);
                ObservableList<KafkaMessageTableItem> msgItems = FXCollections.observableArrayList(messageTableState.getItems());
//                    allMsgTableItems.setAll(msgItems);
//                    configureSortAndFilterForMessageTable(messageTableState.getFilter());
                messageTable.setItems(msgItems, false);
                messageTable.configureSortAndFilterForMessageTable(messageTableState.getFilter(), messagePollingPosition);
            } else {
//                    messageTable.setItems(FXCollections.emptyObservableList());
                messageTable.setItems(FXCollections.observableArrayList(), false);
                messageTable.configureSortAndFilterForMessageTable(new Filter("", false), messagePollingPosition);
            }
            countMessages();
        } else if (newValue instanceof KafkaPartitionTreeItem<?> selectedItem) {
            KafkaPartition partition = (KafkaPartition) selectedItem.getValue();
            Predicate<KafkaMessageTableItem> partitionPredicate = item -> item.getPartition() == partition.id();
            TreeItem<?> topicTreeItem = selectedItem.getParent();
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
//                    allMsgTableItems.setAll(msgItems);
            if (messageTableState != null) {
                filter = messageTableState.getFilter();
//                    this.filterMsgTextProperty.set(filter.getFilterText());
//                    this.regexFilterToggleBtn.setSelected(filter.isRegexFilter());
//        this.allMsgTableItems.setAll(list);
//        Comparator defaultComparator = Comparator.comparing(KafkaMessageTableItem::getTimestamp).reversed();
//                    ObservableList<KafkaMessageTableItem> filteredList = this.allMsgTableItems
//                            .filtered(item -> item.getPartition() == partition.id())
//                            .filtered(isMsgTableItemMatched(filter));
//                    SortedList<KafkaMessageTableItem> sortedList = new SortedList<>(filteredList);
//                    sortedList.comparatorProperty().bind(messageTable.comparatorProperty());
//                    messageTable.setItems(sortedList);
//                    configureSortAndFilterForMessageTable(messageTableState.getFilterText());
            }
            messageTable.configureSortAndFilterForMessageTable(filter, messagePollingPosition, partitionPredicate);
        }
    }

    @FXML
    protected void pollMessages() {
        if (isPolling.get()) {
            isPolling.set(false);
//            pullMessagesBtn.setText(AppConstant.POLL_MESSAGES_TEXT);
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
//        ObservableList<KafkaMessageTableItem> list = messageTable.getItems();
//        allMsgTableItems =  list;
//        filterMsgTextField.setOnKeyPressed(e -> {
//            if (e.getCode().equals(KeyCode.ENTER)) {
//                configureSortAndFilterForMessageTable(list, filterMsgTextProperty.get());
//            }
//        });
        KafkaConsumerService.MessagePollingPosition messagePollingPosition = msgPollingPosition.getValue();
//        filterMsgTextField.textProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                Filter filter = new Filter(filterMsgTextField.getText(), regexFilterToggleBtn.isSelected());
//                messageTable.configureSortAndFilterForMessageTable(filter, messagePollingPosition);
//                Optional.ofNullable(treeMsgTableItemCache.get(clusterTree.getSelectionModel().getSelectedItem())).ifPresent(t -> t.setFilter(filter));
//            }
//        });
//        regexFilterToggleBtn.selectedProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                Filter filter = new Filter(filterMsgTextField.getText(), regexFilterToggleBtn.isSelected());
//
//                messageTable.configureSortAndFilterForMessageTable(filter, messagePollingPosition);
//                Optional.ofNullable(treeMsgTableItemCache.get(clusterTree.getSelectionModel().getSelectedItem())).ifPresent(t -> t.setFilter(filter));
//            }
//        });
//        messageTable.configureSortAndFilterForMessageTable(new Filter(filterMsgTextProperty.get(), regexFilterToggleBtn.isSelected()), messagePollingPosition);
//        messageTable.setItems(list);
//        if (maxMessagesTextField.getText().isEmpty()) {
//            maxMessagesTextField.setText(String.valueOf(DEFAULT_MAX_POLL_RECORDS));
//        }
        KafkaConsumerService.PollingOptions pollingOptions =
                KafkaConsumerService.PollingOptions.builder()
//                        .pollTime(Integer.parseInt(pollTimeTextField.getText()))
                        .pollTime(DEFAULT_POLL_TIME_MS)
                        .noMessages(StringUtils.isBlank(maxMessagesTextField.getText()) ? Integer.MAX_VALUE : Integer.parseInt(maxMessagesTextField.getText()))
                        .startTimestamp(getPollStartTimestamp())
                        .pollingPosition(messagePollingPosition)
                        .valueContentType(valueContentType.getValue())
                        .schema(schema)
                        .pollCallback(() -> {
                            isBlockingAppUINeeded.set(false);
                            Platform.runLater(() -> messageTable.handleNumOfMsgChanged(messageTable.getItems().size()));
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
        isBlockingAppUINeeded.set(true);
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
                    log.error("Error when polling messages", e);
                    throw new RuntimeException(e);
                }
            }

            return null;
        };
        Consumer<Void> onSuccess = (val) -> {
            isBlockingAppUINeeded.set(false);
            isPolling.set(false);
            messageTable.handleNumOfMsgChanged(messageTable.getItems().size());
//            allMsgTableItems.setAll(list);
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
