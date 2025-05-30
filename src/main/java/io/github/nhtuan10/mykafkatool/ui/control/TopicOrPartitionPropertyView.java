package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class TopicOrPartitionPropertyView extends AnchorPane {
    @FXML
    private TopicOrPartitionPropertyTable topicConfigTable;


    @FXML
    private TopicPartitionsTable kafkaPartitionsTable;

    @FXML
    private TitledPane partitionsTitledPane;

    private final StringProperty totalMessagesInTheTopicStringProperty = new SimpleStringProperty("0 Messages");

    @FXML
    private Label totalMessagesInTheTopicLabel;

    public TopicOrPartitionPropertyView() {
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
        topicConfigTable.setStage(stage);
        kafkaPartitionsTable.setStage(stage);
    }

    @FXML
    private void initialize() {
        totalMessagesInTheTopicLabel.textProperty().bind(totalMessagesInTheTopicStringProperty
//                totalMessagesInTheTopicProperty.asString("%,d Messages")
        );
        partitionsTitledPane.setVisible(false);

    }

    public void loadTopicConfig(KafkaTopic topic, BooleanProperty isBlockingAppUINeeded) {
        partitionsTitledPane.setVisible(true);
        this.topicConfigTable.loadTopicConfig(topic, isBlockingAppUINeeded);
    }

    public void loadTopicPartitions(KafkaTopic topic, BooleanProperty isBlockingAppUINeeded) {
        this.kafkaPartitionsTable.loadTopicPartitions(topic, this.totalMessagesInTheTopicStringProperty, isBlockingAppUINeeded);
    }

    public void loadPartitionConfig(KafkaPartition kafkaPartition, BooleanProperty isBlockingAppUINeeded) {
        partitionsTitledPane.setVisible(false);
        this.topicConfigTable.loadPartitionConfig(kafkaPartition, isBlockingAppUINeeded);
    }
}
