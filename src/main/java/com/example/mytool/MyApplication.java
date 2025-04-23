package com.example.mytool;

import com.example.mytool.constant.AppConstant;
import com.example.mytool.manager.UserPreferenceManager;
import com.example.mytool.ui.ConsumerGroupOffsetTableItem;
import com.example.mytool.ui.KafkaMessageTableItem;
import com.example.mytool.ui.util.ViewUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.io.IOException;

public class MyApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MyApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("MyTool");
        stage.setScene(scene);
        stage.show();
//        stage.setResizable(false);
        initClusterPanel(stage);
        initTopicConfigUI(stage);
    }

    private void initClusterPanel(Stage stage) {
        TreeView clusterTree = (TreeView) stage.getScene().lookup("#clusterTree");

        TreeItem<Object> clustersItem = new TreeItem<>(AppConstant.TREE_ITEM_CLUSTERS_DISPLAY_NAME);
        clustersItem.setExpanded(true);
        clusterTree.setRoot(clustersItem);

        UserPreferenceManager.loadUserPreference().connections().forEach((cluster -> {
            try {
                ViewUtil.addClusterConnIntoClusterTreeView(clusterTree, cluster);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        TableView<KafkaMessageTableItem> messageTableView = configureMessageTableView(stage);
        TableView<ConsumerGroupOffsetTableItem> consumerGroupOffsetTableView = configureConsumerGroupOffsetTableView(stage);
        // Use a change listener to respond to a selection within
        // a tree view
//        clusterTree.getSelectionModel().selectedItemProperty().addListener((ChangeListener<TreeItem<String>>) (changed, oldVal, newVal) -> {
//
//
//        });


//        try {
//            ClusterManager.getInstance().getTopicDesc("local-9092","demo");
//            ClusterManager.getInstance().getTopicConfig("local-9092","demo");
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (TimeoutException e) {
//            e.printStackTrace();
//        }
//        for (int i = 1; i < 6; i++) {
//            TreeItem<Object> item = new KafkaTopicTreeItem<>(new KafkaTopic("Topic" + i));
//            for (int j = 0; j < 10; j++) {
//                item.getChildren().add(new TreeItem<>(new KafkaPartition("Partition" + j)));
//            }
//            clustersItem.getChildren().add(item);
//        }


//        TreeView<String> tree = new TreeView<String> (rootItem);

//        clusterTree.setEditable(true);
//        clusterTree.setCellFactory((Callback<TreeView<String>, TreeCell<String>>) p -> new TextFieldTreeCellImpl());
    }

    private TableView<KafkaMessageTableItem> configureMessageTableView(Stage stage) {
        TableView<KafkaMessageTableItem> messageTableView = (TableView<KafkaMessageTableItem>) stage.getScene().lookup("#messageTable");
        messageTableView.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("partition"));

        TableColumn<KafkaMessageTableItem, Long> offset = (TableColumn<KafkaMessageTableItem, Long>) messageTableView.getColumns().get(1);
        offset.setCellValueFactory(new PropertyValueFactory<>("offset"));

        TableColumn<KafkaMessageTableItem, String> key = (TableColumn<KafkaMessageTableItem, String>) messageTableView.getColumns().get(2);
        key.setCellValueFactory(new PropertyValueFactory<>("key"));

        TableColumn<KafkaMessageTableItem, String> value = (TableColumn<KafkaMessageTableItem, String>) messageTableView.getColumns().get(3);
        value.setCellValueFactory(new PropertyValueFactory<>("value"));

        TableColumn<KafkaMessageTableItem, String> timestamp = (TableColumn<KafkaMessageTableItem, String>) messageTableView.getColumns().get(4);
        timestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        return messageTableView;
    }

    private TableView<ConsumerGroupOffsetTableItem> configureConsumerGroupOffsetTableView(Stage stage) {
        TableView<ConsumerGroupOffsetTableItem> messageTableView = (TableView<ConsumerGroupOffsetTableItem>) stage.getScene().lookup("#consumerGroupOffsetTable");
        messageTableView.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("topic"));

        messageTableView.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("partition"));

        messageTableView.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("start"));

        messageTableView.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("end"));

        messageTableView.getColumns().get(4).setCellValueFactory(new PropertyValueFactory<>("offset"));

        messageTableView.getColumns().get(5).setCellValueFactory(new PropertyValueFactory<>("lag"));

        messageTableView.getColumns().get(6).setCellValueFactory(new PropertyValueFactory<>("lastCommit"));

        return messageTableView;
    }


    private void initTopicConfigUI(Stage stage) {
        TableView<KafkaMessageTableItem> topicConfigTable = (TableView<KafkaMessageTableItem>) stage.getScene().lookup("#topicConfigTable");
        TableColumn<KafkaMessageTableItem, Long> partition = (TableColumn<KafkaMessageTableItem, Long>) topicConfigTable.getColumns().get(0);
        partition.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<KafkaMessageTableItem, Long> offset = (TableColumn<KafkaMessageTableItem, Long>) topicConfigTable.getColumns().get(1);
        offset.setCellValueFactory(new PropertyValueFactory<>("value"));

    }

    public static void main(String[] args) {
        launch();
    }

    private final class TextFieldTreeCellImpl extends TreeCell<String> {

        private TextField textField;

        public TextFieldTreeCellImpl() {
        }

        @Override
        public void startEdit() {
            super.startEdit();

            if (textField == null) {
                createTextField();
            }
            setText(null);
            setGraphic(textField);
            textField.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText((String) getItem());
            setGraphic(getTreeItem().getGraphic());
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(getTreeItem().getGraphic());
                }
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setOnKeyReleased(t -> {
                if (t.getCode() == KeyCode.ENTER) {
                    commitEdit(textField.getText());
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }
    }
}