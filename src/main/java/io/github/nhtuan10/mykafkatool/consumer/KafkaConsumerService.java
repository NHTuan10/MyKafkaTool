package io.github.nhtuan10.mykafkatool.consumer;

import io.github.nhtuan10.mykafkatool.consumer.creator.ConsumerCreator;
import io.github.nhtuan10.mykafkatool.exception.DeserializationException;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.serdes.AvroUtil;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import io.github.nhtuan10.mykafkatool.ui.KafkaMessageTableItem;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
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
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static io.github.nhtuan10.mykafkatool.constant.AppConstant.DEFAULT_POLL_TIME_MS;

@Slf4j
public class KafkaConsumerService {
    private final SerDesHelper serDesHelper;

    private List<Consumer> consumers = Collections.synchronizedList(new ArrayList<>());

    public KafkaConsumerService(SerDesHelper serDesHelper) {
        this.serDesHelper = serDesHelper;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumers.forEach(Consumer::wakeup);
//            consumers.clear();
        }));
    }

    public List<KafkaMessageTableItem> consumeMessages(KafkaPartition partition, PollingOptions pollingOptions) {
        Set<TopicPartition> topicPartitions = Set.of(new TopicPartition(partition.topic().name(), partition.id()));
        return consumeMessagesFromPartitions(partition.topic(), topicPartitions, pollingOptions);

    }

    public List<KafkaMessageTableItem> consumeMessages(KafkaTopic kafkaTopic, PollingOptions pollingOptions) throws ExecutionException, InterruptedException, TimeoutException {
        String topicName = kafkaTopic.name();
        Set<TopicPartition> topicPartitions = ClusterManager.getInstance().getTopicPartitions(kafkaTopic.cluster().getName(), topicName)
                .stream().map(p -> new TopicPartition(topicName, p.partition())).collect(Collectors.toSet());
        return consumeMessagesFromPartitions(kafkaTopic, topicPartitions, pollingOptions);
    }

    private List<KafkaMessageTableItem> consumeMessagesFromPartitions(KafkaTopic kafkaTopic, Set<TopicPartition> topicPartitions, PollingOptions pollingOptions) {
        ConsumerCreator.ConsumerCreatorConfig consumerCreatorConfig = ConsumerCreator.ConsumerCreatorConfig.builder(kafkaTopic.cluster())
                .keyDeserializer(StringDeserializer.class)
                .valueDeserializer(serDesHelper.getDeserializeClass(pollingOptions.valueContentType()))
                .build();
        Map<String, Object> consumerProps = ConsumerCreator.buildConsumerConfigs(consumerCreatorConfig);
        Consumer consumer = ClusterManager.getInstance().getConsumer(consumerProps);
        consumers.add(consumer);
        consumer.assign(topicPartitions);
//        if (pollingOptions.startTimestamp() != null) {
//            seekOffsetWithTimestamp(consumer, topicPartitions, pollingOptions.startTimestamp());
//        } else {
//            seekOffset(consumer, topicPartitions, pollingOptions.pollingPosition(), pollingOptions.noMessages());
//        }
        Map<TopicPartition, Pair<Long, Long>> partitionOffsetsToPoll = seekOffset(consumer, topicPartitions, pollingOptions);
        List<KafkaMessageTableItem> list = pollMessages(consumer, consumerProps, pollingOptions, partitionOffsetsToPoll);
        consumer.close();
        consumers.remove(consumer);
        return list;
    }

    private static void seekOffsetWithTimestamp(Consumer<String, String> consumer, Set<TopicPartition> topicPartitions, Long timestamp) {
        var offsetsForTime = getPartitionOffsetForTimestamp(consumer, topicPartitions, timestamp);
        offsetsForTime.forEach((tp, offsetAndTimestamp) -> {
            if (offsetAndTimestamp != null) {
                consumer.seek(tp, offsetAndTimestamp.offset());
            } else
                consumer.seekToEnd(List.of(tp));
        });
    }

    private static Map<TopicPartition, OffsetAndTimestamp> getPartitionOffsetForTimestamp(Consumer<String, String> consumer, Set<TopicPartition> topicPartitions, Long timestamp) {
        Map<TopicPartition, Long> partitionTimestampMap = topicPartitions.stream()
                .collect(Collectors.toMap(p -> p, p -> timestamp));
        var offsetsForTime = consumer.offsetsForTimes(partitionTimestampMap);
        return offsetsForTime;
    }

