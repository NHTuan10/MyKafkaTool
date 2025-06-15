package io.github.nhtuan10.mykafkatool.ui.event;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Flow;

@Slf4j
public abstract class EventSubscriber<T extends UIEvent> implements Flow.Subscriber<T> {
    protected Flow.Subscription subscription = null;

    @Override
    public void onNext(T item) {
        try {
            handleOnNext(item);
        } finally {
            subscription.request(1);
        }
    }

    protected abstract void handleOnNext(T item);

    @Getter
    @Setter
    protected EventDispatcher eventDispatcher;

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
