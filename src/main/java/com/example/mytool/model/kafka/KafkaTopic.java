package com.example.mytool.model.kafka;

public record KafkaTopic(String name, KafkaCluster cluster) {
    @Override
    public String toString() {
        return name;
    }
}
