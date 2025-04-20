package com.example.mytool.service;

import com.example.mytool.manager.ClusterManager;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.ui.KafkaMessageTableItem;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.example.mytool.constant.AppConstant.DEFAULT_POLL_TIME;

public class KafkaConsumerService {
    public List<KafkaMessageTableItem> consumeMessages(KafkaPartition partition, Integer pollTime, Integer maxMessage, Long timestamp) {
        Consumer<String, String> consumer = ClusterManager.getInstance().getConsumer(partition.getTopic().getCluster());
        TopicPartition partitionInfo = new TopicPartition(partition.getTopic().getName(), partition.getId());
        consumer.assign(Set.of(partitionInfo));
        if (timestamp != null) {
            TopicPartition tp = new TopicPartition(partition.getTopic().getName(), partition.getId());
            OffsetAndTimestamp offsetAndTimestamp = consumer.offsetsForTimes(Map.of(tp, timestamp)).get(tp);
            if (offsetAndTimestamp != null) {
                consumer.seek(tp, offsetAndTimestamp.offset());
            } else return new ArrayList<>();
        } else {
            consumer.seekToBeginning(Set.of(partitionInfo));
        }
        List<KafkaMessageTableItem> list = pollMessages(consumer, pollTime, maxMessage);
        consumer.close();
        return list;
    }


    public List<KafkaMessageTableItem> consumeMessages(KafkaTopic kafkaTopic, Integer pollTime, Integer maxMessage, Long timestamp) throws ExecutionException, InterruptedException, TimeoutException {
        String topicName = kafkaTopic.getName();

        Consumer<String, String> consumer = ClusterManager.getInstance().getConsumer(kafkaTopic.getCluster());
        if (timestamp != null) {
            List<TopicPartition> topicPartitions = ClusterManager.getInstance().getTopicPartitions(kafkaTopic.getCluster().getName(), topicName)
                    .stream().map(p -> new TopicPartition(topicName, p.partition())).toList();
            consumer.assign(topicPartitions);
            Map<TopicPartition, Long> partitionTimestampMap = topicPartitions.stream()
                    .collect(Collectors.toMap(p -> new TopicPartition(topicName, p.partition()), p -> timestamp));
            consumer.offsetsForTimes(partitionTimestampMap)
                    .forEach((tp, offsetAndTimestamp) -> {
                        if (offsetAndTimestamp != null) {
                            consumer.seek(tp, offsetAndTimestamp.offset());
                        } else
                            consumer.seekToEnd(List.of(tp));
                    });
        } else {
            consumer.subscribe(Collections.singleton(topicName));
            consumer.seekToBeginning(consumer.assignment());
        }
        List<KafkaMessageTableItem> list = pollMessages(consumer, pollTime, maxMessage);
        consumer.close();
        return list;
    }

    public List<KafkaMessageTableItem> pollMessages(Consumer<String, String> consumer, Integer pollTime, Integer maxMessage) {
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
            if (maxMessage != null && count > maxMessage) break;
        }
        if (maxMessage != null)
            return list.subList(0, Math.min(list.size(), maxMessage));

        return list;
    }
}
