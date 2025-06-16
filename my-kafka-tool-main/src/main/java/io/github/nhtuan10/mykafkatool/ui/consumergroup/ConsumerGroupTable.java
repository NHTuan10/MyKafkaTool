package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.control.EditableTableControl;
import io.github.nhtuan10.mykafkatool.ui.util.ModalUtils;
import io.github.nhtuan10.mykafkatool.ui.util.TableViewConfigurer;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.OffsetSpec;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

//TODO: create a new CG view which includes this table. Then add info such as number of topic, members, partition, members, total lag, topics, etc.

@Slf4j
public class ConsumerGroupTable extends EditableTableControl<ConsumerGroupTableItem> {
    //    private ConsumerGroupTreeItem consumerGroupTreeItem;
    private String clusterName;
    private List<String> consumerGroupIds;
    private KafkaTopic topic;
    private final ClusterManager clusterManager;
    private BooleanProperty isBusy;

    public ConsumerGroupTable() {
        super(false);
        this.clusterManager = MyKafkaToolApplication.DAGGER_APP_COMPONENT.clusterManager();
    }

    public void loadCG(String clusterName, List<String> consumerGroupIds, BooleanProperty isBusy) {
        this.clusterName = clusterName;
        this.consumerGroupIds = consumerGroupIds;
        this.topic = null;
        this.isBusy = isBusy;
        refresh();
    }

    public void loadCG(KafkaTopic topic, BooleanProperty isBusy) {
        this.clusterName = topic.cluster().getName();
        this.topic = topic;
        this.isBusy = isBusy;
        refresh();
    }

    @Override
    protected void configureTableView() {
        TableViewConfigurer.TableViewConfiguration<ConsumerGroupTableItem> configuration = new TableViewConfigurer.TableViewConfiguration<>(
                SelectionMode.SINGLE
                , false
                , true
                , true
                , new TableViewConfigurer.TableViewConfiguration.ExtraFieldsToCopyAndExport<>(List.of()
                , item -> List.of()));
        configureTableView(configuration);
    }

    @Override
    @FXML
    public void refresh() {
        isBusy.set(true);
        ViewUtils.runBackgroundTask(() -> {
            try {
                if (topic != null) {
                    this.consumerGroupIds = clusterManager.getConsumerGroupList(clusterName).stream().map(ConsumerGroupListing::groupId).toList();
                    return FXCollections.observableArrayList(clusterManager
                            .listConsumerGroupDetails(clusterName, consumerGroupIds).stream()
                            .filter(item -> item.getTopic().equals(topic.name())).toList());
                } else {
                    return FXCollections.observableArrayList(clusterManager.listConsumerGroupDetails(clusterName, consumerGroupIds));
                }
//                return FXCollections.observableArrayList(clusterManager.listConsumerGroupDetails(consumerGroupTreeItem.getClusterName(), List.of(consumerGroupTreeItem.getConsumerGroupId())));
            } catch (ExecutionException | InterruptedException e) {
                log.error("Error when get consumer group offsets", e);
                throw new RuntimeException(e);
            }
        }, (items) -> {
            isBusy.set(false);
            setItems(items);
            ObservableList<TableColumn<ConsumerGroupTableItem, ?>> sortOrder = table.getSortOrder();
            final List<String> sortedColumnNames = List.of(ConsumerGroupTableItemFXModel.TOPIC);
            if (sortOrder.isEmpty()) {
                List<TableColumn<ConsumerGroupTableItem, ?>> sortedColumns = table.getColumns().stream()
                        .filter(c -> sortedColumnNames.contains(c.getId()))
                        .peek(c -> c.setSortType(TableColumn.SortType.ASCENDING))
                        .toList();

                table.getSortOrder().addAll(sortedColumns);
                table.sort();
            }
        }, (e) -> {
            isBusy.set(false);
            setItems(FXCollections.emptyObservableList());
            throw ((RuntimeException) e);
        });
    }

    public void resetCG(CGResetOption resetOption, Long startTimestamp) {
        table.getSelectionModel().getSelectedItems().stream().forEach(item -> {
            try {
                switch (resetOption) {
                    case EARLIEST ->
                            clusterManager.resetConsumerGroupOffset(clusterName, item.getTopic(), item.getGroupId(), OffsetSpec.earliest());
                    case LATEST ->
                            clusterManager.resetConsumerGroupOffset(clusterName, item.getTopic(), item.getGroupId(), OffsetSpec.latest());
                    case START_TIMESTAMP -> {
                        if (startTimestamp == null) {
                            ModalUtils.showAlertDialog(Alert.AlertType.WARNING, "Please select start time to reset", "Please select start time to reset", ButtonType.OK);
                        }
                    }
                }
                ModalUtils.showAlertDialog(Alert.AlertType.INFORMATION, "Reset offset successfully", "Reset offset successfully", ButtonType.OK);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public ReadOnlyObjectProperty<ConsumerGroupTableItem> selectedItemProperty() {
        return table.getSelectionModel().selectedItemProperty();
    }
}
