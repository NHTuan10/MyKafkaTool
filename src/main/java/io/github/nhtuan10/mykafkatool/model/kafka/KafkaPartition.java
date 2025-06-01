package io.github.nhtuan10.mykafkatool.model.kafka;

/**
 * @param topic private int size;
 */
public record KafkaPartition(int id, KafkaTopic topic) {

    @Override
    public String toString() {
        return "Partition " + id;
    }
}
