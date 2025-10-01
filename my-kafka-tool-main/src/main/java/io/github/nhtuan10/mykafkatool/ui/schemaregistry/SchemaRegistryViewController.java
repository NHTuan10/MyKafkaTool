package io.github.nhtuan10.mykafkatool.ui.schemaregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.api.model.DisplayType;
import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;
import io.github.nhtuan10.mykafkatool.configuration.annotation.RichTextFxObjectMapper;
import io.github.nhtuan10.mykafkatool.ui.UIErrorHandler;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.Highlighter;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.ProtobufHighlighter;
import io.github.nhtuan10.mykafkatool.ui.event.EventSubscriber;
import io.github.nhtuan10.mykafkatool.ui.event.SchemaRegistryUIEvent;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import org.apache.commons.lang3.tuple.Pair;
import org.fxmisc.richtext.CodeArea;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class SchemaRegistryViewController extends SplitPane {

    private final JsonHighlighter jsonHighlighter;

    private final ObjectMapper objectMapper;

    private final ProtobufHighlighter protobufHighlighter;

    @Setter
    private BooleanProperty isBlockingAppUINeeded;

    @Getter
    private final SchemaRegistryEventSubscriber schemaRegistryEventSubscriber;

    @FXML
    private SplitPane messageSplitPane;

    @FXML
    private SchemaEditableTable schemaEditableTable;

    @FXML
    private CodeArea schemaRegistryTextArea;

    @Inject
    public SchemaRegistryViewController(JsonHighlighter jsonHighlighter, @RichTextFxObjectMapper ObjectMapper objectMapper, ProtobufHighlighter protobufHighlighter) {
        this.jsonHighlighter = jsonHighlighter;
        this.protobufHighlighter = protobufHighlighter;
        this.objectMapper = objectMapper;
        schemaRegistryEventSubscriber = new SchemaRegistryEventSubscriber(this, (ex) -> UIErrorHandler.showError(Thread.currentThread(), ex)
        );
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
        this.schemaEditableTable.setStage(stage);
    }

    @FXML
    public void initialize() {
        // TODO: Multiple version for an schema, make the schema table editable
//        schemaRegistryTextArea.textProperty().addListener((obs, oldText, newText) -> {
//            ViewUtils.setValueAndHighlightJsonInCodeArea(newText, schemaRegistryTextArea, true, objectMapper, jsonHighlighter);
//        });
        schemaEditableTable.addEventHandler(SelectedSchemaEvent.SELECTED_SCHEMA_EVENT_TYPE,
                (event) -> {
                    String schema = event.getData().getValue().getRight();
                    DisplayType displayType = event.getData().getValue().getLeft();
                    ViewUtils.setValueAndHighlightInCodeArea(schema, schemaRegistryTextArea, true, objectMapper, getHighlighter(displayType));
                });
    }

    public Highlighter getHighlighter(DisplayType displayType) {
        return displayType == DisplayType.PROTOBUF ? protobufHighlighter : jsonHighlighter;
    }
    public void loadAllSchema(KafkaCluster cluster, boolean useCache) throws ExecutionException, InterruptedException {
        schemaEditableTable.loadAllSchemas(cluster, useCache,
                isBlockingAppUINeeded, (e) -> isBlockingAppUINeeded.set(false),
                (e) -> {
                    isBlockingAppUINeeded.set(false);
                    throw ((RuntimeException) e);
                });
    }

    public void refresh() throws RestClientException, IOException, ExecutionException, InterruptedException {
        schemaEditableTable.refresh();
    }

    @Getter
    static class SelectedSchemaEvent extends Event {
        // Define event type
        public static final EventType<SelectedSchemaEvent> SELECTED_SCHEMA_EVENT_TYPE =
                new EventType<>(Event.ANY, "SELECTED_SCHEMA_EVENT_TYPE");

        // Getter for custom data
        private final SimpleObjectProperty<Pair<DisplayType, String>> data;

        public SelectedSchemaEvent(SimpleObjectProperty<Pair<DisplayType, String>> data) {
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
    public static class SchemaRegistryEventSubscriber extends EventSubscriber<SchemaRegistryUIEvent> {
        private final SchemaRegistryViewController schemaRegistryViewController;
        private final Consumer<Exception> onFailure;

        @Override
        public void handleOnNext(SchemaRegistryUIEvent item) {
            if (SchemaRegistryUIEvent.isRefreshEvent(item)) {
                Platform.runLater(() -> {
                    try {
                        schemaRegistryViewController.loadAllSchema(item.cluster(), item.useCache());
                    } catch (ExecutionException | InterruptedException ex) {
                        onFailure.accept(ex);
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
