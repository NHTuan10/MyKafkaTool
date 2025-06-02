package io.github.nhtuan10.mykafkatool.ui.control;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.serdes.AvroUtil;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.event.UIEvent;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.CodeArea;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;

public class SchemaRegistryControl extends SplitPane {

    private final JsonHighlighter jsonHighlighter;

    @Setter
    private BooleanProperty isBlockingAppUINeeded;

    @Getter
    private final SchemaRegistryEventSubscriber schemaRegistryEventSubscriber;

    @FXML
    private SplitPane messageSplitPane;

    @FXML
    private SchemaEditableTableControl schemaEditableTableControl;

    @FXML
    private CodeArea schemaRegistryTextArea;

    public SchemaRegistryControl() {
        jsonHighlighter = new JsonHighlighter();
        schemaRegistryEventSubscriber = new SchemaRegistryEventSubscriber(this);
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
        schemaEditableTableControl.addEventHandler(SelectedSchemaEvent.SELECTED_SCHEMA_EVENT_TYPE,
                (event) -> schemaRegistryTextArea.replaceText(event.getData().getValue()));
    }

    public void loadAllSchema(KafkaCluster cluster) throws ExecutionException, InterruptedException {
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

    @Getter
    static class SelectedSchemaEvent extends Event {
        // Define event type
        public static final EventType<SelectedSchemaEvent> SELECTED_SCHEMA_EVENT_TYPE =
                new EventType<>(Event.ANY, "SELECTED_SCHEMA_EVENT_TYPE");

        // Getter for custom data
        private final SimpleStringProperty data;

        public SelectedSchemaEvent(SimpleStringProperty data) {
            super(SELECTED_SCHEMA_EVENT_TYPE);
            this.data = data;
        }

        // Override copy method (required for event system)
        @Override
        public SelectedSchemaEvent copyFor(Object newSource, EventTarget newTarget) {
            return new SelectedSchemaEvent(this.data);
        }

        // Override event type method (required for event system)
        @Override
        public EventType<? extends SelectedSchemaEvent> getEventType() {
            return SELECTED_SCHEMA_EVENT_TYPE;
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    public static class SchemaRegistryEventSubscriber implements Flow.Subscriber<UIEvent.SchemaRegistryEvent> {
        protected Flow.Subscription subscription = null;
        private final SchemaRegistryControl schemaRegistryControl;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(UIEvent.SchemaRegistryEvent item) {
            try {
                schemaRegistryControl.loadAllSchema(item.cluster());
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            subscription.request(1);
        }


        @Override
        public void onError(Throwable throwable) {
            log.error("Error when refresh schema registry", throwable);
            throw new RuntimeException(throwable);
        }

        @Override
        public void onComplete() {
            log.info("Topic refresh subscription complete");
        }
    }

}
