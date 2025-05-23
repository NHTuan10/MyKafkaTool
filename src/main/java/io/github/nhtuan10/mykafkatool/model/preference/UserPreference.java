package io.github.nhtuan10.mykafkatool.model.preference;

import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;

import java.util.List;

public record UserPreference(List<KafkaCluster> connections) {

}
