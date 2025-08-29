package io.github.nhtuan10.mykafkatool.model.kafka;

import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;

public record KafkaTopic(String name, KafkaCluster cluster) {
    @Override
    public String toString() {
        return "▤ " + name;
    }
}
