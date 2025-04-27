package com.example.mytool.service;

import com.example.mytool.manager.ClusterManager;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.producer.creator.ConsumerCreator;
import com.example.mytool.serde.AvroUtil;
import com.example.mytool.serde.SerdeUtil;
import com.example.mytool.ui.KafkaMessageTableItem;
import lombok.Builder;
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
import java.util.stream.Collectors;

import static com.example.mytool.constant.AppConstant.DEFAULT_POLL_TIME_MS;

@Slf4j
public class KafkaConsumerService {
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
        Consumer consumer = getConsumer(kafkaTopic, pollingOptions.valueContentType());
        consumer.assign(topicPartitions);
        if (pollingOptions.timestamp() != null) {
            seekOffsetWithTimestamp(consumer, kafkaTopic.getName(), topicPartitions, pollingOptions.timestamp());
        } else {
            seekOffset(consumer, topicPartitions, pollingOptions.pollingPosition(), pollingOptions.noMessages());
        }
        List<KafkaMessageTableItem> list = pollMessages(consumer, pollingOptions);
        consumer.close();
        return list;
    }

    private static Consumer getConsumer(KafkaTopic topic, String valueContentType) {
        ConsumerCreator.ConsumerCreatorConfig consumerCreatorConfig = ConsumerCreator.ConsumerCreatorConfig.builder(topic.getCluster())
                .keyDeserializer(StringDeserializer.class.getName())
                .valueDeserializer(SerdeUtil.getDeserializeClass(valueContentType))
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
                long startIndex = Math.max(consumer.position(topicPartition) - noMessages, 0);
                consumer.seek(topicPartition, startIndex);
            });
        }
    }

    public List<KafkaMessageTableItem> pollMessages(Consumer<String, Object> consumer, PollingOptions pollingOptions) {
        int pollingTimeMs = Objects.requireNonNullElse(pollingOptions.pollTime(), DEFAULT_POLL_TIME_MS);
        List<KafkaMessageTableItem> messages = new ArrayList<>();
        while (true) {
            ConsumerRecords<String, Object> consumerRecords = consumer.poll(Duration.ofMillis(pollingTimeMs));
            if (consumerRecords.isEmpty()) break;

            for (ConsumerRecord<String, Object> record : consumerRecords) {
                try {
                    KafkaMessageTableItem message = createMessageItem(record, pollingOptions);
                    messages.add(message);
                } catch (Exception e) {
                    log.error("Error processing record: key={}, partition={}, offset={}",
                            record.key(), record.partition(), record.offset(), e);
                }
            }
        }

        return filterAndSortMessages(messages, pollingOptions);
    }

    private KafkaMessageTableItem createMessageItem
            (ConsumerRecord<String, Object> record, PollingOptions pollingOptions) throws IOException {
        String key = record.key() != null ? record.key() : "";
        String value = SerdeUtil.SERDE_AVRO.equals(pollingOptions.valueContentType())
                ? AvroUtil.deserializeAsJsonString((byte[]) record.value(), pollingOptions.schema())
                : (String) record.value();

        String timestamp = Instant.ofEpochMilli(record.timestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .toString();

        return new KafkaMessageTableItem(record.partition(), record.offset(), key, value, timestamp);
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
                                        String schema) {
    }
}
