package com.example.mytool.ui;

import com.example.mytool.annotation.TableColumn;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Getter;

public class SchemaTableItem {
    @TableColumn
    private final SimpleStringProperty subject;
    @TableColumn
    private final SimpleIntegerProperty schemaId;
    @TableColumn
    private final SimpleIntegerProperty latestVersion;
    @TableColumn
    private final SimpleStringProperty type;
    @TableColumn
    private final SimpleStringProperty compatibility;
    private final SimpleStringProperty schema;

    @Getter
    private final String clusterName;

    public SchemaTableItem(String subject, int schemaId, int latestVersion, String type, String compatibility, String schema, String clusterName) {
        this.schemaId = new SimpleIntegerProperty(schemaId);
        this.latestVersion = new SimpleIntegerProperty(latestVersion);
        this.type = new SimpleStringProperty(type);
        this.subject = new SimpleStringProperty(subject);
        this.compatibility = new SimpleStringProperty(compatibility);
        this.schema = new SimpleStringProperty(schema);
        this.clusterName = clusterName;
    }

    public int getSchemaId() {
        return schemaId.get();
    }

    public void setSchemaId(int schemaId) {
        this.schemaId.set(schemaId);
    }

    public int getLatestVersion() {
        return latestVersion.get();
    }

    public void setLatestVersion(int latestVersion) {
        this.latestVersion.set(latestVersion);
    }

    public String getType() {
        return type.get();
    }

    public void setType(String type) {
        this.type.set(type);
    }

    public String getSubject() {
        return subject.get();
    }

    public void setSubject(String subject) {
        this.subject.set(subject);
    }

    public String getCompatibility() {
        return compatibility.get();
    }

    public void setCompatibility(String compatibility) {
        this.compatibility.set(compatibility);
    }

    public String getSchema() {
        return schema.get();
    }

    public SimpleStringProperty schemaProperty() {
        return schema;
    }

    public void setSschema(String schema) {
        this.subject.set(schema);
    }


}
