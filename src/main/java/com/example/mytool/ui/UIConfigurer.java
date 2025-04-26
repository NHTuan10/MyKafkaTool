package com.example.mytool.ui;

import com.example.mytool.constant.AppConstant;
import com.example.mytool.exception.ClusterNameExistedException;
import com.example.mytool.manager.ClusterManager;
import com.example.mytool.manager.UserPreferenceManager;
import com.example.mytool.ui.util.ViewUtil;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.stream.IntStream;

@Slf4j
public class UIConfigurer {
    public static TableView<KafkaMessageTableItem> configureMessageTableView(Stage stage) {
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

    public static TableView<ConsumerGroupOffsetTableItem> configureConsumerGroupOffsetTableView(Stage stage) {
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

    public static TableView<KafkaPartitionsTableItem> configureKafkaPartitionsTableView(Stage stage) {
        TableView<KafkaPartitionsTableItem> kafkaPartitionsTableView = (TableView<KafkaPartitionsTableItem>) stage.getScene().lookup("#kafkaPartitionsTable");
        IntStream.range(0, KafkaPartitionsTableItem.FIELD_NAMES.size()).forEach(i -> {
            kafkaPartitionsTableView.getColumns().get(i).setCellValueFactory(new PropertyValueFactory<>(KafkaPartitionsTableItem.FIELD_NAMES.get(i)));
        });
        return kafkaPartitionsTableView;
    }

    public static void configureTopicConfigTableView(Stage stage) {
        TableView<KafkaMessageTableItem> topicConfigTable = (TableView<KafkaMessageTableItem>) stage.getScene().lookup("#topicConfigTable");
        TableColumn<KafkaMessageTableItem, Long> partition = (TableColumn<KafkaMessageTableItem, Long>) topicConfigTable.getColumns().get(0);
        partition.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<KafkaMessageTableItem, Long> offset = (TableColumn<KafkaMessageTableItem, Long>) topicConfigTable.getColumns().get(1);
        offset.setCellValueFactory(new PropertyValueFactory<>("value"));

    }

    public static void initClusterPanel(Stage stage) {
        TreeView clusterTree = (TreeView) stage.getScene().lookup("#clusterTree");

        TreeItem<Object> clustersItem = new TreeItem<>(AppConstant.TREE_ITEM_CLUSTERS_DISPLAY_NAME);
        clustersItem.setExpanded(true);
        clusterTree.setRoot(clustersItem);

        UserPreferenceManager.loadUserPreference().connections().forEach((cluster -> {
            try {
                if (!ViewUtil.isClusterNameExistedInTree(clusterTree, cluster.getName())) {
                    ClusterManager.getInstance().connectToCluster(cluster);
                    ViewUtil.addClusterConnIntoClusterTreeView(clusterTree, cluster);
                }
            } catch (IOException | ClusterNameExistedException e) {
                log.error("Error when add new connection during loading user preferences", e);
                throw new RuntimeException(e);
            }
        }));
        TableView<KafkaMessageTableItem> messageTableView = UIConfigurer.configureMessageTableView(stage);
        TableView<ConsumerGroupOffsetTableItem> consumerGroupOffsetTableView = UIConfigurer.configureConsumerGroupOffsetTableView(stage);
        UIConfigurer.configureKafkaPartitionsTableView(stage);
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
}
