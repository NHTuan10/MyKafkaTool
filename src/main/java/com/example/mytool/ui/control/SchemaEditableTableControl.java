package com.example.mytool.ui.control;

import com.example.mytool.manager.SchemaRegistryManager;
import com.example.mytool.model.kafka.SchemaMetadataFromRegistry;
import com.example.mytool.ui.SchemaTableItem;
import com.example.mytool.ui.util.ViewUtil;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.fxml.FXML;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class SchemaEditableTableControl extends EditableTableControl<SchemaTableItem> {
    private String selectedClusterName;

    public void setItems(List<SchemaMetadataFromRegistry> schemaMetadataList, String clusterName) {
        ObservableList<SchemaTableItem> items = FXCollections.observableArrayList(schemaMetadataList.stream().map(schemaMetadata -> mapFromSchemaMetaData(schemaMetadata, clusterName)).toList());
        tableItems.setAll(items);
        selectedClusterName = clusterName;
    }

    static SchemaTableItem mapFromSchemaMetaData(SchemaMetadataFromRegistry schemaMetadataFromRegistry, String clusterName) {
        SchemaMetadata schemaMetadata = schemaMetadataFromRegistry.schemaMetadata();
        return new SchemaTableItem(
                schemaMetadata.getSubject(),
                schemaMetadata.getId(),
                schemaMetadata.getVersion(),
                schemaMetadata.getSchemaType(),
                schemaMetadataFromRegistry.compatibility(),
                schemaMetadata.getSchema(),
                clusterName
        );

    }

    @FXML
    protected void initialize() {
        super.initialize();
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                log.info("selected item {}", newValue.getSubject());
                SelectedSchemaEvent selectedSchemaEvent = new SelectedSchemaEvent(new SimpleStringProperty(newValue.getSchema()));
                fireEvent(selectedSchemaEvent);
            }
        });
//        addItemBtn.setVisible(false);
    }

    @FXML
    public void refresh() throws RestClientException, IOException {
        if (this.selectedClusterName != null) {
            List<SchemaMetadataFromRegistry> schemaMetadataList = SchemaRegistryManager.getInstance().getAllSubjectMetadata(this.selectedClusterName);
            setItems(schemaMetadataList, this.selectedClusterName);
        }
    }

    @FXML
    public void removeItem() {
        List<Integer> indicesToRemove = table.getSelectionModel().getSelectedIndices().reversed();
        indicesToRemove.forEach((i) -> {
            SchemaTableItem item = tableItems.get(i);
            if (ViewUtil.confirmAlert("Delete Subject", "Are you sure to delete " + item.getSubject() + " ?", "Yes", "Cancel")) {
                try {
                    SchemaRegistryManager.getInstance().deleteSubject(item.getClusterName(), item.getSubject());
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
}
