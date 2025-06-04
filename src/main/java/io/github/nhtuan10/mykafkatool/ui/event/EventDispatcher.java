package io.github.nhtuan10.mykafkatool.ui.event;

import io.github.nhtuan10.mykafkatool.ui.control.SchemaRegistryControl;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.BiPredicate;

@RequiredArgsConstructor
public class EventDispatcher {
    final SubmissionPublisher<TopicUIEvent> topicEventPublisher;
    final SubmissionPublisher<PartitionUIEvent> partitionEventPublisher;
    final SubmissionPublisher<SchemaRegistryUIEvent> schemaRegistryEventSubmissionPublisher;

    public void addTopicEventSubscriber(TopicEventSubscriber subscriber) {
        topicEventPublisher.subscribe(subscriber);
        subscriber.setEventDispatcher(this);
    }

    public void addPartitionEventSubscriber(PartitionEventSubscriber subscriber) {
        partitionEventPublisher.subscribe(subscriber);
        subscriber.setEventDispatcher(this);
    }

    public void addSchemaRegistryEventSubscriber(SchemaRegistryControl.SchemaRegistryEventSubscriber subscriber) {
        schemaRegistryEventSubmissionPublisher.subscribe(subscriber);
        subscriber.setEventDispatcher(this);
    }

    public <T> BiPredicate<Flow.Subscriber<? super T>, ? super T> getErrorHandler() {
        return (subscriber, e) -> {
            subscriber.onError(new RuntimeException(e.getClass().getSimpleName() + " event is not accepted by subscriber"));
            return false;
        };
    }

    public void publishEvent(UIEvent uiEvent) {

        switch (uiEvent) {
            case TopicUIEvent event -> this.topicEventPublisher.offer(event, this.getErrorHandler());
            case PartitionUIEvent event -> this.partitionEventPublisher.offer(event, this.getErrorHandler());
            case SchemaRegistryUIEvent event ->
                    this.schemaRegistryEventSubmissionPublisher.offer(event, this.getErrorHandler());
            default -> throw new IllegalStateException("Unexpected value: " + uiEvent);
        }
    }
}
