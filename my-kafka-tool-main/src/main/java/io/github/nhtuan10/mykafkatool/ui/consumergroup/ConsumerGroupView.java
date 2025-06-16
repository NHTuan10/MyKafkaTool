package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;


@Slf4j
public class ConsumerGroupView extends BorderPane {
    @FXML
    private ConsumerTable consumerTable;
    @FXML
    private ConsumerGroupTable consumerGroupTable;
    @FXML
    private Label consumerGroupId;
    @FXML
    private ToggleButton toggleConsumerView;

    private ObjectProperty<ConsumerGroupTreeItem> consumerGroupTreeItem = new SimpleObjectProperty<>();
    BooleanProperty isBlockingAppUINeeded;
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
                if (Boolean.TRUE.equals(newVal)) {
                    this.consumerTable.loadCG(this.consumerGroupTreeItem.get(), isBlockingAppUINeeded);
                } else {
                    this.consumerGroupTable.loadCG(this.consumerGroupTreeItem.get(), isBlockingAppUINeeded);
                }
            }
        });
    }

    public void loadCG(ConsumerGroupTreeItem selected, BooleanProperty isBlockingAppUINeeded) {
        this.consumerGroupTreeItem.set(selected);
        this.isBlockingAppUINeeded = isBlockingAppUINeeded;
        this.consumerGroupId.textProperty().bind(consumerGroupTreeItem.map(ConsumerGroupTreeItem::getConsumerGroupId));
        this.consumerTable.visibleProperty().bind(toggleConsumerView.selectedProperty());
        this.consumerGroupTable.visibleProperty().bind(toggleConsumerView.selectedProperty().not());
        if (toggleConsumerView.isSelected()) {
            this.consumerTable.loadCG(selected, isBlockingAppUINeeded);
        } else {
            this.consumerGroupTable.loadCG(selected, isBlockingAppUINeeded);
        }
    }
}
