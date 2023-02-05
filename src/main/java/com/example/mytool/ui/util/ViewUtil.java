package com.example.mytool.ui.util;

import com.example.mytool.manager.ClusterManager;
import com.example.mytool.model.kafka.KafkaCluster;
import com.example.mytool.ui.ConsumerGroupListTreeItem;
import com.example.mytool.ui.KafkaTopicListTreeItem;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.Optional;

public class ViewUtil {

    public static void addClusterConnIntoClusterTreeView(TreeView clusterTree, KafkaCluster cluster) throws IOException {
        ClusterManager.getInstance().connectToCluster(cluster);
        TreeItem<Object> brokerTreeItem = new TreeItem<>(cluster.getName());
        TreeItem<Object> topicListTreeItem = new KafkaTopicListTreeItem<>(new KafkaTopicListTreeItem.KafkaTopicListTreeItemValue(cluster));
        ConsumerGroupListTreeItem<Object> consumerGroupListTreeItem = new ConsumerGroupListTreeItem<>(new ConsumerGroupListTreeItem.ConsumerGroupListTreeItemValue(cluster));

        topicListTreeItem.getChildren();
        brokerTreeItem.getChildren().add(topicListTreeItem);

        consumerGroupListTreeItem.getChildren();
        brokerTreeItem.getChildren().add(consumerGroupListTreeItem);

        clusterTree.getRoot().getChildren().add(brokerTreeItem);
    }

    public static boolean confirmAlert(String title, String text, String okDoneText, String cancelCloseText) {
        ButtonType yes = new ButtonType(okDoneText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(cancelCloseText, ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, yes, cancel);
        alert.setTitle(title);
        Optional<ButtonType> result = alert.showAndWait();

        if (result.orElse(cancel) == yes) {
            return true;
        } else return false;
    }
}
