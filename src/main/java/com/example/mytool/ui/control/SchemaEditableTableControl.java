package com.example.mytool.ui.control;

import com.example.mytool.manager.SchemaRegistryManager;
import com.example.mytool.model.kafka.KafkaCluster;
import com.example.mytool.model.kafka.SchemaMetadataFromRegistry;
import com.example.mytool.ui.SchemaTableItem;
import com.example.mytool.ui.util.ViewUtil;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.fxml.FXML;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
public class SchemaEditableTableControl extends EditableTableControl<SchemaTableItem> {
    private KafkaCluster selectedClusterName;
    private BooleanProperty isBusy;
    private Map<String, SchemaTableItemsAndFilter> clusterNameToSchemaTableItemsCache;
    SchemaRegistryManager schemaRegistryManager = SchemaRegistryManager.getInstance();

//    public void setItems(List<SchemaMetadataFromRegistry> schemaMetadataList, String clusterName) {
//        ObservableList<SchemaTableItem> items = FXCollections.observableArrayList(schemaMetadataList.stream().map(schemaMetadata -> mapFromSchemaMetaData(schemaMetadata, clusterName)).toList());
//        tableItems.setAll(items);
//        selectedClusterName = clusterName;
//    }

    static SchemaTableItem mapFromSchemaMetaData(SchemaMetadataFromRegistry schemaMetadataFromRegistry, String clusterName) {
        // schemaMetadata is null if only subject is loaded from registry
        Optional<SchemaMetadata> schemaMetadataOptional = Optional.ofNullable(schemaMetadataFromRegistry.schemaMetadata());

        return new SchemaTableItem(
                schemaMetadataFromRegistry.subjectName(),
                schemaMetadataOptional.map(SchemaMetadata::getId).map(String::valueOf).orElse(null),
                schemaMetadataOptional.map(SchemaMetadata::getVersion).map(String::valueOf).orElse(null),
                schemaMetadataOptional.map(SchemaMetadata::getSchemaType).orElse(null),
                schemaMetadataFromRegistry.compatibility(),
                schemaMetadataOptional.map(SchemaMetadata::getSchema).orElse(null),
                clusterName
        );

    }

    @FXML
    protected void initialize() {
        clusterNameToSchemaTableItemsCache = new ConcurrentHashMap<>();
        super.initialize();
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String subjectName = newValue.getSubject();
                log.info("selected item {}", subjectName);

                String schema = newValue.getSchema();
                if (schema == null) {
                    try {
                        SchemaMetadata schemaMetadata = schemaRegistryManager.getSubject(selectedClusterName.getName(), subjectName);
                        schema = schemaMetadata.getSchema();
                        newValue.setSchemaId(String.valueOf(schemaMetadata.getId()));
                        newValue.setSchema(schema);
                        newValue.setType(schemaMetadata.getSchemaType());
                        newValue.setLatestVersion(String.valueOf(schemaMetadata.getVersion()));
                        newValue.setCompatibility(schemaRegistryManager.getCompatibility(selectedClusterName.getName(), subjectName));
                        table.refresh();
                    } catch (RestClientException | IOException e) {
                        throw new RuntimeException("Error when loading subject {} from Schema Registry", e);
                    }
                }
                SelectedSchemaEvent selectedSchemaEvent = new SelectedSchemaEvent(new SimpleStringProperty(schema));
                fireEvent(selectedSchemaEvent);
            }
        });
        this.filterTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilter(newValue);
            if (clusterNameToSchemaTableItemsCache.containsKey(this.selectedClusterName.getName())) {
                SchemaTableItemsAndFilter schemaTableItemsAndFilter = clusterNameToSchemaTableItemsCache.get(this.selectedClusterName.getName());
                schemaTableItemsAndFilter.setFilter(newValue);
            }
        });
    }

    @Override
    protected Predicate<SchemaTableItem> filterPredicate(String filterText) {
        return item -> item.getSubject().toLowerCase().contains(filterText.toLowerCase()) ||
                (item.getSchema() != null && item.getSchema().toLowerCase().contains(filterText.toLowerCase()));
    }

    //    @RequiredArgsConstructor
//    private class FilterPredicated implements Predicate<SchemaTableItem> {
//        final String filterText;
//
//        @Override
//        public boolean test(SchemaTableItem item) {
//            return  item.getSubject().toLowerCase().contains(filterText.toLowerCase()) ||
//                    (item.getSchema() != null && item.getSchema().toLowerCase().contains(filterText.toLowerCase()));
//        }
//    }
    @FXML
    public void refresh() throws RestClientException, IOException {
        if (this.selectedClusterName != null) {
            refresh((e) -> isBusy.set(false), (e) -> {
                isBusy.set(false);
                throw ((RuntimeException) e);
            });
        }
    }

    public void loadAllSchemas(KafkaCluster kafkaCluster, Consumer<Object> onSuccess, Consumer<Throwable> onError, BooleanProperty isBusy) {
        this.selectedClusterName = kafkaCluster;
        this.isBusy = isBusy;
        if (!clusterNameToSchemaTableItemsCache.containsKey(this.selectedClusterName.getName())) {
            this.filterTextField.setText("");
            refresh(onSuccess, onError);
        } else {
            SchemaTableItemsAndFilter schemaTableItemsAndFilter = clusterNameToSchemaTableItemsCache.get(this.selectedClusterName.getName());
            tableItems.setAll(schemaTableItemsAndFilter.getItems());
            applyFilter(schemaTableItemsAndFilter.getFilter());
        }
    }

    private void refresh(Consumer<Object> onSuccess, Consumer<Throwable> onError) {
        ViewUtil.runBackgroundTask(() -> {
            try {
                this.isBusy.set(true);
                List<SchemaMetadataFromRegistry> schemaMetadataList = schemaRegistryManager.getAllSubjectMetadata(this.selectedClusterName.getName(), this.selectedClusterName.isOnlySubjectLoaded());
                ObservableList<SchemaTableItem> items = FXCollections.observableArrayList(
                        schemaMetadataList
                                .stream()
                                .map(schemaMetadata -> mapFromSchemaMetaData(schemaMetadata, this.selectedClusterName.getName()))
                                .toList());
                tableItems.setAll(items);
                clusterNameToSchemaTableItemsCache.put(this.selectedClusterName.getName(), new SchemaTableItemsAndFilter(items, this.filterTextField.getText()));
                this.isBusy.set(false);
            } catch (RestClientException | IOException e) {
                log.error("Error when get schema registry subject metadata", e);
                throw new RuntimeException(e);
            }
            return null;
        }, onSuccess, onError);
    }

    @FXML
    public void removeItem() {
        List<Integer> indicesToRemove = table.getSelectionModel().getSelectedIndices().reversed();
        indicesToRemove.forEach((i) -> {
            SchemaTableItem item = tableItems.get(i);
            if (ViewUtil.confirmAlert("Delete Subject", "Are you sure to delete " + item.getSubject() + " ?", "Yes", "Cancel")) {
                try {
                    schemaRegistryManager.deleteSubject(item.getClusterName(), item.getSubject());
                } catch (RestClientException | IOException e) {
                    throw new RuntimeException(e);
                }
                tableItems.remove((int) i);
            }
        });
    }


    @Getter
    public static class SelectedSchemaEvent extends Event {
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

    @Data
    @AllArgsConstructor
    class SchemaTableItemsAndFilter {
        private ObservableList<SchemaTableItem> items;
        private String filter;
    }
}
