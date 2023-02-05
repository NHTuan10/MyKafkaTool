package com.example.mytool.model.kafka;

import lombok.Data;

@Data
public class KafkaTopic {
    private final String name;
    private final KafkaCluster cluster;

    @Override
    public String toString() {
        return name;
    }
}
