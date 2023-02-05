package com.example.mytool.ui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

public class KafkaMessageTableItem {
    private final SimpleIntegerProperty partition;
    private final SimpleLongProperty offset;
    private final SimpleStringProperty key;
    private final SimpleStringProperty value;
    private final SimpleStringProperty timestamp;

    public KafkaMessageTableItem(int partition, long offset, String key, String value, String timestamp) {
        this.partition = new SimpleIntegerProperty(partition);
        this.offset = new SimpleLongProperty(offset);
        this.key = new SimpleStringProperty(key);
        this.value = new SimpleStringProperty(value);
        this.timestamp = new SimpleStringProperty(timestamp);
    }

    public int getPartition() {
        return partition.get();
    }

    public void setPartition(int partition) {
        this.partition.set(partition);
    }

    public long getOffset() {
        return offset.get();
    }

    public void setOffset(long offset) {
        this.offset.set(offset);
    }

    public String getKey() {
        return key.get();
    }

    public void setKey(String key) {
        this.key.set(key);
    }

    public String getValue() {
        return value.get();
    }

    public void setValue(String value) {
        this.value.set(value);
    }

    public String getTimestamp() {
        return timestamp.get();
    }

    public void setTimestamp(String timestamp) {
        this.timestamp.set(timestamp);
    }

}
