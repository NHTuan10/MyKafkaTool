package com.example.mytool.ui.cg;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

public class ConsumerGroupOffsetTableItem {
    private final SimpleStringProperty topic;
    private final SimpleIntegerProperty partition;
    private final SimpleLongProperty start;
    private final SimpleLongProperty end;
    private final SimpleLongProperty offset;
    private final SimpleLongProperty lag;
    private final SimpleStringProperty lastCommit;

    public ConsumerGroupOffsetTableItem(String topic, int partition, long start, long end, long offset, long lag, String lastCommit) {
        this.topic = new SimpleStringProperty(topic);
        this.partition = new SimpleIntegerProperty(partition);
        this.start = new SimpleLongProperty(start);
        this.end = new SimpleLongProperty(end);
        this.offset = new SimpleLongProperty(offset);
        this.lag = new SimpleLongProperty(lag);
        this.lastCommit = new SimpleStringProperty(lastCommit);
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

    public String getTopic() {
        return topic.get();
    }

    public void setTopic(String topic) {
        this.topic.set(topic);
    }

    public long getStart() {
        return start.get();
    }

    public void setStart(long start) {
        this.start.set(start);
    }

    public long getEnd() {
        return end.get();
    }

    public void setEnd(long start) {
        this.end.set(start);
    }

    public long getLag() {
        return lag.get();
    }

    public void setLag(long lag) {
        this.lag.set(lag);
    }

    public String getLastCommit() {
        return lastCommit.get();
    }

    public void setLastCommit(String lastCommit) {
        this.lastCommit.set(lastCommit);
    }

}
