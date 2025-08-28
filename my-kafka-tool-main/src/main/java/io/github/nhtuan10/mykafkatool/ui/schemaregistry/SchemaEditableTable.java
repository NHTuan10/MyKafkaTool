package io.github.nhtuan10.mykafkatool.ui.schemaregistry;

import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.SchemaMetadataFromRegistry;
import io.github.nhtuan10.mykafkatool.schemaregistry.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.ui.Filter;
import io.github.nhtuan10.mykafkatool.ui.control.EditableTableControl;
import io.github.nhtuan10.mykafkatool.ui.util.ModalUtils;
import io.github.nhtuan10.mykafkatool.ui.util.TableViewConfigurer;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.ComboBoxTableCell;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Slf4j
public class SchemaEditableTable extends EditableTableControl<SchemaTableItem> {
    public static final String DEFAULT_SCHEMA_TABLE_PLACEHOLDER = "No schema found";
    public static final String ERROR_MESSAGE_TABLE_PLACEHOLDER = "Error when get schema registry subject metadata";
    private KafkaCluster selectedClusterName;
    private BooleanProperty isBlockingUINeeded;
    // add event handler to clean cache
    private Map<KafkaCluster, SchemaTableItemsAndFilter> clusterNameToSchemaTableItemsCache;
    private final SchemaRegistryManager schemaRegistryManager = MyKafkaToolApplication.DAGGER_APP_COMPONENT.schemaRegistryManager();

    static SchemaTableItem mapFromSchemaMetaData(SchemaMetadataFromRegistry schemaMetadataFromRegistry, String clusterName) {
        // schemaMetadata is null if only subject is loaded from registry
        Optional<SchemaMetadata> schemaMetadataOptional = Optional.ofNullable(schemaMetadataFromRegistry.schemaMetadata());

        return SchemaTableItemFXModel.builder()
                .subject(schemaMetadataFromRegistry.subjectName())
                .schemaId(schemaMetadataOptional.map(SchemaMetadata::getId).map(String::valueOf).orElse(null))
                .version(schemaMetadataOptional.map(SchemaMetadata::getVersion).map(String::valueOf).orElse(null))
                .allVersions(schemaMetadataFromRegistry.allVersions().stream().map(String::valueOf).toList())
                .type(schemaMetadataOptional.map(SchemaMetadata::getSchemaType).orElse(null))
                .compatibility(schemaMetadataFromRegistry.compatibility())
                .schema(schemaMetadataOptional.map(SchemaMetadata::getSchema).orElse(null))
                .clusterName(clusterName)
                .build();
    }

