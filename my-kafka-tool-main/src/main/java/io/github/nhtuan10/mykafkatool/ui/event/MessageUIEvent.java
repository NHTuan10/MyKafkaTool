package io.github.nhtuan10.mykafkatool.ui.event;

import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageTableItem;

public record MessageUIEvent(KafkaMessageTableItem message, Action action) implements UIEvent {
    public static boolean isMessageSelectionEvent(MessageUIEvent event) {
        return event.action() == Action.SELECT_MESSAGE;
    }

    public static MessageUIEvent newMessageSelectionEvent(KafkaMessageTableItem message) {
        return new MessageUIEvent(message, Action.SELECT_MESSAGE);
    }
}
