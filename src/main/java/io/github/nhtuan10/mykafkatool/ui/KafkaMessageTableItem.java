package io.github.nhtuan10.mykafkatool.ui;

import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Getter;
import org.apache.kafka.common.header.Headers;

public class KafkaMessageTableItem {
    @TableViewColumn
    private final SimpleIntegerProperty partition;
    @TableViewColumn
    private final SimpleLongProperty offset;
    @TableViewColumn
    private final SimpleStringProperty key;
    @TableViewColumn
    private final SimpleStringProperty value;
    @TableViewColumn
    private final SimpleStringProperty timestamp;
    @Getter
    private final String valueContentType;
    @Getter
    private final Headers headers;
    @Getter
    private final String schema;
    @Getter
    private final boolean isErrorItem;

    public static final String PARTITION = "partition";
    public static final String OFFSET = "offset";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String TIMESTAMP = "timestamp";

    public KafkaMessageTableItem(int partition, long offset, String key, String value, String timestamp, String valueContentType, Headers headers, String schema, boolean isErrorItem) {
        this.partition = new SimpleIntegerProperty(partition);
        this.offset = new SimpleLongProperty(offset);
        this.key = new SimpleStringProperty(key);
        this.value = new SimpleStringProperty(value);
        this.timestamp = new SimpleStringProperty(timestamp);
        this.valueContentType = valueContentType;
        this.headers = headers;
        this.schema = schema;
        this.isErrorItem = isErrorItem;
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
