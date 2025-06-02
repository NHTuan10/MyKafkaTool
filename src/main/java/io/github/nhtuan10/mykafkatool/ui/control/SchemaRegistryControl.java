package io.github.nhtuan10.mykafkatool.ui.control;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.serdes.AvroUtil;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class SchemaRegistryControl extends SplitPane {
    @FXML
    private SplitPane messageSplitPane;

    @FXML
    private SchemaEditableTableControl schemaEditableTableControl;

    @FXML
    private CodeArea schemaRegistryTextArea;

    private final JsonHighlighter jsonHighlighter;

    public SchemaRegistryControl() {
        jsonHighlighter = new JsonHighlighter();
        FXMLLoader fxmlLoader = new FXMLLoader(MyKafkaToolApplication.class.getResource(
                "schema-registry-view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setStage(Stage stage) {
        this.schemaEditableTableControl.setStage(stage);
    }

    @FXML
    public void initialize() {
        // TODO: Multiple version for an schema, make the schema table editable
        schemaRegistryTextArea.textProperty().addListener((obs, oldText, newText) -> {
            ViewUtil.highlightJsonInCodeArea(newText, schemaRegistryTextArea, true, AvroUtil.OBJECT_MAPPER, jsonHighlighter);
        });
        schemaEditableTableControl.addEventHandler(SchemaEditableTableControl.SelectedSchemaEvent.SELECTED_SCHEMA_EVENT_TYPE,
                (event) -> schemaRegistryTextArea.replaceText(event.getData().getValue()));
    }

    public void loadAllSchema(KafkaCluster cluster, BooleanProperty isBlockingAppUINeeded) throws ExecutionException, InterruptedException {
        schemaEditableTableControl.loadAllSchemas(cluster,
                (e) -> isBlockingAppUINeeded.set(false),
                (e) -> {
                    isBlockingAppUINeeded.set(false);
                    throw ((RuntimeException) e);
                }, isBlockingAppUINeeded);
    }

    public void refresh() throws RestClientException, IOException, ExecutionException, InterruptedException {
        schemaEditableTableControl.refresh();
    }
}
