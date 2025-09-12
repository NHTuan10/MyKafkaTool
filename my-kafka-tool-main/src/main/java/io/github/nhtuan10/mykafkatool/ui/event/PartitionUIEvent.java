package io.github.nhtuan10.mykafkatool.ui.event;

import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;

public record PartitionUIEvent(KafkaPartition partition, Action action) implements UIEvent {
    public static boolean isRefreshPartitionEvent(PartitionUIEvent event) {
        return event.action() == Action.REFRESH_PARTITION;
    }

    public static PartitionUIEvent newRefreshPartitionEven(KafkaPartition partition) {
        return new PartitionUIEvent(partition, Action.REFRESH_PARTITION);
    }

    public static boolean isMessagePollingEvent(PartitionUIEvent event) {
        return event.action() == Action.MESSAGE_POLLING;
    }

    public static PartitionUIEvent newMessagePollingEvent(KafkaPartition partition) {
        return new PartitionUIEvent(partition, Action.MESSAGE_POLLING);
    }
}
