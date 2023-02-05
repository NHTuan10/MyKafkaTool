package com.example.mytool.model.preference;

import com.example.mytool.model.kafka.KafkaCluster;

import java.util.List;

public record UserPreference(List<KafkaCluster> connections) {

}
