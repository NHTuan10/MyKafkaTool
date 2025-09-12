package io.github.nhtuan10.mykafkatool.ui.event;

import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;

public record TopicUIEvent(KafkaTopic topic, Action action) implements UIEvent {
    public static boolean isRefreshTopicEvent(TopicUIEvent event) {
        return event.action() == Action.REFRESH_TOPIC;
    }

    public static TopicUIEvent newRefreshTopicEvent(KafkaTopic topic) {
        return new TopicUIEvent(topic, Action.REFRESH_TOPIC);
    }

    public static boolean isMessagePollingEvent(TopicUIEvent event) {
        return event.action() == Action.MESSAGE_POLLING;
    }

    public static TopicUIEvent newMessagePollingEvent(KafkaTopic topic) {
        return new TopicUIEvent(topic, Action.MESSAGE_POLLING);
    }
}
