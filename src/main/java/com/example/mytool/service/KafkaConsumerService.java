package com.example.mytool.service;

import com.example.mytool.MainController;
import com.example.mytool.manager.ClusterManager;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.ui.KafkaMessageTableItem;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;

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

public class KafkaConsumerService {
    public List<KafkaMessageTableItem> consumeMessages(KafkaPartition partition, Integer pollTime, Integer noMessages, Long timestamp, MainController.MessagePollingPosition pollingPosition) {
        Consumer<String, String> consumer = ClusterManager.getInstance().getConsumer(partition.getTopic().getCluster());
        TopicPartition partitionInfo = new TopicPartition(partition.getTopic().getName(), partition.getId());
        Set<TopicPartition> topicPartitions = Set.of(partitionInfo);
        consumer.assign(topicPartitions);
        if (timestamp != null) {
//            TopicPartition tp = new TopicPartition(partition.getTopic().getName(), partition.getId());
//            OffsetAndTimestamp offsetAndTimestamp = consumer.offsetsForTimes(Map.of(tp, timestamp)).get(tp);
//            if (offsetAndTimestamp != null) {
//                consumer.seek(tp, offsetAndTimestamp.offset());
//            } else return new ArrayList<>();
            seekOffsetWithTimestamp(consumer, partition.getTopic().getName(), topicPartitions, timestamp);
        } else {
            seekOffset(consumer, topicPartitions, pollingPosition, noMessages);
        }
        List<KafkaMessageTableItem> list = pollMessages(consumer, pollTime, noMessages, pollingPosition);
        consumer.close();
        return list;
    }


    public List<KafkaMessageTableItem> consumeMessages(KafkaTopic kafkaTopic, Integer pollTime, Integer noMessages, Long timestamp, MainController.MessagePollingPosition pollingPosition) throws ExecutionException, InterruptedException, TimeoutException {
        String topicName = kafkaTopic.getName();

        Consumer<String, String> consumer = ClusterManager.getInstance().getConsumer(kafkaTopic.getCluster());
        Set<TopicPartition> topicPartitions = ClusterManager.getInstance().getTopicPartitions(kafkaTopic.getCluster().getName(), topicName)
                .stream().map(p -> new TopicPartition(topicName, p.partition())).collect(Collectors.toSet());
        consumer.assign(topicPartitions);
        if (timestamp != null) {
            seekOffsetWithTimestamp(consumer, topicName, topicPartitions, timestamp);
        } else {
            seekOffset(consumer, topicPartitions, pollingPosition, noMessages);
        }
        List<KafkaMessageTableItem> list = pollMessages(consumer, pollTime, noMessages, pollingPosition);
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

    public List<KafkaMessageTableItem> pollMessages(Consumer<String, String> consumer, Integer pollTime, Integer maxMessage, MainController.MessagePollingPosition pollingPosition) {
        pollTime = pollTime != null ? pollTime : DEFAULT_POLL_TIME;
        List<KafkaMessageTableItem> list = new ArrayList<>();
        int count = 0;
//        int remainingRetry = 1;
        while (true) {
//            ConsumerRecords<Long,String> consumerRecords = consumer.poll(1000);
//                ConsumerRecords<String,String> consumerRecords = consumer.poll(Duration.ofMillis(150));
            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(pollTime));
            if (consumerRecords.isEmpty()) break;
            consumerRecords.forEach(record -> {
//                    System.out.println("Record key " + record.key());
//                    System.out.println("Record value " + record.value());
//                    System.out.println("Record partition " + record.partition());
//                    System.out.println("Record offset " + record.offset());
                list.add(new KafkaMessageTableItem(record.partition(), record.offset(), record.key(), record.value(), Instant.ofEpochMilli(record.timestamp()).atZone(ZoneId.systemDefault()).toLocalDateTime().toString()));
            });
//            consumer.commitAsync();
            count += consumerRecords.count();
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
