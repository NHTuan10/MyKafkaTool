package io.github.nhtuan10.mykafkatool.ui.partition;

import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;


public class KafkaPartitionsTableItem {
    @TableViewColumn
    private final SimpleIntegerProperty partition;
    @TableViewColumn
    private final SimpleLongProperty startOffset;
    @TableViewColumn
    private final SimpleLongProperty endOffset;
    @TableViewColumn
    private final SimpleLongProperty numOfMessages;
    @TableViewColumn
    private final SimpleStringProperty leader;
    @TableViewColumn
    private final SimpleListProperty<String> replicasInSync;
    @TableViewColumn
    private final SimpleListProperty<String> replicasNotInSync;


    public KafkaPartitionsTableItem(int partition, long startOffset, long endOffset, long numOfMessages, String leader, ObservableList<String> replicasInSync, ObservableList<String> replicasNotInSync) {
        this.partition = new SimpleIntegerProperty(partition);
        this.startOffset = new SimpleLongProperty(startOffset);
        this.endOffset = new SimpleLongProperty(endOffset);
        this.numOfMessages = new SimpleLongProperty(numOfMessages);
        this.leader = new SimpleStringProperty(leader);
        this.replicasInSync = new SimpleListProperty<>(replicasInSync);
        this.replicasNotInSync = new SimpleListProperty<>(replicasNotInSync);
    }

    public int getPartition() {
        return partition.get();
    }

    public SimpleIntegerProperty partitionProperty() {
        return partition;
    }

    public long getStartOffset() {
        return startOffset.get();
    }

    public SimpleLongProperty startOffsetProperty() {
        return startOffset;
    }

    public long getEndOffset() {
        return endOffset.get();
    }

    public SimpleLongProperty endOffsetProperty() {
        return endOffset;
    }

    public long getNumOfMessages() {
        return numOfMessages.get();
    }

    public SimpleLongProperty numOfMessagesProperty() {
        return numOfMessages;
    }

    public String getLeader() {
        return leader.get();
    }

    public SimpleStringProperty leaderProperty() {
        return leader;
    }

    public Object getReplicasInSync() {
        return replicasInSync.get();
    }

    public SimpleListProperty<String> replicasInSyncProperty() {
        return replicasInSync;
    }

    public Object getReplicasNotInSync() {
        return replicasNotInSync.get();
    }

    public SimpleListProperty<String> replicasNotInSyncProperty() {
        return replicasNotInSync;
    }


    public void setPartition(int partition) {
        this.partition.set(partition);
    }

    public void setStartOffset(long startOffset) {
        this.startOffset.set(startOffset);
    }

    public void setEndOffset(long endOffset) {
        this.endOffset.set(endOffset);
    }

    public void setNumOfMessages(long numOfMessages) {
        this.numOfMessages.set(numOfMessages);
    }

    public void setLeader(String leader) {
        this.leader.set(leader);
    }

    public void setReplicasInSync(ObservableList<String> replicasInSync) {
        this.replicasInSync.set(replicasInSync);
    }

    public void setReplicasNotInSync(ObservableList<String> replicasNotInSync) {
        this.replicasNotInSync.set(replicasNotInSync);
    }


}
