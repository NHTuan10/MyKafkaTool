package com.example.mytool.ui.util;

import com.example.mytool.manager.ClusterManager;
import com.example.mytool.model.kafka.KafkaCluster;
import com.example.mytool.ui.ConsumerGroupListTreeItem;
import com.example.mytool.ui.KafkaTopicListTreeItem;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

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

        return result.orElse(cancel) == yes;
    }

    public static void copyDataFromTableToClipboard(TableView tableView) {
        tableView.getSelectionModel().setCellSelectionEnabled(true);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        MenuItem item = new MenuItem("Copy");
        item.setOnAction(event -> {
            ObservableList<TablePosition> posList = tableView.getSelectionModel().getSelectedCells();
            int old_r = -1;
            StringBuilder clipboardString = new StringBuilder();
            for (TablePosition p : posList) {
                int r = p.getRow();
                int c = p.getColumn();
                Object cell = ((TableColumn) tableView.getColumns().get(c)).getCellData(r);
                if (cell == null)
                    cell = "";
                if (old_r == r)
                    clipboardString.append('\t');
                else if (old_r != -1)
                    clipboardString.append('\n');
                clipboardString.append(cell);
                old_r = r;
            }
            final ClipboardContent content = new ClipboardContent();
            content.putString(clipboardString.toString());
            Clipboard.getSystemClipboard().setContent(content);
        });
        ContextMenu menu = new ContextMenu();
        menu.getItems().add(item);
        tableView.setContextMenu(menu);
    }
}
