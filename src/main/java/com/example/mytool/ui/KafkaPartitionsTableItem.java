package com.example.mytool.ui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;

import java.util.List;


public class KafkaPartitionsTableItem {
    public static final List<String> FIELD_NAMES = List.of("partition", "startOffset", "endOffset", "noMessage", "leader", "replicasInSync", "replicasNotInSync");
    private final SimpleIntegerProperty partition;
    private final SimpleLongProperty startOffset;
    private final SimpleLongProperty endOffset;
    private final SimpleLongProperty noMessage;
    private final SimpleStringProperty leader;
    private final SimpleListProperty replicasInSync;
    private final SimpleListProperty replicasNotInSync;


    public KafkaPartitionsTableItem(int partition, long startOffset, long endOffset, long noMessage, String leader, ObservableList<String> replicasInSync, ObservableList<String> replicasNotInSync) {
        this.partition = new SimpleIntegerProperty(partition);
        this.startOffset = new SimpleLongProperty(startOffset);
        this.endOffset = new SimpleLongProperty(endOffset);
        this.noMessage = new SimpleLongProperty(noMessage);
        this.leader = new SimpleStringProperty(leader);
        this.replicasInSync = new SimpleListProperty(replicasInSync);
        this.replicasNotInSync = new SimpleListProperty(replicasNotInSync);
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

    public long getNoMessage() {
        return noMessage.get();
    }

    public SimpleLongProperty noMessageProperty() {
        return noMessage;
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

    public SimpleListProperty replicasInSyncProperty() {
        return replicasInSync;
    }

    public Object getReplicasNotInSync() {
        return replicasNotInSync.get();
    }

    public SimpleListProperty replicasNotInSyncProperty() {
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

    public void setNoMessage(long noMessage) {
        this.noMessage.set(noMessage);
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
