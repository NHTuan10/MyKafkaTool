package io.github.nhtuan10.mykafkatool.ui.control;

import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.github.nhtuan10.mykafkatool.manager.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.SchemaMetadataFromRegistry;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.SchemaTableItem;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtil;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
public class SchemaEditableTableControl extends EditableTableControl<SchemaTableItem> {
    private KafkaCluster selectedClusterName;
    private BooleanProperty isBusy;
    private Map<String, SchemaTableItemsAndFilter> clusterNameToSchemaTableItemsCache;
    SchemaRegistryManager schemaRegistryManager = SchemaRegistryManager.getInstance();

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
            applyFilter(new Filter(newValue, this.regexFilterToggleBtn.isSelected()));
            Optional.ofNullable(clusterNameToSchemaTableItemsCache.get(this.selectedClusterName.getName()))
                    .ifPresent(schemaTableItemsAndFilter -> schemaTableItemsAndFilter.getFilter().setFilterText(newValue));
//            if (clusterNameToSchemaTableItemsCache.containsKey(this.selectedClusterName.getName())) {
//                SchemaTableItemsAndFilter schemaTableItemsAndFilter = clusterNameToSchemaTableItemsCache.get(this.selectedClusterName.getName());
//                schemaTableItemsAndFilter.setFilter(newValue);
//            }
        });
        this.regexFilterToggleBtn.selectedProperty().addListener((observable, oldValue, newValue) -> {
            applyFilter(new Filter(this.filterTextProperty.get(), this.regexFilterToggleBtn.isSelected()));
            Optional.ofNullable(clusterNameToSchemaTableItemsCache.get(this.selectedClusterName.getName()))
                    .ifPresent(schemaTableItemsAndFilter -> schemaTableItemsAndFilter.getFilter().setRegexFilter(newValue));
        });
    }

    @Override
    protected Predicate<SchemaTableItem> filterPredicate(Filter filter) {
        return Filter.buildFilterPredicate(filter,
                SchemaTableItem::getSubject,
                SchemaTableItem::getSchema,
                SchemaTableItem::getType,
                SchemaTableItem::getCompatibility,
                SchemaTableItem::getSchemaId,
                SchemaTableItem::getLatestVersion);
//        if (regexFilterToggleBtn.isSelected()) {
//            Pattern pattern = Pattern.compile(filter.getFilterText(), Pattern.CASE_INSENSITIVE);
//
//            return item -> pattern.matcher(item.getSubject()).find() ||
//                    (item.getSchema() != null && pattern.matcher(item.getSchema()).find());
//        }
//        return item -> item.getSubject().toLowerCase().contains(filter.getFilterText().toLowerCase()) ||
//                (item.getSchema() != null && item.getSchema().toLowerCase().contains(filter.getFilterText().toLowerCase()));

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
    public void refresh() throws RestClientException, IOException, ExecutionException, InterruptedException {
        if (this.selectedClusterName != null) {
            ObservableList<SchemaTableItem> items = refresh((e) -> isBusy.set(false), (e) -> {
                isBusy.set(false);
                throw ((RuntimeException) e);
            });
            setTableItemsAndFilter(items, new Filter(this.filterTextProperty.get(), this.regexFilterToggleBtn.isSelected()));
        }
    }

    public void loadAllSchemas(KafkaCluster kafkaCluster, Consumer<ObservableList<SchemaTableItem>> onSuccess, Consumer<Throwable> onError, BooleanProperty isBusy) throws ExecutionException, InterruptedException {
        this.selectedClusterName = kafkaCluster;
        this.isBusy = isBusy;
        if (!clusterNameToSchemaTableItemsCache.containsKey(this.selectedClusterName.getName())) {
            setTableItemsAndFilter(refresh(onSuccess, onError), new Filter("", this.regexFilterToggleBtn.isSelected()));
        } else {
            SchemaTableItemsAndFilter schemaTableItemsAndFilter = clusterNameToSchemaTableItemsCache.get(this.selectedClusterName.getName());
            setTableItemsAndFilter(schemaTableItemsAndFilter.getItems(), schemaTableItemsAndFilter.getFilter());
        }
    }

    private void setTableItemsAndFilter(ObservableList<SchemaTableItem> items, Filter filter) {
        tableItems.setAll(items);
        applyFilter(filter);
    }

    private ObservableList<SchemaTableItem> refresh(Consumer<ObservableList<SchemaTableItem>> onSuccess, Consumer<Throwable> onError) throws ExecutionException, InterruptedException {
        ObservableList<SchemaTableItem> schemaItems;
        Callable<ObservableList<SchemaTableItem>> getSchemaTask = () -> {
            ObservableList<SchemaTableItem> items;
            try {
                this.isBusy.set(true);
                List<SchemaMetadataFromRegistry> schemaMetadataList = schemaRegistryManager.getAllSubjectMetadata(this.selectedClusterName.getName(), this.selectedClusterName.isOnlySubjectLoaded());
                items = FXCollections.observableArrayList(
                        schemaMetadataList
                                .stream()
                                .map(schemaMetadata -> mapFromSchemaMetaData(schemaMetadata, this.selectedClusterName.getName()))
                                .toList());
//                tableItems.setAll(items);
                clusterNameToSchemaTableItemsCache.put(this.selectedClusterName.getName(), new SchemaTableItemsAndFilter(items, new Filter(this.filterTextField.getText(), this.regexFilterToggleBtn.isSelected())));
                this.isBusy.set(false);
            } catch (RestClientException | IOException e) {
                log.error("Error when get schema registry subject metadata", e);
                throw new RuntimeException(e);
            }
            return items;
        };
        schemaItems = ViewUtil.runBackgroundTask(getSchemaTask, onSuccess, onError).get();
        return schemaItems;
    }

    @Override
    protected boolean doRemoveItem(int index, SchemaTableItem item) {
        if (ViewUtil.confirmAlert("Delete Subject", "Are you sure to delete " + item.getSubject() + " ?", "Yes", "Cancel")) {
            try {
                schemaRegistryManager.deleteSubject(item.getClusterName(), item.getSubject());
            } catch (RestClientException | IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
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
    private static class SchemaTableItemsAndFilter {
        private ObservableList<SchemaTableItem> items;
        private Filter filter;

    }
}
