package io.github.nhtuan10.mykafkatool.ui.event;

import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;

public record SchemaRegistryUIEvent(KafkaCluster cluster, Action action) implements UIEvent {
}