    @FXML
    protected void initialize() {
        clusterNameToSchemaTableItemsCache = new ConcurrentHashMap<>();
        super.initialize();
        TableColumn<SchemaTableItem, String> versionCol =
                new TableColumn<>("Version");
        versionCol.setCellValueFactory(cellData -> {
            StringProperty versionProperty = cellData.getValue().versionProperty();
            versionProperty.addListener((obs, oldVal, newVal) -> {
                try {
                    SchemaMetadata schemaMetadata = schemaRegistryManager.getSubjectMetadata(selectedClusterName.getName(), cellData.getValue().getSubject(), Integer.parseInt(newVal));
                    String schema = schemaMetadata.getSchema();
                    cellData.getValue().setSchema(schema);
//                    cellData.getTableView().refresh();
                    SchemaRegistryViewController.SelectedSchemaEvent selectedSchemaEvent = new SchemaRegistryViewController.SelectedSchemaEvent(new SimpleStringProperty(schema));
                    fireEvent(selectedSchemaEvent);
                } catch (RestClientException | IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return versionProperty;
        });
        versionCol.setCellFactory((tableColumn) -> new ComboBoxTableCell<>() {
            @Override
            public void startEdit() {
                getItems().setAll(getTableRow().getItem().getAllVersions());
                super.startEdit();
            }
        });

//                ComboBoxTableCell.forTableColumn(
//                        "1", "2", "3"
//                )

        table.getColumns().remove(TableViewConfigurer.getTableColumnById(table, SchemaTableItem.VERSION).orElse(null));
        table.getColumns().add(versionCol);
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String subjectName = newValue.getSubject();
                log.info("selected item {}", subjectName);

                String schema = newValue.getSchema();
                if (schema == null) {
                    try {
                        SchemaMetadata schemaMetadata = schemaRegistryManager.getSubject(selectedClusterName.getName(), subjectName);
                        List<Integer> allVersions = schemaRegistryManager.getAllVersions(selectedClusterName.getName(), subjectName);
                        String compatibility = schemaRegistryManager.getCompatibility(selectedClusterName.getName(), subjectName);
                        schema = schemaMetadata.getSchema();
                        newValue.setSchemaId(String.valueOf(schemaMetadata.getId()));
                        newValue.setSchema(schema);
                        newValue.setType(schemaMetadata.getSchemaType());
                        newValue.setVersion(String.valueOf(schemaMetadata.getVersion()));
                        newValue.setAllVersions(allVersions.stream().map(String::valueOf).toList());
                        newValue.setCompatibility(compatibility);
//                        newValue = mapFromSchemaMetaData(new SchemaMetadataFromRegistry(subjectName, schemaMetadata, compatibility, allVersions), selectedClusterName.getName());
//                        schema = schemaMetadata.getSchema();
                        table.refresh();
                    } catch (RestClientException | IOException e) {
                        throw new RuntimeException("Error when loading subject {} from Schema Registry", e);
                    }
                }
                SchemaRegistryViewController.SelectedSchemaEvent selectedSchemaEvent = new SchemaRegistryViewController.SelectedSchemaEvent(new SimpleStringProperty(schema));
                fireEvent(selectedSchemaEvent);
            }
        });
        List.of(filterTextField.textProperty(), regexFilterToggleBtn.selectedProperty(), caseSensitiveFilterToggleBtn.selectedProperty(), negativeFilterToggleBtn.selectedProperty()).forEach(property -> {
            property.addListener((observable, oldValue, newValue) -> {
//                filterItems();
                Optional.ofNullable(clusterNameToSchemaTableItemsCache.get(this.selectedClusterName))
                        .ifPresent(schemaTableItemsAndFilter -> schemaTableItemsAndFilter.setFilter(this.filterProperty.get().copy()));
            });
        });
//        this.filterTextField.textProperty().addListener((observable, oldValue, newValue) -> {
//            filterItems();
//            Optional.ofNullable(clusterNameToSchemaTableItemsCache.get(this.selectedClusterName))
//                    .ifPresent(schemaTableItemsAndFilter -> schemaTableItemsAndFilter.getFilter().setFilterText(newValue));
//        });
//        this.regexFilterToggleBtn.selectedProperty().addListener((observable, oldValue, newValue) -> {
//            filterItems();
//            Optional.ofNullable(clusterNameToSchemaTableItemsCache.get(this.selectedClusterName))
//                    .ifPresent(schemaTableItemsAndFilter -> schemaTableItemsAndFilter.getFilter().setIsRegexFilter(newValue));
//        });
//        this.caseSensitiveFilterToggleBtn.selectedProperty().addListener((observable, oldValue, newValue) -> {
//            filterItems();
//            Optional.ofNullable(clusterNameToSchemaTableItemsCache.get(this.selectedClusterName))
//                    .ifPresent(schemaTableItemsAndFilter -> schemaTableItemsAndFilter.getFilter().setIsCaseSensitive(newValue));
//        });
//        this.negativeFilterToggleBtn.selectedProperty().addListener((observable, oldValue, newValue) -> {
//            filterItems();
//            Optional.ofNullable(clusterNameToSchemaTableItemsCache.get(this.selectedClusterName))
//                    .ifPresent(schemaTableItemsAndFilter -> schemaTableItemsAndFilter.getFilter().setIsNegative(newValue));
//        });
        // TODO: functionality to add a new schema
    }

//    @Override
//    protected Predicate<SchemaTableItem> filterPredicate(Filter filter) {
//        return Filter.buildFilterPredicate(filter,
//                SchemaTableItem::getSubject,
//                SchemaTableItem::getSchema,
//                SchemaTableItem::getType,
//                SchemaTableItem::getCompatibility,
//                SchemaTableItem::getSchemaId,
//                SchemaTableItem::getLatestVersion);
//    }

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
            ObservableList<SchemaTableItem> items = refresh((e) -> isBlockingUINeeded.set(false), (e) -> {
                isBlockingUINeeded.set(false);
                throw ((RuntimeException) e);
            });
            setTableItemsAndFilter(items, this.filterProperty.get());
        }
    }

    public void loadAllSchemas(KafkaCluster kafkaCluster, Consumer<ObservableList<SchemaTableItem>> onSuccess, Consumer<Throwable> onError, BooleanProperty isBusy) throws ExecutionException, InterruptedException {
        this.selectedClusterName = kafkaCluster;
        this.isBlockingUINeeded = isBusy;
        if (!clusterNameToSchemaTableItemsCache.containsKey(this.selectedClusterName)) {
            setTableItemsAndFilter(refresh(onSuccess, onError), new Filter());
        } else {
            SchemaTableItemsAndFilter schemaTableItemsAndFilter = clusterNameToSchemaTableItemsCache.get(this.selectedClusterName);
            setTableItemsAndFilter(schemaTableItemsAndFilter.getItems(), schemaTableItemsAndFilter.getFilter());
        }
    }

    private void setTableItemsAndFilter(ObservableList<SchemaTableItem> items, Filter filter) {
        setItems(items);
        applyFilter(filter);
    }

    private ObservableList<SchemaTableItem> refresh(Consumer<ObservableList<SchemaTableItem>> onSuccess, Consumer<Throwable> onError) throws ExecutionException, InterruptedException {
        ObservableList<SchemaTableItem> schemaItems;
        Callable<ObservableList<SchemaTableItem>> getSchemaTask = () -> {
            ObservableList<SchemaTableItem> items;
            try {
                Platform.runLater(() -> this.isBlockingUINeeded.set(true));
                List<SchemaMetadataFromRegistry> schemaMetadataList = schemaRegistryManager.getAllSubjectMetadata(this.selectedClusterName.getName(), this.selectedClusterName.isOnlySubjectLoaded(), false);
                items = FXCollections.observableArrayList(
                        schemaMetadataList
                                .stream()
                                .map(schemaMetadata -> mapFromSchemaMetaData(schemaMetadata, this.selectedClusterName.getName()))
                                .toList());
                clusterNameToSchemaTableItemsCache.put(this.selectedClusterName, new SchemaTableItemsAndFilter(items, this.filterProperty.get().copy(), DEFAULT_SCHEMA_TABLE_PLACEHOLDER));
                Platform.runLater(() -> {
                    this.isBlockingUINeeded.set(false);
                    table.setPlaceholder(new Label(DEFAULT_SCHEMA_TABLE_PLACEHOLDER));
                });

            } catch (Exception e) {
                log.error(ERROR_MESSAGE_TABLE_PLACEHOLDER, e);
                Platform.runLater(() -> {
                    var emptyItems = FXCollections.<SchemaTableItem>emptyObservableList();
                    setItems(emptyItems);
                    clusterNameToSchemaTableItemsCache.put(this.selectedClusterName, new SchemaTableItemsAndFilter(emptyItems, this.filterProperty.get().copy(), ERROR_MESSAGE_TABLE_PLACEHOLDER));
                    table.setPlaceholder(new Label("Error when get schemas: " + e.getMessage()));
                });
                throw new RuntimeException(e);
            }
            return items;
        };
        schemaItems = ViewUtils.runBackgroundTask(getSchemaTask, onSuccess, onError).get();
        return schemaItems;
    }

    @Override
    protected boolean doRemoveItem(int index, SchemaTableItem item) {
        if (ModalUtils.confirmAlert("Delete Subject", "Are you sure to delete " + item.getSubject() + " ?", "Yes", "Cancel")) {
            try {
                schemaRegistryManager.deleteSubject(item.getClusterName(), item.getSubject());
            } catch (RestClientException | IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
    }


    @Data
    @AllArgsConstructor
    private static class SchemaTableItemsAndFilter {
        private ObservableList<SchemaTableItem> items;
        private Filter filter;
        private String placeHolderText;
    }
}
