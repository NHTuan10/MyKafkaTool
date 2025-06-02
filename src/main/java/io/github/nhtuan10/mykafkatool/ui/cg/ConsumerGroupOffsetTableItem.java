package io.github.nhtuan10.mykafkatool.ui.cg;

import io.github.nhtuan10.mykafkatool.annotation.TableColumn;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.ToString;

public class ConsumerGroupOffsetTableItem {
    @TableColumn
    private final SimpleStringProperty clientID;
    @TableColumn
    private final SimpleStringProperty topic;
    @TableColumn
    private final SimpleIntegerProperty partition;
    @TableColumn
    private final SimpleLongProperty start;
    @TableColumn
    private final SimpleLongProperty end;
    @TableColumn
    private final SimpleStringProperty committedOffset;
    @TableColumn
    private final SimpleStringProperty lag;
    @TableColumn
    private final SimpleStringProperty lastCommit;
    @TableColumn
    private final SimpleStringProperty host;

    private static final String CLIENT_ID = "Client Id";
    private static final String TOPIC = "Topic";
    private static final String PARTITION = "Partition";
    private static final String START = "Start";
    private static final String END = "End";
    private static final String COMMITTED_OFFSET = "Committed Offset";
    private static final String LAG = "Lag";
    private static final String LAST_COMMIT = "Last commit";
    private static final String HOST = "Host";

    public ConsumerGroupOffsetTableItem(String clientID, String topic, int partition, long start, long end, String committedOffset, String lag, String lastCommit, String host) {
        this.clientID = new SimpleStringProperty(clientID);
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

    public String getClientID() {
        return clientID.get();
    }

    public void setClientID(String clientID) {
        this.clientID.set(clientID);
    }

    @ToString
    public static class ConsumerGroupOffsetTableItemBuilder {
        String clientId;
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

        public ConsumerGroupOffsetTableItemBuilder clientId(String clientId) {
            this.clientId = clientId;
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
            return new ConsumerGroupOffsetTableItem(this.clientId, this.topic, this.partition, this.start, this.end, this.committedOffset, this.lag, this.lastCommit, this.host);
        }

    }
}
