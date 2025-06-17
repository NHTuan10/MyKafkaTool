package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.ui.control.EditableTableControl;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

@Slf4j
public class AbstractConsumerGroupTable<T extends ConsumerGroupTopic> extends EditableTableControl<T> {
    protected String clusterName;
    protected List<String> consumerGroupIds;
    protected final ObjectProperty<KafkaTopic> topic;
    protected final ClusterManager clusterManager;
    protected BooleanProperty isBusy;

    public AbstractConsumerGroupTable() {
        super(false);
        this.clusterManager = MyKafkaToolApplication.DAGGER_APP_COMPONENT.clusterManager();
        this.topic = new SimpleObjectProperty<>();
    }

    public void loadCG(String clusterName, List<String> consumerGroupIds, BooleanProperty isBusy) throws Exception {
        this.clusterName = clusterName;
        this.consumerGroupIds = consumerGroupIds;
        this.topic.set(null);
        this.isBusy = isBusy;
        refresh();
    }

    public void loadCG(KafkaTopic topic, BooleanProperty isBusy) throws Exception {
        this.clusterName = topic.cluster().getName();
        this.topic.set(topic);
        this.isBusy = isBusy;
        refresh();
    }

    protected void loadConsumerGroupDetails(BiFunction<String, List<String>, Collection<T>> describeConsumerGroupDetails, List<String> sortColumnNames) {
        isBusy.set(true);
        ViewUtils.runBackgroundTask(() -> {
            try {
                if (topic.get() != null) {
                    this.consumerGroupIds = clusterManager.getConsumerGroupList(clusterName).stream().map(ConsumerGroupListing::groupId).toList();
                    return FXCollections.observableArrayList(describeConsumerGroupDetails.apply(clusterName, consumerGroupIds).stream()
                            .filter(item -> item.getTopic().equals(topic.get().name())).toList());
                } else {
                    return FXCollections.observableArrayList(describeConsumerGroupDetails.apply(clusterName, consumerGroupIds));
                }
            } catch (ExecutionException | InterruptedException e) {
                log.error("Error when get consumer group offsets", e);
                throw new RuntimeException(e);
            } catch (RuntimeException e) {
                throw e;
            }
        }, (items) -> {
            isBusy.set(false);
            setItems(items);
            ObservableList<TableColumn<T, ?>> sortOrder = table.getSortOrder();
            if (sortOrder.isEmpty()) {
                List<TableColumn<T, ?>> sortedColumns = table.getColumns().stream()
                        .filter(c -> sortColumnNames.contains(c.getId()))
                        .toList();
                sortedColumns.forEach(c -> c.setSortType(TableColumn.SortType.ASCENDING));
                table.getSortOrder().addAll(sortedColumns);
                table.sort();
            }
        }, (e) -> {
            isBusy.set(false);
            setItems(FXCollections.emptyObservableList());
            throw ((RuntimeException) e);
        });
    }


    public void resetCG(CGResetOption resetOption, String groupId, List<TopicPartition> topicPartitionList) throws ExecutionException, InterruptedException {
        resetCG(resetOption, groupId, topicPartitionList, null);
    }

    public void resetCG(CGResetOption resetOption, String groupId, List<TopicPartition> topicPartitionList, Long startTimestamp) throws ExecutionException, InterruptedException {

        switch (resetOption) {
            case EARLIEST ->
                    clusterManager.resetConsumerGroupOffset(clusterName, groupId, topicPartitionList, OffsetSpec.earliest());
            case LATEST ->
                    clusterManager.resetConsumerGroupOffset(clusterName, groupId, topicPartitionList, OffsetSpec.latest());
            case START_TIMESTAMP -> {
                if (startTimestamp == null) {
                    throw new RuntimeException("Start timestamp is null when reset consumer group offset");
                } else {
                    clusterManager.resetConsumerGroupOffset(clusterName, groupId, topicPartitionList, OffsetSpec.forTimestamp(startTimestamp));
                }
            }
        }
    }

    public ReadOnlyObjectProperty<T> selectedItemProperty() {
        return table.getSelectionModel().selectedItemProperty();
    }
}
