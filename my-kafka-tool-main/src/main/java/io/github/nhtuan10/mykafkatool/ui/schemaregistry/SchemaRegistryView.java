package io.github.nhtuan10.mykafkatool.ui.schemaregistry;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;

import java.io.IOException;

public class SchemaRegistryView extends SplitPane {

    private final SchemaRegistryViewController controller;

    public SchemaRegistryView() {
        this.controller = (SchemaRegistryViewController) MyKafkaToolApplication.DAGGER_APP_COMPONENT.viewControllers().get(SchemaRegistryViewController.class);
        FXMLLoader fxmlLoader = new FXMLLoader(MyKafkaToolApplication.class.getResource(
                "schema-registry-view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this.controller);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setStage(Stage stage) {
        this.controller.setStage(stage);
    }

    public SchemaRegistryViewController.SchemaRegistryEventSubscriber getSchemaRegistryEventSubscriber() {
        return this.controller.getSchemaRegistryEventSubscriber();
    }

    public void setIsBlockingAppUINeeded(BooleanProperty setIsBlockingAppUINeeded) {
        this.controller.setIsBlockingAppUINeeded(setIsBlockingAppUINeeded);
    }
}
