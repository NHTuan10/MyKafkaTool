package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;


@Slf4j
public class ConsumerGroupView extends BorderPane {
    @FXML
    private ConsumerGroupIDTable consumerGroupIDTable;

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

    public void loadCG(ConsumerGroupTreeItem selected, BooleanProperty isBlockingAppUINeeded) {
        this.consumerGroupIDTable.loadCG(selected, isBlockingAppUINeeded);
    }
}
