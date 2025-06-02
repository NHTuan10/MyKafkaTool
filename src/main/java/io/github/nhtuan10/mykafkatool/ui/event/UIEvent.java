package io.github.nhtuan10.mykafkatool.ui.event;

import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;

public interface UIEvent {
    enum Action {
        REFRESH_TOPIC, REFRESH_SCHEMA_REGISTRY
    }

    record TopicEvent(KafkaTopic topic, Action action) implements UIEvent {
    }

    record SchemaRegistryEvent(KafkaCluster cluster, Action action) implements UIEvent {
    }
}
