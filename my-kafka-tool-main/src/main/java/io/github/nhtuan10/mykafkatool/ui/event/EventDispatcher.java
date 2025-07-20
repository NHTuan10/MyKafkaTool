package io.github.nhtuan10.mykafkatool.ui.event;

import io.github.nhtuan10.mykafkatool.ui.consumergroup.ConsumerGroupView;
import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageViewController;
import io.github.nhtuan10.mykafkatool.ui.schemaregistry.SchemaRegistryViewController;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.BiPredicate;

@RequiredArgsConstructor
public class EventDispatcher {
    final SubmissionPublisher<TopicUIEvent> topicEventPublisher;
    final SubmissionPublisher<PartitionUIEvent> partitionEventPublisher;
    final SubmissionPublisher<SchemaRegistryUIEvent> schemaRegistryEventPublisher;
    final SubmissionPublisher<ConsumerGroupUIEvent> consumerGroupUIEventPublisher;
    final SubmissionPublisher<MessageUIEvent> messageUIEventPublisher;

    public void addTopicEventSubscriber(TopicEventSubscriber subscriber) {
        topicEventPublisher.subscribe(subscriber);
        subscriber.setEventDispatcher(this);
    }

    public void addPartitionEventSubscriber(PartitionEventSubscriber subscriber) {
        partitionEventPublisher.subscribe(subscriber);
        subscriber.setEventDispatcher(this);
    }

    public void addSchemaRegistryEventSubscriber(SchemaRegistryViewController.SchemaRegistryEventSubscriber subscriber) {
        schemaRegistryEventPublisher.subscribe(subscriber);
        subscriber.setEventDispatcher(this);
    }

    public void addConsumerGroupEventSubscriber(ConsumerGroupView.ConsumerGroupEventSubscriber subscriber) {
        consumerGroupUIEventPublisher.subscribe(subscriber);
        subscriber.setEventDispatcher(this);
    }

    public void addMessageEventSubscriber(KafkaMessageViewController.MessageEventSubscriber subscriber) {
        messageUIEventPublisher.subscribe(subscriber);
        subscriber.setEventDispatcher(this);
    }

    public <T> BiPredicate<Flow.Subscriber<? super T>, ? super T> getErrorHandler() {
        return (subscriber, e) -> {
            subscriber.onError(new RuntimeException(e.getClass().getSimpleName() + " event is not accepted by subscriber"));
            return false;
        };
    }

    public void publishEvent(UIEvent uiEvent) {
        if (uiEvent instanceof TopicUIEvent event) {
            this.topicEventPublisher.offer(event, this.getErrorHandler());
        } else if (uiEvent instanceof PartitionUIEvent event) {
            this.partitionEventPublisher.offer(event, this.getErrorHandler());
        } else if (uiEvent instanceof SchemaRegistryUIEvent event) {
            this.schemaRegistryEventPublisher.offer(event, this.getErrorHandler());
        } else if (uiEvent instanceof ConsumerGroupUIEvent event) {
            this.consumerGroupUIEventPublisher.offer(event, this.getErrorHandler());
        } else if (uiEvent instanceof MessageUIEvent event) {
            this.messageUIEventPublisher.offer(event, this.getErrorHandler());
        } else {
            throw new IllegalStateException("Unexpected value: " + uiEvent);
        }
    }
}
