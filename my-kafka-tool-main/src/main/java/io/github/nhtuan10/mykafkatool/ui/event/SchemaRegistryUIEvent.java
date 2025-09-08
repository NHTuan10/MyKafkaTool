package io.github.nhtuan10.mykafkatool.ui.event;

import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;

public record SchemaRegistryUIEvent(KafkaCluster cluster, Action action, boolean useCache) implements UIEvent {
    public static boolean isBackgroundRefreshEvent(SchemaRegistryUIEvent event) {
        return event.action() == Action.BACKGROUND_REFRESH_SCHEMA_REGISTRY;
    }

    public static SchemaRegistryUIEvent newBackgroundRefreshEven(KafkaCluster cluster) {
        return new SchemaRegistryUIEvent(cluster, Action.BACKGROUND_REFRESH_SCHEMA_REGISTRY, false);
    }

    public static boolean isRefreshEvent(SchemaRegistryUIEvent event) {
        return event.action() == Action.REFRESH_SCHEMA_REGISTRY;
    }

    public static SchemaRegistryUIEvent newRefreshEvent(KafkaCluster cluster) {
        return new SchemaRegistryUIEvent(cluster, Action.REFRESH_SCHEMA_REGISTRY, false);
    }

    public static SchemaRegistryUIEvent newRefreshEvent(KafkaCluster cluster, boolean useCache) {
        return new SchemaRegistryUIEvent(cluster, Action.REFRESH_SCHEMA_REGISTRY, useCache);
    }
}
