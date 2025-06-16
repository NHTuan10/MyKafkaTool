package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import javafx.scene.control.TreeItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ConsumerGroupTreeItem extends TreeItem<String> {

    private List<ConsumerTableItem> consumerTableItemList;
    private String clusterName;
    private String consumerGroupId;

    public ConsumerGroupTreeItem(String displayVal, String clusterName, String consumerGroupId) {
        super(displayVal);
        this.clusterName = clusterName;
        this.consumerGroupId = consumerGroupId;
    }
}
