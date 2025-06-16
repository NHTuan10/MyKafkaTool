package io.github.nhtuan10.mykafkatool.ui.event;

import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;

import java.util.List;

public record ConsumerGroupUIEvent(String clusterName, List<String> consumerGroupIds, KafkaTopic topic,
                                   Action action) implements UIEvent {
    public static boolean isRefreshConsumerGroupEvent(ConsumerGroupUIEvent event) {
        return event.action() == Action.REFRESH_CONSUMER_GROUP;
    }

    public static ConsumerGroupUIEvent newRefreshConsumerGroupEven(String clusterName, List<String> consumerGroupIds, KafkaTopic topic) {
        return new ConsumerGroupUIEvent(clusterName, consumerGroupIds, topic, Action.REFRESH_CONSUMER_GROUP);
    }
}
