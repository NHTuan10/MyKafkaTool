package io.github.nhtuan10.mykafkatool.ui.topic;

import javafx.scene.control.TreeItem;

public class KafkaPartitionTreeItem<T> extends TreeItem<T> {
    public KafkaPartitionTreeItem(T value) {
        super(value);
    }
}
