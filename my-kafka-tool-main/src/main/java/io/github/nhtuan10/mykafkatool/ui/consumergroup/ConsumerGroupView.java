package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.control.DateTimePicker;
import io.github.nhtuan10.mykafkatool.ui.event.ConsumerGroupUIEvent;
import io.github.nhtuan10.mykafkatool.ui.event.EventSubscriber;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Objects;


@Slf4j
public class ConsumerGroupView extends BorderPane {
    //    private ObjectProperty<ConsumerGroupTreeItem> consumerGroupTreeItem = new SimpleObjectProperty<>();
    private String clusterName;
    private ObservableList<String> consumerGroupIds = FXCollections.observableArrayList();
    @Setter
    private BooleanProperty isBlockingAppUINeeded;
    private ObjectProperty<KafkaTopic> topic = new SimpleObjectProperty<>();
    @Getter
    private final ConsumerGroupEventSubscriber consumerGroupEventSubscriber;

    @FXML
    private ConsumerTable consumerTable;
    @FXML
    private ConsumerGroupTable consumerGroupTable;
    @FXML
    private Label infoLabel;
    @FXML
    private ToggleButton toggleConsumerView;
    @FXML
    private ComboBox<CGResetOption> resetCGComboBox;
    @FXML
    private HBox resetCGHBox;
    @FXML
    private DateTimePicker resetCGStartDateTimePicker;
//    StringProperty consumerGroupId = new SimpleStringProperty();

    /// /
//    public String getConsumerGroupId() {
//        return  consumerGroupId.get();
//    }
//
//
//    public StringProperty getConsumerGroupIdProperty() {
//        return  consumerGroupId;
//    }
    public ConsumerGroupView() {
        this.consumerGroupEventSubscriber = new ConsumerGroupEventSubscriber(this);
        FXMLLoader fxmlLoader = new FXMLLoader(MyKafkaToolApplication.class.getResource(
                "consumer-group-view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    protected void initialize() {
        toggleConsumerView.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!Objects.equals(newVal, oldVal)) {
                if (this.topic.get() != null) {
                    loadCG(this.topic.get());
                } else {
                    loadCG(this.clusterName, this.consumerGroupIds.stream().toList());
                }
//                if (Boolean.TRUE.equals(newVal)) {
//                    this.consumerTable.loadCG(this.consumerGroupTreeItem.get().getClusterName(), List.of(this.consumerGroupTreeItem.get().getConsumerGroupId()), isBusy);
//                } else {
//                    this.consumerGroupTable.loadCG(topic, isBusy);
//                }
            }
        });
//        this.infoLabel.textProperty().bind(topic.map(t -> "Topic: " + t.name()).orElse("CG: " + consumerGroupTreeItem.get().getConsumerGroupId()));
//        this.infoLabel.textProperty().bind(topic.map(t -> "Topic: " + t.name())
//                .orElse(consumerGroupTreeItem.isNotNull().get() ? "CG:" + consumerGroupTreeItem.get().getConsumerGroupId() : ""));
//                .map(cg -> "CG: " + cg.())).bind(consumerGroupTreeItem.map());

        topic.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                this.infoLabel.setText("Topic: " + newVal.name());
            }
        });

        consumerGroupIds.addListener((ListChangeListener<? super String>) (change) -> {
            if (change != null && !consumerGroupIds.isEmpty()) {
                this.infoLabel.setText("Consumer Group: " + consumerGroupIds.getFirst());
            }
        });

        this.consumerTable.visibleProperty().bind(toggleConsumerView.selectedProperty());
        this.consumerGroupTable.visibleProperty().bind(toggleConsumerView.selectedProperty().not());
        resetCGComboBox.getItems().addAll(CGResetOption.values());
        resetCGHBox.visibleProperty().bind(toggleConsumerView.selectedProperty().not().and(consumerGroupTable.selectedItemProperty().isNotNull()));
        resetCGComboBox.getSelectionModel().selectFirst();
        resetCGStartDateTimePicker.visibleProperty().bind(resetCGComboBox.getSelectionModel().selectedItemProperty().isEqualTo(CGResetOption.START_TIMESTAMP));
        resetCGStartDateTimePicker.managedProperty().bind(resetCGComboBox.getSelectionModel().selectedItemProperty().isEqualTo(CGResetOption.START_TIMESTAMP));
    }

    public void loadCG(String clusterName, List<String> selectedCG) {
        this.consumerGroupIds.setAll(selectedCG);
        this.topic.set(null);
        this.clusterName = clusterName;
//        this.isBlockingAppUINeeded = isBlockingAppUINeeded;
        if (toggleConsumerView.isSelected()) {
            this.consumerTable.loadCG(clusterName, selectedCG, isBlockingAppUINeeded);
        } else {
            this.consumerGroupTable.loadCG(clusterName, selectedCG, isBlockingAppUINeeded);
        }
    }

    public void loadCG(KafkaTopic topic) {
        this.topic.set(topic);
        this.consumerGroupIds.clear();
        this.clusterName = topic.cluster().getName();
//        this.isBlockingAppUINeeded = isBlockingAppUINeeded;
        if (toggleConsumerView.isSelected()) {
            this.consumerTable.loadCG(topic, isBlockingAppUINeeded);
        } else {
            this.consumerGroupTable.loadCG(topic, isBlockingAppUINeeded);
        }
    }


    @FXML
    public void resetCG() {
        CGResetOption cgResetOption = resetCGComboBox.getSelectionModel().getSelectedItem();

        consumerGroupTable.resetCG(cgResetOption, ViewUtils.getTimestamp(resetCGStartDateTimePicker));
    }

    @Slf4j
    @RequiredArgsConstructor
    public static class ConsumerGroupEventSubscriber extends EventSubscriber<ConsumerGroupUIEvent> {
        private final ConsumerGroupView consumerGroupView;

        @Override
        public void handleOnNext(ConsumerGroupUIEvent item) {
            if (ConsumerGroupUIEvent.isRefreshConsumerGroupEvent(item)) {
                Platform.runLater(() -> {
                    if (item.topic() != null) {
                        consumerGroupView.loadCG(item.topic());
                    } else {
                        consumerGroupView.loadCG(item.clusterName(), item.consumerGroupIds());
                    }

                });
            }
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("Error when refresh schema registry", throwable);
        }

        @Override
        public void onComplete() {
            log.info("Topic refresh subscription complete");
        }
    }
}
