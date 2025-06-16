package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import io.github.nhtuan10.mykafkatool.annotationprocessor.FXModel;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.StringProperty;
import org.apache.kafka.common.ConsumerGroupState;

@FXModel
public final class ConsumerTableItem implements ConsumerTableItemFXModel {
    @TableViewColumn
    StringProperty consumerID;
    @TableViewColumn
    StringProperty topic;
    @TableViewColumn
    IntegerProperty partition;
    @TableViewColumn
    LongProperty start;
    @TableViewColumn
    LongProperty end;
    @TableViewColumn
    StringProperty committedOffset;
    @TableViewColumn
    StringProperty lag;
    @TableViewColumn
    StringProperty lastCommit;
    @TableViewColumn
    StringProperty host;

    ConsumerGroupState state;

//    public static final String CONSUMER_ID = "consumerID";
//    public static final String TOPIC = "topic";
//    public static final String PARTITION = "partition";
//    public static final String START = "start";
//    public static final String END = "end";
//    public static final String COMMITTED_OFFSET = "committedOffset";
//    public static final String LAG = "lag";
//    public static final String LAST_COMMIT = "lastCommit";
//    public static final String HOST = "host";

//    public ConsumerGroupIDOffsetTableItem(String consumerID, String topic, int partition, long start, long end, String committedOffset, String lag, String lastCommit, String host) {
//        this.consumerID = new SimpleStringProperty(consumerID);
//        this.topic = new SimpleStringProperty(topic);
//        this.partition = new SimpleIntegerProperty(partition);
//        this.start = new SimpleLongProperty(start);
//        this.end = new SimpleLongProperty(end);
//        this.committedOffset = new SimpleStringProperty(committedOffset);
//        this.lag = new SimpleStringProperty(this, "lag", "Unknown");
//        this.lastCommit = new SimpleStringProperty(lastCommit);
//        this.host = new SimpleStringProperty(host);
//    }

//    public static ConsumerGroupOffsetTableItemBuilder builder() {
//        return new ConsumerGroupOffsetTableItemBuilder();
//    }

//    public int getPartition() {
//        return partition.get();
//    }
//
//    public void setPartition(int partition) {
//        this.partition.set(partition);
//    }
//
//    public String getTopic() {
//        return topic.get();
//    }
//
//    public void setTopic(String topic) {
//        this.topic.set(topic);
//    }
//
//    public long getStart() {
//        return start.get();
//    }
//
//    public void setStart(long start) {
//        this.start.set(start);
//    }
//
//    public long getEnd() {
//        return end.get();
//    }
//
//    public void setEnd(long start) {
//        this.end.set(start);
//    }
//
//    public String getCommittedOffset() {
//        return committedOffset.get();
//    }
//
//    public void setCommittedOffset(String committedOffset) {
//        this.committedOffset.set(committedOffset);
//    }
//
//    public String getLag() {
//        return lag.get();
//    }
//
//    public void setLag(String lag) {
//        this.lag.set(lag);
//    }
//
//    public String getLastCommit() {
//        return lastCommit.get();
//    }
//
//    public void setLastCommit(String lastCommit) {
//        this.lastCommit.set(lastCommit);
//    }
//
//    public String getHost() {
//        return host.get();
//    }
//
//    public void setHost(String host) {
//        this.host.set(host);
//    }
//
//    public String getConsumerID() {
//        return consumerID.get();
//    }
//
//    public void setConsumerID(String consumerID) {
//        this.consumerID.set(consumerID);
//    }

//    @ToString
//    public static class ConsumerGroupOffsetTableItemBuilder {
//        String consumerID;
//        String topic;
//        int partition;
//        long start;
//        long end;
//        String committedOffset;
//        String lag;
//        String lastCommit;
//        String host;
//
//        ConsumerGroupOffsetTableItemBuilder() {
//        }
//
//        public ConsumerGroupOffsetTableItemBuilder consumerID(String consumerID) {
//            this.consumerID = consumerID;
//            return this;
//        }
//
//        public ConsumerGroupOffsetTableItemBuilder topic(String topic) {
//            this.topic = topic;
//            return this;
//        }
//
//        public ConsumerGroupOffsetTableItemBuilder partition(int partition) {
//            this.partition = partition;
//            return this;
//        }
//
//        public ConsumerGroupOffsetTableItemBuilder start(long start) {
//            this.start = start;
//            return this;
//        }
//
//        public ConsumerGroupOffsetTableItemBuilder end(long end) {
//            this.end = end;
//            return this;
//        }
//
//        public ConsumerGroupOffsetTableItemBuilder committedOffset(String committedOffset) {
//            this.committedOffset = committedOffset;
//            return this;
//        }
//
//        public ConsumerGroupOffsetTableItemBuilder lag(String lag) {
//            this.lag = lag;
//            return this;
//        }
//
//        public ConsumerGroupOffsetTableItemBuilder lastCommit(String lastCommit) {
//            this.lastCommit = lastCommit;
//            return this;
//        }
//
//
//        public ConsumerGroupOffsetTableItemBuilder host(String host) {
//            this.host = host;
//            return this;
//        }
//
//        public ConsumerGroupIDOffsetTableItem build() {
//            return new ConsumerGroupIDOffsetTableItem(this.consumerID, this.topic, this.partition, this.start, this.end, this.committedOffset, this.lag, this.lastCommit, this.host);
//        }
//
//    }
}
