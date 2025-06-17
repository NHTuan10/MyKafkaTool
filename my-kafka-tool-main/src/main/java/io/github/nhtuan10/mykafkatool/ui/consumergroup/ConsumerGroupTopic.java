package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import javafx.beans.property.SimpleStringProperty;

public class ConsumerGroupTopic {
    @TableViewColumn
    protected SimpleStringProperty topic;
    @TableViewColumn
    protected SimpleStringProperty groupID;

    public String getTopic() {
        return topic.get();
    }

    public String getGroupID() {
        return groupID.get();
    }
}
