package com.example.mytool.model.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.kafka.common.TopicPartitionInfo;

@Data
@AllArgsConstructor
public class KafkaPartition {
    private final int id;
    //    private int size;
    private final KafkaTopic topic;
    private final TopicPartitionInfo partitionInfo;

    @Override
    public String toString() {
        return "Partition " + id;
    }
}
