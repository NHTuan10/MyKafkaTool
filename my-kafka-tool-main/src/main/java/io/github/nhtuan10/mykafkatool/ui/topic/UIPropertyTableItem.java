package io.github.nhtuan10.mykafkatool.ui.topic;

import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import io.github.nhtuan10.mykafkatool.annotationprocessor.FXModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@FXModel
public final class UIPropertyTableItem implements UIPropertyTableItemFXModel {
    public static final String START_OFFSET = "Start Offset";
    public static final String END_OFFSET = "End Offset";
    public static final String NO_MESSAGES = "No Messages";
    public static final String LEADER = "Leader";
    public static final String REPLICA_IN_SYNC = "Replica [In-Sync]";
    public static final String REPLICA_NOT_IN_SYNC = "Replica [Not-In-Sync]";
    @TableViewColumn
    StringProperty name;
    @TableViewColumn
    StringProperty value;

    public UIPropertyTableItem(String name, String value) {
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
    }

    public UIPropertyTableItem() {

    }
}
