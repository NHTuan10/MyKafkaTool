package com.example.mytool.service;

import com.example.mytool.MainController;
import com.example.mytool.manager.ClusterManager;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.producer.creator.ConsumerCreator;
import com.example.mytool.serde.SerdeUtil;
import com.example.mytool.ui.KafkaMessageTableItem;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.example.mytool.constant.AppConstant.DEFAULT_POLL_TIME;

//TODO: refactor this whole class, too many parameters in methods
public class KafkaConsumerService {
    public List<KafkaMessageTableItem> consumeMessages(KafkaPartition partition, Integer pollTime, Integer noMessages, Long timestamp, MainController.MessagePollingPosition pollingPosition, String valueContentType, String schema) {
        Set<TopicPartition> topicPartitions = Set.of(new TopicPartition(partition.getTopic().getName(), partition.getId()));
        return consumeMessagesFromPartitions(partition.getTopic(), topicPartitions, pollTime, noMessages, timestamp, pollingPosition, valueContentType, schema);

    }

    public List<KafkaMessageTableItem> consumeMessages(KafkaTopic kafkaTopic, Integer pollTime, Integer noMessages, Long timestamp, MainController.MessagePollingPosition pollingPosition, String valueContentType, String schema) throws ExecutionException, InterruptedException, TimeoutException {
        String topicName = kafkaTopic.getName();
        Set<TopicPartition> topicPartitions = ClusterManager.getInstance().getTopicPartitions(kafkaTopic.getCluster().getName(), topicName)
                .stream().map(p -> new TopicPartition(topicName, p.partition())).collect(Collectors.toSet());

        return consumeMessagesFromPartitions(kafkaTopic, topicPartitions, pollTime, noMessages, timestamp, pollingPosition, valueContentType, schema);
    }


    private static Consumer getConsumer(KafkaTopic topic, String valueContentType) {
        ConsumerCreator.ConsumerCreatorConfig consumerCreatorConfig = ConsumerCreator.ConsumerCreatorConfig.builder(topic.getCluster())
                .keyDeserializer(StringDeserializer.class.getName())
                .valueDeserializer(SerdeUtil.getDeserializeClass(valueContentType))
                .build();
        return ClusterManager.getInstance().getConsumer(consumerCreatorConfig);
    }

    private List<KafkaMessageTableItem> consumeMessagesFromPartitions(KafkaTopic kafkaTopic, Set<TopicPartition> topicPartitions, Integer pollTime, Integer noMessages, Long timestamp, MainController.MessagePollingPosition pollingPosition, String valueContentType, String schema) {
        Consumer consumer = getConsumer(kafkaTopic, valueContentType);
        consumer.assign(topicPartitions);
        if (timestamp != null) {
            seekOffsetWithTimestamp(consumer, kafkaTopic.getName(), topicPartitions, timestamp);
        } else {
            seekOffset(consumer, topicPartitions, pollingPosition, noMessages);
        }
        List<KafkaMessageTableItem> list = pollMessages(consumer, pollTime, noMessages, pollingPosition, valueContentType, schema);
        consumer.close();
        return list;
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

    private static void seekOffset(Consumer<String, String> consumer, Set<TopicPartition> topicPartitions, MainController.MessagePollingPosition pollingPosition, Integer noMessages) {
        if (pollingPosition == MainController.MessagePollingPosition.FIRST) {
//                consumer.subscribe(Collections.singleton(topicName));
            consumer.seekToBeginning(topicPartitions);
        } else if (pollingPosition == MainController.MessagePollingPosition.LAST) {
//                consumer.subscribe(Collections.singleton(topicName));
//                Set<TopicPartition> partitionSet = consumer.assignment();
            consumer.seekToEnd(topicPartitions);
            topicPartitions.forEach(topicPartition -> {
                long startIndex = Math.max(consumer.position(topicPartition) - noMessages, 0);
                consumer.seek(topicPartition, startIndex);
            });
        }
    }

    //    @SneakyThrows(IOException.class)
    public List<KafkaMessageTableItem> pollMessages(Consumer consumer, Integer pollTime, Integer maxMessage, MainController.MessagePollingPosition pollingPosition, String valueContentType, String schema) {
        pollTime = pollTime != null ? pollTime : DEFAULT_POLL_TIME;
        List<KafkaMessageTableItem> list = new ArrayList<>();
//        int count = 0;
//        int remainingRetry = 1;
        while (true) {
//            ConsumerRecords<Long,String> consumerRecords = consumer.poll(1000);
//                ConsumerRecords<String,String> consumerRecords = consumer.poll(Duration.ofMillis(150));
            ConsumerRecords consumerRecords = consumer.poll(Duration.ofMillis(pollTime));
            if (consumerRecords.isEmpty()) break;
            for (Object consumerRecord : consumerRecords) {
//                    System.out.println("Record key " + record.key());
//                    System.out.println("Record value " + record.value());
//                    System.out.println("Record partition " + record.partition());
//                    System.out.println("Record offset " + record.offset());
                try {
                    ConsumerRecord record = (ConsumerRecord) consumerRecord;
                    String key = record.key() != null ? record.key().toString() : "";
                    String value;
                    if (SerdeUtil.SERDE_AVRO.equals(valueContentType)) {
                        value = SerdeUtil.deserializeAsJsonString((byte[]) record.value(), schema);
                    } else {
                        value = (String) record.value();
                    }

                    list.add(new KafkaMessageTableItem(record.partition(), record.offset(), key, value, Instant.ofEpochMilli(record.timestamp()).atZone(ZoneId.systemDefault()).toLocalDateTime().toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
//            consumer.commitAsync();
//            count += consumerRecords.count();
//            if (maxMessage != null && count > maxMessage) break;
        }
        if (maxMessage != null) {
            list.sort((msg1, msg2) -> CharSequence.compare(msg1.getTimestamp(), msg2.getTimestamp()));
            if (pollingPosition == MainController.MessagePollingPosition.LAST)
                return list.reversed().subList(0, Math.min(list.size(), maxMessage));
            return list.subList(0, Math.min(list.size(), maxMessage));
        }

        return list;
    }
}
