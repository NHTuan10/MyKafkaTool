package io.github.nhtuan10.mykafkatool.ui.event;

import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;

public record SchemaRegistryUIEvent(KafkaCluster cluster,  Action action) implements UIEvent {
    public static boolean isBackgroundRefreshEvent(SchemaRegistryUIEvent event) {
        return event.action() == Action.BACKGROUND_REFRESH_SCHEMA_REGISTRY;
    }

    public static SchemaRegistryUIEvent newBackgroundRefreshEven(KafkaCluster cluster) {
        return new SchemaRegistryUIEvent(cluster, Action.BACKGROUND_REFRESH_SCHEMA_REGISTRY);
    }
}
