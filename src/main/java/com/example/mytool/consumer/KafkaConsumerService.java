package com.example.mytool.consumer;

import com.example.mytool.consumer.creator.ConsumerCreator;
import com.example.mytool.exception.DeserializationException;
import com.example.mytool.manager.ClusterManager;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.serdes.AvroUtil;
import com.example.mytool.serdes.SerdeUtil;
import com.example.mytool.ui.KafkaMessageTableItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.example.mytool.constant.AppConstant.DEFAULT_POLL_TIME_MS;

@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final SerdeUtil serdeUtil;

    public List<KafkaMessageTableItem> consumeMessages(KafkaPartition partition, PollingOptions pollingOptions) {
        Set<TopicPartition> topicPartitions = Set.of(new TopicPartition(partition.getTopic().getName(), partition.getId()));
        return consumeMessagesFromPartitions(partition.getTopic(), topicPartitions, pollingOptions);

    }

    public List<KafkaMessageTableItem> consumeMessages(KafkaTopic kafkaTopic, PollingOptions pollingOptions) throws ExecutionException, InterruptedException, TimeoutException {
        String topicName = kafkaTopic.getName();
        Set<TopicPartition> topicPartitions = ClusterManager.getInstance().getTopicPartitions(kafkaTopic.getCluster().getName(), topicName)
                .stream().map(p -> new TopicPartition(topicName, p.partition())).collect(Collectors.toSet());
        return consumeMessagesFromPartitions(kafkaTopic, topicPartitions, pollingOptions);
    }

    private List<KafkaMessageTableItem> consumeMessagesFromPartitions(KafkaTopic kafkaTopic, Set<TopicPartition> topicPartitions, PollingOptions pollingOptions) {
        ConsumerCreator.ConsumerCreatorConfig consumerCreatorConfig = ConsumerCreator.ConsumerCreatorConfig.builder(kafkaTopic.getCluster())
                .keyDeserializer(StringDeserializer.class)
                .valueDeserializer(serdeUtil.getDeserializeClass(pollingOptions.valueContentType()))
                .build();
        Map<String, Object> consumerProps = ConsumerCreator.buildConsumerConfigs(consumerCreatorConfig);
        Consumer consumer = ClusterManager.getInstance().getConsumer(consumerProps);
        consumer.assign(topicPartitions);
        if (pollingOptions.timestamp() != null) {
            seekOffsetWithTimestamp(consumer, kafkaTopic.getName(), topicPartitions, pollingOptions.timestamp());
        } else {
            seekOffset(consumer, topicPartitions, pollingOptions.pollingPosition(), pollingOptions.noMessages());
        }
        List<KafkaMessageTableItem> list = pollMessages(consumer, consumerProps, pollingOptions);
        consumer.close();
        return list;
    }

    private Consumer getConsumer(KafkaTopic topic, String valueContentType) {
        ConsumerCreator.ConsumerCreatorConfig consumerCreatorConfig = ConsumerCreator.ConsumerCreatorConfig.builder(topic.getCluster())
                .keyDeserializer(StringDeserializer.class)
                .valueDeserializer(serdeUtil.getDeserializeClass(valueContentType))
                .build();
        return ClusterManager.getInstance().getConsumer(consumerCreatorConfig);
    }

    private static void seekOffsetWithTimestamp(Consumer<String, String> consumer, String topicName, Set<TopicPartition> topicPartitions, Long timestamp) {
        Map<TopicPartition, Long> partitionTimestampMap = topicPartitions.stream()
                .collect(Collectors.toMap(p -> new TopicPartition(topicName, p.partition()), p -> timestamp));
        consumer.offsetsForTimes(partitionTimestampMap)
                .forEach((tp, offsetAndTimestamp) -> {
                    if (offsetAndTimestamp != null) {
                        consumer.seek(tp, offsetAndTimestamp.offset());
                    } else
                        consumer.seekToEnd(List.of(tp));
                });
    }

    private static void seekOffset(Consumer<String, String> consumer, Set<TopicPartition> topicPartitions, MessagePollingPosition pollingPosition, Integer noMessages) {
        if (pollingPosition == MessagePollingPosition.FIRST) {
//                consumer.subscribe(Collections.singleton(topicName));
            consumer.seekToBeginning(topicPartitions);
        } else if (pollingPosition == MessagePollingPosition.LAST) {
//                consumer.subscribe(Collections.singleton(topicName));
//                Set<TopicPartition> partitionSet = consumer.assignment();
            consumer.seekToEnd(topicPartitions);
            topicPartitions.forEach(topicPartition -> {
                if (consumer.position(topicPartition) < noMessages) {
                    consumer.seekToBeginning(List.of(topicPartition));
                } else {
                    consumer.seek(topicPartition, consumer.position(topicPartition) - noMessages);
                }
            });
        }
    }

    public List<KafkaMessageTableItem> pollMessages(Consumer<String, Object> consumer, Map<String, Object> consumerProps, PollingOptions pollingOptions) {
        int pollingTimeMs = Objects.requireNonNullElse(pollingOptions.pollTime(), DEFAULT_POLL_TIME_MS);
//        List<KafkaMessageTableItem> allMessages = new ArrayList<>();
        ObservableList<KafkaMessageTableItem> messageObservableList = FXCollections.observableArrayList();
        boolean firstPoll = true;
        int emptyPullCountDown = 2;
        while (true) {
            ConsumerRecords<String, Object> consumerRecords = consumer.poll(Duration.ofMillis(pollingTimeMs));
            PollCallback pollCallback = pollingOptions.pollCallback().get();
            if (!pollCallback.isPolling().get()) break;
//            if (firstPoll) {
            messageObservableList = pollCallback.resultObservableList();
//                firstPoll = false;
//            }
            List<KafkaMessageTableItem> polledMessages = handleConsumerRecords(pollingOptions, consumerRecords, consumerProps);
            if (!polledMessages.isEmpty()) {
                messageObservableList.addAll(polledMessages);
            }
            if (consumerRecords.isEmpty()) emptyPullCountDown--;
            if (emptyPullCountDown == 0) break;
        }

        return filterAndSortMessages(messageObservableList, pollingOptions);
    }

    private List<KafkaMessageTableItem> handleConsumerRecords(PollingOptions pollingOptions, ConsumerRecords<String, Object> consumerRecords, Map<String, Object> consumerProps) {
        List<KafkaMessageTableItem> messages = new ArrayList<>();
        for (ConsumerRecord<String, Object> record : consumerRecords) {
            try {
                KafkaMessageTableItem message = createMessageItem(record, pollingOptions, consumerProps);
                messages.add(message);
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
        String value = serdeUtil.deserializeToJsonString(record,
                pollingOptions.valueContentType,
                record.headers(), consumerProps, false);

        String timestamp = Instant.ofEpochMilli(record.timestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .toString();

        return new KafkaMessageTableItem(record.partition(), record.offset(), key, value, timestamp, pollingOptions.valueContentType(), record.headers(), pollingOptions.schema(), false);
    }

    private KafkaMessageTableItem createErrorMessageItem
            (ConsumerRecord<String, Object> record, PollingOptions pollingOptions) {

        String timestamp = Instant.ofEpochMilli(record.timestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .toString();
        String displayValue = "";
        try {
            displayValue = AvroUtil.toString(record.value());
        } catch (IOException e) {
            log.error("Error when display value: " + record.value(), e);
        }
        return new KafkaMessageTableItem(record.partition(),
                record.offset(),
                record.key() != null ? record.key() : "",
                "Error when deserialize message: " + displayValue,
                timestamp,
                pollingOptions.valueContentType(),
                record.headers(),
                "",
                true);
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

    public enum MessagePollingPosition {
        FIRST, LAST;

        @Override
        public String toString() {
            return StringUtils.capitalize(this.name());
        }
    }

    @Builder
    public static record PollingOptions(Integer pollTime, Integer noMessages, Long timestamp,
                                        MessagePollingPosition pollingPosition, String valueContentType,
                                        String schema,
                                        java.util.function.Supplier<PollCallback> pollCallback) {
    }

    public static record PollCallback(ObservableList<KafkaMessageTableItem> resultObservableList,
                                      AtomicBoolean isPolling) {
    }
}
