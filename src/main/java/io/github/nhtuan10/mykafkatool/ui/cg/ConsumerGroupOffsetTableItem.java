package io.github.nhtuan10.mykafkatool.ui.cg;

import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.ToString;

public class ConsumerGroupOffsetTableItem {
    @TableViewColumn
    private final SimpleStringProperty consumerID;
    @TableViewColumn
    private final SimpleStringProperty topic;
    @TableViewColumn
    private final SimpleIntegerProperty partition;
    @TableViewColumn
    private final SimpleLongProperty start;
    @TableViewColumn
    private final SimpleLongProperty end;
    @TableViewColumn
    private final SimpleStringProperty committedOffset;
    @TableViewColumn
    private final SimpleStringProperty lag;
    @TableViewColumn
    private final SimpleStringProperty lastCommit;
    @TableViewColumn
    private final SimpleStringProperty host;

    public static final String MEMBER_ID = "Member ID";
    public static final String TOPIC = "Topic";
    public static final String PARTITION = "Partition";
    public static final String START = "Start";
    public static final String END = "End";
    public static final String COMMITTED_OFFSET = "Committed Offset";
    public static final String LAG = "Lag";
    public static final String LAST_COMMIT = "Last Commit";
    public static final String HOST = "Host";

    public ConsumerGroupOffsetTableItem(String consumerID, String topic, int partition, long start, long end, String committedOffset, String lag, String lastCommit, String host) {
        this.consumerID = new SimpleStringProperty(consumerID);
        this.topic = new SimpleStringProperty(topic);
        this.partition = new SimpleIntegerProperty(partition);
        this.start = new SimpleLongProperty(start);
        this.end = new SimpleLongProperty(end);
        this.committedOffset = new SimpleStringProperty(committedOffset);
        this.lag = new SimpleStringProperty(lag);
        this.lastCommit = new SimpleStringProperty(lastCommit);
        this.host = new SimpleStringProperty(host);
    }

    public static ConsumerGroupOffsetTableItemBuilder builder() {
        return new ConsumerGroupOffsetTableItemBuilder();
    }

    public int getPartition() {
        return partition.get();
    }

    public void setPartition(int partition) {
        this.partition.set(partition);
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

    public String getCommittedOffset() {
        return committedOffset.get();
    }

    public void setCommittedOffset(String committedOffset) {
        this.committedOffset.set(committedOffset);
    }

    public String getLag() {
        return lag.get();
    }

    public void setLag(String lag) {
        this.lag.set(lag);
    }

    public String getLastCommit() {
        return lastCommit.get();
    }

    public void setLastCommit(String lastCommit) {
        this.lastCommit.set(lastCommit);
    }

    public String getHost() {
        return host.get();
    }

    public void setHost(String host) {
        this.host.set(host);
    }

    public String getConsumerID() {
        return consumerID.get();
    }

    public void setConsumerID(String consumerID) {
        this.consumerID.set(consumerID);
    }

    @ToString
    public static class ConsumerGroupOffsetTableItemBuilder {
        String consumerID;
        String topic;
        int partition;
        long start;
        long end;
        String committedOffset;
        String lag;
        String lastCommit;
        String host;

        ConsumerGroupOffsetTableItemBuilder() {
        }

        public ConsumerGroupOffsetTableItemBuilder consumerID(String consumerID) {
            this.consumerID = consumerID;
            return this;
        }

        public ConsumerGroupOffsetTableItemBuilder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public ConsumerGroupOffsetTableItemBuilder partition(int partition) {
            this.partition = partition;
            return this;
        }

        public ConsumerGroupOffsetTableItemBuilder start(long start) {
            this.start = start;
            return this;
        }

        public ConsumerGroupOffsetTableItemBuilder end(long end) {
            this.end = end;
            return this;
        }

        public ConsumerGroupOffsetTableItemBuilder committedOffset(String committedOffset) {
            this.committedOffset = committedOffset;
            return this;
        }

        public ConsumerGroupOffsetTableItemBuilder lag(String lag) {
            this.lag = lag;
            return this;
        }

        public ConsumerGroupOffsetTableItemBuilder lastCommit(String lastCommit) {
            this.lastCommit = lastCommit;
            return this;
        }


        public ConsumerGroupOffsetTableItemBuilder host(String host) {
            this.host = host;
            return this;
        }

        public ConsumerGroupOffsetTableItem build() {
            return new ConsumerGroupOffsetTableItem(this.consumerID, this.topic, this.partition, this.start, this.end, this.committedOffset, this.lag, this.lastCommit, this.host);
        }

    }
}
