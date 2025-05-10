package com.example.mytool.ui;

import com.example.mytool.annotation.TableColumn;
import javafx.beans.property.SimpleStringProperty;
import lombok.Data;

@Data
public class UIPropertyTableItem {
    public static final String START_OFFSET = "Start Offset";
    public static final String END_OFFSET = "End Offset";
    public static final String NO_MESSAGES = "No Messages";
    public static final String LEADER = "Leader";
    public static final String REPLICA_IN_SYNC = "Replica [In-Sync]";
    public static final String REPLICA_NOT_IN_SYNC = "Replica [Not-In-Sync]";
    @TableColumn
    private final SimpleStringProperty name;
    @TableColumn
    private final SimpleStringProperty value;

    public UIPropertyTableItem(String name, String value) {
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public String getValue() {
        return value.get();
    }

    public void setValue(String value) {
        this.value.set(value);
    }

    public SimpleStringProperty valueProperty() {
        return value;
    }


}
