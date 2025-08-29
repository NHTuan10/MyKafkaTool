package io.github.nhtuan10.mykafkatool.consumer;

import io.github.nhtuan10.mykafkatool.api.Config;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.consumer.creator.ConsumerCreator;
import io.github.nhtuan10.mykafkatool.exception.DeserializationException;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.model.kafka.SchemaMetadataFromRegistry;
import io.github.nhtuan10.mykafkatool.schemaregistry.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.serdes.AvroUtil;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageTableItem;
import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageTableItemFXModel;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.github.nhtuan10.mykafkatool.constant.AppConstant.DEFAULT_POLL_TIME_MS;

@Slf4j
@AppScoped
public class KafkaConsumerService {
    private final SerDesHelper serDesHelper;
    private final ClusterManager clusterManager;
    private final ConsumerCreator consumerCreator;
    private final SchemaRegistryManager schemaRegistryManager;

    private List<Consumer> consumers = Collections.synchronizedList(new ArrayList<>());

    @Inject
    public KafkaConsumerService(SerDesHelper serDesHelper, ClusterManager clusterManager, ConsumerCreator consumerCreator, SchemaRegistryManager schemaRegistryManager) {
        this.serDesHelper = serDesHelper;
        this.clusterManager = clusterManager;
        this.consumerCreator = consumerCreator;
        this.schemaRegistryManager = schemaRegistryManager;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumers.forEach(Consumer::wakeup);
        }));
    }

    public List<KafkaMessageTableItem> consumeMessages(KafkaPartition partition, PollingOptions pollingOptions) {
        Set<TopicPartition> topicPartitions = Set.of(new TopicPartition(partition.topic().name(), partition.id()));
        return consumeMessagesFromPartitions(partition.topic(), topicPartitions, pollingOptions);

    }

    public List<KafkaMessageTableItem> consumeMessages(KafkaTopic kafkaTopic, PollingOptions pollingOptions) throws ExecutionException, InterruptedException, TimeoutException {
        String topicName = kafkaTopic.name();
        Set<TopicPartition> topicPartitions = clusterManager.getTopicPartitions(kafkaTopic.cluster().getName(), topicName)
                .stream().map(p -> new TopicPartition(topicName, p.partition())).collect(Collectors.toSet());
        return consumeMessagesFromPartitions(kafkaTopic, topicPartitions, pollingOptions);
    }

    private List<KafkaMessageTableItem> consumeMessagesFromPartitions(KafkaTopic kafkaTopic, Set<TopicPartition> topicPartitions, PollingOptions pollingOptions) {
        ConsumerCreator.ConsumerCreatorConfig consumerCreatorConfig = ConsumerCreator.ConsumerCreatorConfig.builder(kafkaTopic.cluster())
                .keyDeserializer(StringDeserializer.class)
                .valueDeserializer(serDesHelper.getDeserializeClass(pollingOptions.valueContentType()))
                .build();
        Map<String, Object> consumerProps = consumerCreator.buildConsumerConfigs(consumerCreatorConfig);
        Consumer consumer = clusterManager.createConsumer(consumerProps);
        consumers.add(consumer);
        consumer.assign(topicPartitions);
        Map<TopicPartition, Pair<Long, Long>> partitionOffsetsToPoll = seekOffset(consumer, topicPartitions, pollingOptions);
        consumerProps.put(Config.AUTH_CONFIG_PROP, kafkaTopic.cluster().getAuthConfig());
        List<KafkaMessageTableItem> list = pollMessages(consumer, consumerProps, pollingOptions, partitionOffsetsToPoll);
        consumer.close();
        consumers.remove(consumer);
        return list;
    }

    private static void seekOffset(Consumer<String, String> consumer, Set<TopicPartition> topicPartitions, Long timestamp) {
        if (timestamp != null) {
            var offsetsForTime = getPartitionOffsetForTimestamp(consumer, topicPartitions, timestamp);
            offsetsForTime.forEach((tp, offset) -> consumer.seek(tp, offset));
        } else {
            topicPartitions.forEach(tp -> consumer.seekToBeginning(List.of(tp)));
        }

//        if (timestamp == null) {
//            consumer.seekToBeginning(topicPartitions);
//        }
//        else {
//            topicPartitions.forEach(topicPartition -> {
//                    consumer.seek(topicPartition, new OffsetAndMetadata(timestamp));
//            });
//        }
    }

    private static Map<TopicPartition, Long> getPartitionOffsetForTimestamp(Consumer<String, String> consumer, Set<TopicPartition> topicPartitions, long timestamp) {
        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(topicPartitions);
        Map<TopicPartition, Long> partitionTimestampMap = topicPartitions.stream()
                .collect(Collectors.toMap(p -> p, p -> timestamp));

        return consumer.offsetsForTimes(partitionTimestampMap).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Optional.ofNullable(entry.getValue()).map(OffsetAndTimestamp::offset).orElse(endOffsets.get(entry.getKey()))));
    }

    private static Map<TopicPartition, Pair<Long, Long>> seekOffset(Consumer<String, String> consumer, Set<TopicPartition> topicPartitions, PollingOptions pollingOptions) {
        int noMessages = pollingOptions.noMessages();
        Long startTimestamp = pollingOptions.startTimestamp();
        Long endTimestamp = pollingOptions.endTimestamp();

        if (pollingOptions.pollingPosition() == MessagePollingPosition.FIRST) {
//            if (startTimestamp != null) {
            seekOffset(consumer, topicPartitions, startTimestamp);
//            } else {
//                consumer.seekToBeginning(topicPartitions);
//            }
            Map<TopicPartition, Long> endOffsets = endTimestamp != null
                    ? getPartitionOffsetForTimestamp(consumer, topicPartitions, endTimestamp)
                    : consumer.endOffsets(topicPartitions);
//            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(topicPartitions);
//            long startOffset = consumer.position(partition);
            return topicPartitions.stream().collect(Collectors.toMap(tp -> tp, partition -> {
//                long endOffset = endOffsetForTs.get(partition) != null ? endOffsetForTs.get(partition) : endOffsets.get(partition);
                long startOffset = consumer.position(partition);
                return Pair.of(startOffset, Math.min(startOffset + noMessages, endOffsets.get(partition)));
            }));
        } else {
            Map<TopicPartition, Long> endPollingOffsets;
            if (endTimestamp != null) {
                endPollingOffsets = getPartitionOffsetForTimestamp(consumer, topicPartitions, endTimestamp);
            } else {
                endPollingOffsets = consumer.endOffsets(topicPartitions);
            }

            Map<TopicPartition, Long> startPollingOffsets;
            if (startTimestamp != null) {
                startPollingOffsets = getPartitionOffsetForTimestamp(consumer, topicPartitions, startTimestamp);
            } else {
                startPollingOffsets = consumer.beginningOffsets(topicPartitions);
            }

            return topicPartitions.stream().collect(Collectors.toMap(tp -> tp,
                    tp -> {
                        long endPollingOffset = endPollingOffsets.get(tp);
                        Long newStartPollingOffset = startPollingOffsets.get(tp);
                        if (endPollingOffset >= noMessages) {
                            newStartPollingOffset = Math.max(newStartPollingOffset, endPollingOffset - noMessages);
                        }
                        consumer.seek(tp, newStartPollingOffset);
                        if (pollingOptions.isLiveUpdate()) {
                            return Pair.of(newStartPollingOffset, Long.MAX_VALUE);
                        } else {
                            return Pair.of(newStartPollingOffset, endPollingOffset);
                        }
                    }));
        }
    }

    public List<KafkaMessageTableItem> pollMessages(Consumer<String, Object> consumer, Map<String, Object> consumerProps, PollingOptions pollingOptions, Map<TopicPartition, Pair<Long, Long>> partitionOffsetsToPoll) {
        int pollingTimeMs = Objects.requireNonNullElse(pollingOptions.pollTime(), DEFAULT_POLL_TIME_MS);
        ObservableList<KafkaMessageTableItem> messageObservableList;
        Map<TopicPartition, Boolean> isAllMsgPulled = partitionOffsetsToPoll.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> (entry.getValue().getRight() - entry.getValue().getLeft() <= 0)));
        try (consumer) {
//            boolean firstPoll = true;
            ConsumerRecords<String, Object> consumerRecords = consumer.poll(Duration.ofMillis(pollingTimeMs));
            Supplier<PollCallback> pollCallbackSupplier = pollingOptions.pollCallback();
//            PollCallback pollCallback = pollingOptions.pollCallback().get();
            messageObservableList = pollCallbackSupplier.get().resultObservableList();

            while (true) {
//                ConsumerRecords<String, Object> consumerRecords = consumer.poll(Duration.ofMillis(pollingTimeMs));
//                Supplier<PollCallback> pollCallbackSupplier = pollingOptions.pollCallback();
//                if (firstPoll){
////                    pollCallback = pollCallbackSupplier.get();
//                    messageObservableList = pollCallbackSupplier.get().resultObservableList();
//                    firstPoll = false;
//                }
                if (!consumerRecords.isEmpty()) {
                    List<KafkaMessageTableItem> messages = new ArrayList<>();
                    for (ConsumerRecord<String, Object> record : consumerRecords) {
                        TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
                        long endOffset = partitionOffsetsToPoll.get(topicPartition).getRight();
                        if (record.offset() >= endOffset) {
                            isAllMsgPulled.put(topicPartition, true);
                        } else {
                            try {
                                KafkaMessageTableItem message = createMessageItem(record, pollingOptions, consumerProps);
                                messages.add(message);
                            } catch (DeserializationException e) {
                                log.error("Error processing record: key={}, partition={}, offset={}",
                                        record.key(), record.partition(), record.offset(), e);
                                KafkaMessageTableItem message = createErrorMessageItem(record, pollingOptions);
                                messages.add(message);
                            }
                            if (record.offset() == endOffset - 1) {
                                isAllMsgPulled.put(topicPartition, true);
                            }
                        }
                    }
                    if (!messages.isEmpty()) {
                        Platform.runLater(() -> messageObservableList.addAll(messages));
//                        messageObservableList.addAll(messages);
                    }
                    if (!pollCallbackSupplier.get().isPolling().get()) break;
                }
                if (isAllMsgPulled.entrySet().stream().allMatch(Map.Entry::getValue) && !pollingOptions.isLiveUpdate())
                    break;
                consumerRecords = consumer.poll(Duration.ofMillis(pollingTimeMs));
            }
        }
        return messageObservableList;
    }

    private List<KafkaMessageTableItem> handleConsumerRecords(PollingOptions pollingOptions, ConsumerRecords<String, Object> consumerRecords, Map<String, Object> consumerProps, Map<TopicPartition, Pair<Long, Long>> partitionOffsetsToPoll) {
        List<KafkaMessageTableItem> messages = new ArrayList<>();
        for (ConsumerRecord<String, Object> record : consumerRecords) {
            try {
                long endOffset = partitionOffsetsToPoll.get(new TopicPartition(record.topic(), record.partition())).getRight();
                if (record.offset() < endOffset) {
                    KafkaMessageTableItem message = createMessageItem(record, pollingOptions, consumerProps);
                    messages.add(message);
                }

            } catch (DeserializationException e) {
                log.error("Error processing record: key={}, partition={}, offset={}",
                        record.key(), record.partition(), record.offset(), e);
                KafkaMessageTableItem message = createErrorMessageItem(record, pollingOptions);
                messages.add(message);
            }
        }
        return messages;
    }

    private KafkaMessageTableItem createMessageItem
            (ConsumerRecord<String, Object> record, PollingOptions pollingOptions, Map<String, Object> consumerProps) throws DeserializationException {
        String key = record.key() != null ? record.key() : "";
        Map<String, String> others = new HashMap<>(Map.of(Config.IS_KEY_PROP, Boolean.toString(false), Config.SCHEMA_PROP, pollingOptions.schema()));
        String value = serDesHelper.deserializeToJsonString(record,
                pollingOptions.valueContentType,
                record.headers(), consumerProps, others);
        String timestamp = formatRecordTimestamp(record);
        String schema = pollingOptions.schema();
        List<SchemaMetadataFromRegistry> schemaTableItems  = new ArrayList<>();
        int schemaId = others.containsKey(Config.SCHEMA_ID) ? Integer.parseInt(others.get(Config.SCHEMA_ID)) : -1;
        if (schemaId >= 0) {
            try {
                schemaTableItems = schemaRegistryManager.getSchemasById(pollingOptions.kafkaTopic().cluster().getName(), schemaId);
                schema = schemaTableItems.get(0).schemaMetadata().getSchema();
            } catch (Exception e) {
                throw new DeserializationException("Error during fetching schema by id {}", schemaId, e);
            }
        }
        KafkaMessageTableItem kafkaMessageTableItem =  KafkaMessageTableItemFXModel.builder()
                .partition(record.partition())
                .offset(record.offset())
                .key(key)
                .value(value)
                .timestamp(timestamp)
                .serializedValueSize(record.serializedValueSize())
                .valueContentType(pollingOptions.valueContentType())
                .headers(record.headers())
                .schema(schema)
                .isErrorItem(false)
                .build();
        kafkaMessageTableItem.setSchemaList(schemaTableItems);
        return kafkaMessageTableItem;
    }

    private KafkaMessageTableItem createErrorMessageItem
            (ConsumerRecord<String, Object> record, PollingOptions pollingOptions) {

        String timestamp = formatRecordTimestamp(record);
        String displayValue = "";
        try {
            displayValue = AvroUtil.toString(record.value());
        } catch (IOException e) {
            log.error("Error when display value: {}", record.value(), e);
        }

        return KafkaMessageTableItemFXModel.builder().partition(record.partition())
                .offset(record.offset())
                .key(record.key() != null ? record.key() : "")
                .value(displayValue)
                .timestamp(timestamp)
                .serializedValueSize(record.serializedValueSize())
                .valueContentType(pollingOptions.valueContentType())
                .headers(record.headers())
                .schema("")
                .isErrorItem(true).build();
    }

    private String formatRecordTimestamp(ConsumerRecord<String, Object> record) {
        return Instant.ofEpochMilli(record.timestamp())
                .atZone(ZoneId.systemDefault())
                .toString();
    }

    private List<KafkaMessageTableItem> filterAndSortMessages
            (List<KafkaMessageTableItem> messages, PollingOptions pollingOptions) {
        if (pollingOptions.noMessages() != null) {
            messages.sort(Comparator.comparing(KafkaMessageTableItem::getTimestamp));
            if (pollingOptions.pollingPosition() == MessagePollingPosition.LAST) {
                Collections.reverse(messages);
            }
            return messages.subList(0, Math.min(messages.size(), pollingOptions.noMessages()));
        }
        return messages;
    }

    private void sortMessages(List<KafkaMessageTableItem> messages, PollingOptions pollingOptions) {
        messages.sort(Comparator.comparing(KafkaMessageTableItem::getTimestamp));
        if (pollingOptions.pollingPosition() == MessagePollingPosition.LAST) {
            Collections.reverse(messages);
        }
    }

    public enum MessagePollingPosition {
        FIRST, LAST;

        @Override
        public String toString() {
            return StringUtils.capitalize(this.name());
        }
    }

    @Builder
    public record PollingOptions(Integer pollTime, Integer noMessages, Long startTimestamp, Long endTimestamp,
                                 MessagePollingPosition pollingPosition, String valueContentType,
                                 String schema,
                                 java.util.function.Supplier<PollCallback> pollCallback, boolean isLiveUpdate, KafkaTopic kafkaTopic) {
    }

    public record PollCallback(ObservableList<KafkaMessageTableItem> resultObservableList,
                               BooleanProperty isPolling) {
    }
}
