package com.example.mytool.ui;

import com.example.mytool.constant.AppConstant;
import com.example.mytool.exception.ClusterNameExistedException;
import com.example.mytool.manager.ClusterManager;
import com.example.mytool.manager.UserPreferenceManager;
import com.example.mytool.ui.cg.ConsumerGroupOffsetTableItem;
import com.example.mytool.ui.partition.KafkaPartitionsTableItem;
import com.example.mytool.ui.util.ViewUtil;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
public class UIConfigurer {

    public static <T> void configureTableView(Class<T> clazz, String fxId, Stage stage) {
        TableView<T> kafkaPartitionsTableView = (TableView<T>) stage.getScene().lookup("#" + fxId);
        List<String> fieldNames = ViewUtil.getPropertyFieldNamesFromTableItem(clazz);
        IntStream.range(0, fieldNames.size()).forEach(i -> {
            kafkaPartitionsTableView.getColumns().get(i).setCellValueFactory(new PropertyValueFactory<>(fieldNames.get(i)));
        });
//        return kafkaPartitionsTableView;
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
        UIConfigurer.configureTableView(KafkaMessageTableItem.class, "messageTable", stage);
        UIConfigurer.configureTableView(ConsumerGroupOffsetTableItem.class, "consumerGroupOffsetTable", stage);
        UIConfigurer.configureTableView(KafkaPartitionsTableItem.class, "kafkaPartitionsTable", stage);
        UIConfigurer.configureTableView(UIPropertyItem.class, "topicConfigTable", stage);
        // Use a change listener to respond to a selection within
        // a tree view
//        clusterTree.getSelectionModel().selectedItemProperty().addListener((ChangeListener<TreeItem<String>>) (changed, oldVal, newVal) -> {
//
//
//        });


//        TreeView<String> tree = new TreeView<String> (rootItem);

//        clusterTree.setEditable(true);
//        clusterTree.setCellFactory((Callback<TreeView<String>, TreeCell<String>>) p -> new TextFieldTreeCellImpl());
    }
}
