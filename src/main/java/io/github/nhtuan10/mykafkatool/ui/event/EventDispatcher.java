package io.github.nhtuan10.mykafkatool.ui.event;

import io.github.nhtuan10.mykafkatool.ui.control.SchemaRegistryControl;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.SubmissionPublisher;

@RequiredArgsConstructor
public class EventDispatcher {
    final SubmissionPublisher<UIEvent.TopicEvent> topicEventPublisher;
    final SubmissionPublisher<UIEvent.SchemaRegistryEvent> schemaRegistryEventSubmissionPublisher;

    public void addTopicEventSubscriber(TopicEventSubscriber subscriber) {
        topicEventPublisher.subscribe(subscriber);
    }

    public void addSchemaRegistryEventSubscriber(SchemaRegistryControl.SchemaRegistryEventSubscriber subscriber) {
        schemaRegistryEventSubmissionPublisher.subscribe(subscriber);
    }

    public void publishEvent(UIEvent uiEvent) {
        switch (uiEvent) {
            case UIEvent.TopicEvent event -> this.topicEventPublisher.offer(event, (subscriber, e) -> {
                subscriber.onError(new RuntimeException("Topic refresh event is not accepted by subscriber"));
                return false;
            });
            case UIEvent.SchemaRegistryEvent event ->
                    this.schemaRegistryEventSubmissionPublisher.offer(event, (subscriber, e) -> {
                        subscriber.onError(new RuntimeException("Topic refresh event is not accepted by subscriber"));
                        return false;
                    });
            default -> throw new IllegalStateException("Unexpected value: " + uiEvent);
        }
    }
}
