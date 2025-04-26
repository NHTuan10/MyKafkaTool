package com.example.mytool.ui.util;

import com.example.mytool.ModalController;
import com.example.mytool.MyApplication;
import com.example.mytool.exception.ClusterNameExistedException;
import com.example.mytool.model.kafka.KafkaCluster;
import com.example.mytool.ui.ConsumerGroupListTreeItem;
import com.example.mytool.ui.KafkaPartitionsTableItem;
import com.example.mytool.ui.KafkaTopicListTreeItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


public class ViewUtil {

    public static void addClusterConnIntoClusterTreeView(TreeView clusterTree, KafkaCluster cluster) throws IOException, ClusterNameExistedException {
        String clusterName = cluster.getName();
        if (isClusterNameExistedInTree(clusterTree, clusterName)) {
            throw new ClusterNameExistedException(clusterName, "Cluster already exists");
        }

        TreeItem<Object> brokerTreeItem = new TreeItem<>(clusterName);
        TreeItem<Object> topicListTreeItem = new KafkaTopicListTreeItem<>(new KafkaTopicListTreeItem.KafkaTopicListTreeItemValue(cluster));
        ConsumerGroupListTreeItem<Object> consumerGroupListTreeItem = new ConsumerGroupListTreeItem<>(new ConsumerGroupListTreeItem.ConsumerGroupListTreeItemValue(cluster));

        topicListTreeItem.getChildren();
        brokerTreeItem.getChildren().add(topicListTreeItem);

        consumerGroupListTreeItem.getChildren();
        brokerTreeItem.getChildren().add(consumerGroupListTreeItem);

        clusterTree.getRoot().getChildren().add(brokerTreeItem);
    }

    public static boolean isClusterNameExistedInTree(TreeView clusterTree, String clusterName) throws ClusterNameExistedException {
        return ((ObservableList<TreeItem>) clusterTree.getRoot().getChildren()).stream()
                .filter(treeItem -> treeItem.getValue().equals(clusterName)).findAny().isPresent();
    }

    public static boolean confirmAlert(String title, String text, String okDoneText, String cancelCloseText) {
        ButtonType yes = new ButtonType(okDoneText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(cancelCloseText, ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, yes, cancel);
        alert.setTitle(title);
        Optional<ButtonType> result = alert.showAndWait();

        return result.orElse(cancel) == yes;
    }

    public static void enableCopyDataFromTableToClipboard(TableView tableView) {
        tableView.getSelectionModel().setCellSelectionEnabled(true);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        MenuItem item = new MenuItem("Copy");
        item.setOnAction(event -> {
            copyTableSelectionToClipboard(tableView);
        });
        ContextMenu menu = new ContextMenu();
        menu.getItems().add(item);
        tableView.setContextMenu(menu);

        final KeyCodeCombination keyCodeCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.META_DOWN);
        tableView.setOnKeyPressed(event -> {
            if (keyCodeCopy.match(event)) {
                copyTableSelectionToClipboard(tableView);
            }
        });
    }

    public static void copyTableSelectionToClipboard(TableView tableView) {
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
    }

    public static KafkaPartitionsTableItem mapToUIPartitionTableItem(TopicPartitionInfo partitionInfo, Pair<Long, Long> partitionOffsetsInfo) {
        Node leader = partitionInfo.leader();
        return new KafkaPartitionsTableItem(
                partitionInfo.partition(),
                partitionOffsetsInfo.getLeft(),
                partitionOffsetsInfo.getRight(),
                partitionOffsetsInfo.getRight() - partitionOffsetsInfo.getLeft(),
                leader.host() + ":" + leader.port(),
                FXCollections.observableArrayList(partitionInfo.isr().stream().filter(r -> r != leader).map(replica -> replica.host() + ":" + replica.port()).toList()),
                FXCollections.observableArrayList(partitionInfo.replicas().stream().filter(r -> r != leader && !partitionInfo.isr().contains(r)).map(replica -> replica.host() + ":" + replica.port()).toList()));
    }

    //    private Tuple2<String, String> showAddMsgModalAndGetResult() throws IOException {
    public static void showAddModal(String modalFxml, String title, AtomicReference<Object> modelRef, final Map<String, String> inputVarMap) throws IOException {
        Stage stage = new Stage();
//        FXMLLoader addMsgModalLoader = new FXMLLoader(
//                AddMessageModalController.class.getResource("add-message-modal.fxml"));

        FXMLLoader modalLoader = new FXMLLoader(
                MyApplication.class.getResource(modalFxml));
        Parent parent = modalLoader.load();

//        AddMessageModalController addMessageModalController =  modalLoader.getController();
        ModalController modalController = modalLoader.getController();
//        modalController.setParentController(parentController);
        modalController.setModelRef(modelRef);
        modalController.setTextFieldOrAreaText(modalController, inputVarMap);
        stage.setTitle(title);
        stage.initModality(Modality.WINDOW_MODAL);
//        ActionEvent event
//        stage.initOwner(
//                ((Node)event.getSource()).getScene().getWindow() );
        stage.setScene(new Scene(parent));
        stage.showAndWait();
//        return modelRef.get();
    }


    public static void showAlertDialog(Alert.AlertType alertType, String text, String title, ButtonType... buttonTypes) {
        Alert alert = new Alert(alertType, text, buttonTypes);
        if (title != null) {
            alert.setTitle(title);
        }

        alert.showAndWait();
    }
}
