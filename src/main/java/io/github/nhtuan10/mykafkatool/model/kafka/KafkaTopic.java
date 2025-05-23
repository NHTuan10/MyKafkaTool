package io.github.nhtuan10.mykafkatool.model.kafka;

public record KafkaTopic(String name, KafkaCluster cluster) {
    @Override
    public String toString() {
        return name;
    }
}
