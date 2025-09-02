package io.github.nhtuan10.mykafkatool.model.kafka;

/**
 * @param topic private int size;
 */
public record KafkaPartition(int id, KafkaTopic topic) {

    @Override
    public String toString() {
        if (id == -1){
            return "<AUTO>";
        }
        else {
            return "Partition " + id;
        }
    }
}