//    private static Map<TopicPartition, Pair<Long, Long>> seekOffset(Consumer<String, String> consumer, Set<TopicPartition> topicPartitions, MessagePollingPosition pollingPosition, Integer noMessages) {
//        if (pollingPosition == MessagePollingPosition.FIRST) {
////                consumer.subscribe(Collections.singleton(topicName));
//            consumer.seekToBeginning(topicPartitions);
//            return topicPartitions.stream().collect(Collectors.toMap(tp -> tp, partition -> Pair.of(consumer.position(partition), consumer.position(partition) + noMessages)));
//        } else {

    /// /                consumer.subscribe(Collections.singleton(topicName));
    /// /                Set<TopicPartition> partitionSet = consumer.assignment();
//            Map<TopicPartition, Pair<Long, Long>> partitionOffsetMap = new HashMap<>(topicPartitions.size());
//            consumer.seekToEnd(topicPartitions);
//            topicPartitions.forEach(topicPartition -> {
//                long endOffset = consumer.position(topicPartition);
//                if (endOffset < noMessages) {
//                    consumer.seekToBeginning(List.of(topicPartition));
//                    partitionOffsetMap.put(topicPartition, Pair.of(consumer.position(topicPartition), endOffset));
//                } else {
//                    consumer.seek(topicPartition, consumer.position(topicPartition) - noMessages);
//                    partitionOffsetMap.put(topicPartition, Pair.of(consumer.position(topicPartition), endOffset));
//                }
//            });
//            return partitionOffsetMap;
//        }
//    }
    private static Map<TopicPartition, Pair<Long, Long>> seekOffset(Consumer<String, String> consumer, Set<TopicPartition> topicPartitions, PollingOptions pollingOptions) {
        int noMessages = pollingOptions.noMessages();
        Long startTimestamp = pollingOptions.startTimestamp();

        if (pollingOptions.pollingPosition() == MessagePollingPosition.FIRST) {
//                consumer.subscribe(Collections.singleton(topicName));
            if (startTimestamp != null) {
                seekOffsetWithTimestamp(consumer, topicPartitions, startTimestamp);
            } else {
                consumer.seekToBeginning(topicPartitions);
            }
            return topicPartitions.stream().collect(Collectors.toMap(tp -> tp, partition -> Pair.of(consumer.position(partition), consumer.position(partition) + noMessages)));
        } else {
//                consumer.subscribe(Collections.singleton(topicName));
//                Set<TopicPartition> partitionSet = consumer.assignment();

            Map<TopicPartition, Pair<Long, Long>> partitionOffsetMap = new HashMap<>(topicPartitions.size());
            Map<TopicPartition, OffsetAndTimestamp> offsetsForTs = new HashMap<>(topicPartitions.size());
            if (startTimestamp != null) {
                offsetsForTs = getPartitionOffsetForTimestamp(consumer, topicPartitions, startTimestamp);
            }
            consumer.seekToEnd(topicPartitions);
            for (TopicPartition topicPartition : topicPartitions) {
                long endOffset = consumer.position(topicPartition);
                Long startTsOffset = Optional.ofNullable(offsetsForTs.get(topicPartition)).map(OffsetAndTimestamp::offset).orElse(0L);
                if (endOffset < noMessages) {
//                    consumer.seekToBeginning(List.of(topicPartition));
                    consumer.seek(topicPartition, startTsOffset);
                } else {
                    consumer.seek(topicPartition, Math.max(startTsOffset, endOffset - noMessages));
                }
                if (pollingOptions.isLiveUpdate()) {
                    partitionOffsetMap.put(topicPartition, Pair.of(consumer.position(topicPartition), Long.MAX_VALUE));
                } else {
                    partitionOffsetMap.put(topicPartition, Pair.of(consumer.position(topicPartition), endOffset));
                }
            }
            return partitionOffsetMap;
        }
    }

    public List<KafkaMessageTableItem> pollMessages(Consumer<String, Object> consumer, Map<String, Object> consumerProps, PollingOptions pollingOptions, Map<TopicPartition, Pair<Long, Long>> partitionOffsetsToPoll) {
        int pollingTimeMs = Objects.requireNonNullElse(pollingOptions.pollTime(), DEFAULT_POLL_TIME_MS);
//        List<KafkaMessageTableItem> allMessages = new ArrayList<>();
        ObservableList<KafkaMessageTableItem> messageObservableList = FXCollections.observableArrayList();
//        int emptyPullCountDown = AppConstant.EMPTY_PULL_STILL_STOP;
        Map<TopicPartition, Boolean> isAllMsgPulled = partitionOffsetsToPoll.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> false));
        try (consumer) {
            while (true) {
                ConsumerRecords<String, Object> consumerRecords = consumer.poll(Duration.ofMillis(pollingTimeMs));
                PollCallback pollCallback = pollingOptions.pollCallback().get();
                if (!pollCallback.isPolling().get()) break;
//                if (consumerRecords.isEmpty()) emptyPullCountDown--;
//                if (emptyPullCountDown <= 0 && !pollingOptions.isLiveUpdate()) break;
                if (!consumerRecords.isEmpty()) {
                    messageObservableList = pollCallback.resultObservableList();
                    List<KafkaMessageTableItem> messages = new ArrayList<>();
                    for (ConsumerRecord<String, Object> record : consumerRecords) {
                        try {
                            TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
                            long endOffset = partitionOffsetsToPoll.get(topicPartition).getRight();
                            if (record.offset() < endOffset) {
                                KafkaMessageTableItem message = createMessageItem(record, pollingOptions, consumerProps);
                                messages.add(message);
                            }
                            if (record.offset() >= endOffset - 1) {
                                isAllMsgPulled.put(topicPartition, true);
                            }

                        } catch (DeserializationException e) {
                            log.error("Error processing record: key={}, partition={}, offset={}",
                                    record.key(), record.partition(), record.offset(), e);
                            KafkaMessageTableItem message = createErrorMessageItem(record, pollingOptions);
                            messages.add(message);
                        }
                    }
                    if (!messages.isEmpty()) {
                        messageObservableList.addAll(messages);
                    }
//                    sortMessages(messageObservableList, pollingOptions);
                }
                if (isAllMsgPulled.entrySet().stream().allMatch(Map.Entry::getValue) && !pollingOptions.isLiveUpdate())
                    break;
            }
        } catch (WakeupException e) {

        }
        return messageObservableList;
