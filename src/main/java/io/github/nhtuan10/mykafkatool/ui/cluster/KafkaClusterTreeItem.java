package io.github.nhtuan10.mykafkatool.ui.cluster;

import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class KafkaClusterTreeItem extends TreeItem<KafkaCluster> {
    public KafkaClusterTreeItem(KafkaCluster value) {
        super(value);
    }

    public KafkaClusterTreeItem(KafkaCluster value, Node graphic) {
        super(value, graphic);
    }
}
