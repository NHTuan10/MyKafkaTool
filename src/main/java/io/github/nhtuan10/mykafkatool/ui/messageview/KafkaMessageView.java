package io.github.nhtuan10.mykafkatool.ui.messageview;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.consumer.KafkaConsumerService;
import io.github.nhtuan10.mykafkatool.dagger.AppScoped;
import io.github.nhtuan10.mykafkatool.dagger.DaggerAppComponent;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@AppScoped
public class KafkaMessageView extends SplitPane {
    @Delegate
    private final KafkaMessageViewController controller;

    public KafkaMessageView() {
//        this.clusterManager = null;
//        this.serDesHelper = null;
//        this.producerUtil = null;
//        this.kafkaConsumerService = null;
//        this.jsonHighlighter = null;
//        this.stageHolder = null;
//        this.topicEventSubscriber = null;
//
//        this.partitionEventSubscriber = null;
//        this.stageHolder = new StageHolder();
//        this.topicEventSubscriber = new TopicEventSubscriber() {
//            @Override
//            public void handleOnNext(TopicUIEvent item) {
//                if (TopicUIEvent.isRefreshTopicEvent(item)) {
//                    Platform.runLater(() -> countMessages());
//                }
//            }
//        };
//
//        this.partitionEventSubscriber = new PartitionEventSubscriber() {
//            @Override
//            public void handleOnNext(PartitionUIEvent item) {
//                if (PartitionUIEvent.isRefreshPartitionEvent(item)) {
//                    Platform.runLater(() -> countMessages());
//                }
//            }
//        };
//        FXMLLoader fxmlLoader = DaggerAppComponent.create().loader(MyKafkaToolApplication.class.getResource(
//        "message-view.fxml"));

//        this.clusterManager = ClusterManager_Factory.create().get();
//        this.serDesHelper = AppModule_SerDesHelperFactory.create().get();
//        this.producerUtil = ProducerUtil_Factory.create();
//        this.kafkaConsumerService = KafkaConsumerService_Factory.create()
//        this.jsonHighlighter = jsonHighlighter;
//        this.stageHolder = new StageHolder();
//        this.topicEventSubscriber = new TopicEventSubscriber() {
//            @Override
//            public void handleOnNext(TopicUIEvent item) {
//                if (TopicUIEvent.isRefreshTopicEvent(item)) {
//                    Platform.runLater(() -> countMessages());
//                }
//            }
//        };
//
//        this.partitionEventSubscriber = new PartitionEventSubscriber() {
//            @Override
//            public void handleOnNext(PartitionUIEvent item) {
//                if (PartitionUIEvent.isRefreshPartitionEvent(item)) {
//                    Platform.runLater(() -> countMessages());
//                }
//            }
//        };

        FXMLLoader fxmlLoader = new FXMLLoader(MyKafkaToolApplication.class.getResource(
                "message-view.fxml"));
        this.controller = DaggerAppComponent.create().kafkaMessageViewController();
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this.controller);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
//         DaggerAppComponent.create().kafkaMessageView();
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class MessageTableState {
        ObservableList<KafkaMessageTableItem> items;
        Filter filter;
        //        Comparator<KafkaMessageTableItem> comparator;
        KafkaConsumerService.PollingOptions pollingOptions;
    }
}