//        return filterAndSortMessages(messageObservableList, pollingOptions);
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
//        String value = SerdeUtil.SERDE_AVRO.equals(pollingOptions.valueContentType())
//                ? AvroUtil.deserializeAsJsonString((byte[]) record.value(), pollingOptions.schema())
//                : (String) record.value();
//        String value = record.value().toString();
        Map<String, String> others = Map.of(SerDesHelper.IS_KEY_PROP, Boolean.toString(false), SerDesHelper.SCHEMA_PROP, pollingOptions.schema());
        String value = serDesHelper.deserializeToJsonString(record,
                pollingOptions.valueContentType,
                record.headers(), consumerProps, others);

        String timestamp = formatRecordTimestamp(record);

        return new KafkaMessageTableItem(record.partition(), record.offset(), key, value, timestamp, pollingOptions.valueContentType(), record.headers(), pollingOptions.schema(), false);
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
        return new KafkaMessageTableItem(record.partition(),
                record.offset(),
                record.key() != null ? record.key() : "",
//                "Error when deserialize message: " + displayValue,
                displayValue,
                timestamp,
                pollingOptions.valueContentType(),
                record.headers(),
                "",
                true);
    }

    private String formatRecordTimestamp(ConsumerRecord<String, Object> record) {
        return Instant.ofEpochMilli(record.timestamp())
                .atZone(ZoneId.systemDefault())
//                .toLocalDateTime()
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
    public record PollingOptions(Integer pollTime, Integer noMessages, Long startTimestamp,
                                 MessagePollingPosition pollingPosition, String valueContentType,
                                 String schema,
                                 java.util.function.Supplier<PollCallback> pollCallback, boolean isLiveUpdate) {
    }

    public record PollCallback(ObservableList<KafkaMessageTableItem> resultObservableList,
                               BooleanProperty isPolling) {
    }
}
