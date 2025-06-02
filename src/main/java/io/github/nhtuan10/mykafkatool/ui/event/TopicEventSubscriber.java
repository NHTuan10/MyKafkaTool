package io.github.nhtuan10.mykafkatool.ui.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Flow;

@Slf4j
@RequiredArgsConstructor
public abstract class TopicEventSubscriber implements Flow.Subscriber<UIEvent.TopicEvent> {
    protected Flow.Subscription subscription = null;
//    private final TopicOrPartitionPropertyView topicOrPartitionPropertyView;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        log.error("Error when refresh topic", throwable);
    }

    @Override
    public void onComplete() {
        log.info("Topic refresh subscription complete");
    }
}
