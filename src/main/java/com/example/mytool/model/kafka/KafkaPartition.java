package com.example.mytool.model.kafka;

/**
 * @param topic private int size;
 */
public record KafkaPartition(int id, KafkaTopic topic) {
    //    private final TopicPartitionInfo partitionInfo;

    @Override
    public String toString() {
        return "Partition " + id;
    }
}
