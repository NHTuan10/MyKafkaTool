package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.event.UIEvent;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

public class TopicOrPartitionPropertyView extends AnchorPane {
//    BooleanProperty isBlockingAppUINeeded;

//    ReadOnlyBooleanProperty isShownOnWindow;

    @Getter
    private final TopicEventSubscriber topicEventSubscriber;

    @FXML
    private TopicOrPartitionPropertyTable topicOrPartitionPropertyTable;

    @FXML
    private TopicPartitionsTable kafkaPartitionsTable;

    @FXML
    private TitledPane partitionsTitledPane;

    private final StringProperty totalMessagesInTheTopicStringProperty;

    @FXML
    private Label totalMessagesInTheTopicLabel;

    public TopicOrPartitionPropertyView() {
        topicEventSubscriber = new TopicEventSubscriber(this);
        totalMessagesInTheTopicStringProperty = new SimpleStringProperty("0 Messages");
        FXMLLoader fxmlLoader = new FXMLLoader(MyKafkaToolApplication.class.getResource(
                "topic-or-partition-property-view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setStage(Stage stage) {
        topicOrPartitionPropertyTable.setStage(stage);
        kafkaPartitionsTable.setStage(stage);
    }

    @FXML
    private void initialize() {
        //TODO: Don't use Accordion, so it can show both properties & partitions panel
        totalMessagesInTheTopicLabel.textProperty().bind(totalMessagesInTheTopicStringProperty);
        partitionsTitledPane.setVisible(false);
//        this.setProperties(this.isBlockingAppUINeeded, this.isShownOnWindow);
    }

    public void loadTopicConfig(KafkaTopic topic) {
        partitionsTitledPane.setVisible(true);
        this.topicOrPartitionPropertyTable.loadTopicConfig(topic);
    }

    public void loadTopicPartitions(KafkaTopic topic) {
        this.kafkaPartitionsTable.loadTopicPartitions(topic, this.totalMessagesInTheTopicStringProperty);
    }

    public void loadPartitionConfig(KafkaPartition kafkaPartition) {
        partitionsTitledPane.setVisible(false);
        this.topicOrPartitionPropertyTable.loadPartitionConfig(kafkaPartition);
    }

    public void setProperties(BooleanProperty isBlockingAppUINeeded, ReadOnlyBooleanProperty isShownOnWindow) {
//        this.isBlockingAppUINeeded = isBlockingAppUINeeded;
//        this.isShownOnWindow = isShownOnWindow;
        this.topicOrPartitionPropertyTable.setProperties(isBlockingAppUINeeded, isShownOnWindow);
        this.kafkaPartitionsTable.setProperties(isBlockingAppUINeeded, isShownOnWindow);
    }

    @RequiredArgsConstructor
    public static class TopicEventSubscriber extends io.github.nhtuan10.mykafkatool.ui.event.TopicEventSubscriber {
        private final TopicOrPartitionPropertyView topicOrPartitionPropertyView;


        @Override
        public void onNext(UIEvent.TopicEvent item) {
            topicOrPartitionPropertyView.loadTopicConfig(item.topic());
            topicOrPartitionPropertyView.loadTopicPartitions(item.topic());
            subscription.request(1);
        }
    }
}
