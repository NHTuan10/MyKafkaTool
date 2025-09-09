package io.github.nhtuan10.mykafkatool.ui.event;

public interface UIEvent {
    enum Action {
        REFRESH_TOPIC, REFRESH_PARTITION, REFRESH_SCHEMA_REGISTRY, REFRESH_CONSUMER_GROUP, SELECT_MESSAGE, BACKGROUND_REFRESH_SCHEMA_REGISTRY, APP_READY
    }
}
