package com.example.mytool.ui;

import javafx.scene.control.TreeItem;
import lombok.Data;

import java.util.List;

@Data
public class ConsumerGroupTreeItem extends TreeItem<String> {

    private List<ConsumerGroupOffsetTableItem> consumerGroupOffsetTableItemList;
    private String clusterName;
    private String consumerGroupId;

    public ConsumerGroupTreeItem(String displayVal, String clusterName, String consumerGroupId) {
        super(displayVal);
        this.clusterName = clusterName;
        this.consumerGroupId = consumerGroupId;
    }
}
