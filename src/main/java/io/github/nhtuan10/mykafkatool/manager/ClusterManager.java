package io.github.nhtuan10.mykafkatool.manager;

import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.consumer.creator.ConsumerCreator;
import io.github.nhtuan10.mykafkatool.exception.ClusterNameExistedException;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.producer.creator.ProducerCreator;
import io.github.nhtuan10.mykafkatool.ui.cg.ConsumerGroupOffsetTableItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class ClusterManager {

    private final Map<String, Admin> adminMap;
    private final Map<ProducerCreator.ProducerCreatorConfig, KafkaProducer> producerMap;

    private static class InstanceHolder {
        private static final ClusterManager INSTANCE = new ClusterManager(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    public static ClusterManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private ClusterManager(Map<String, Admin> adminMap, Map<ProducerCreator.ProducerCreatorConfig, KafkaProducer> producerMap) {
        this.adminMap = adminMap;
        this.producerMap = producerMap;
    }

    public void connectToCluster(KafkaCluster cluster) throws ClusterNameExistedException {
        String clusterName = cluster.getName();
        if (adminMap.containsKey(clusterName)) {
            throw new ClusterNameExistedException(clusterName, "Cluster already exists");
        }
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServer());
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, AppConstant.DEFAULT_ADMIN_REQUEST_TIMEOUT);
        Admin adminClient = Admin.create(properties);
        adminMap.put(clusterName, adminClient);
        ProducerCreator.ProducerCreatorConfig producerCreatorConfig = ProducerCreator.ProducerCreatorConfig.builder().cluster(cluster).build();
        KafkaProducer producer = ProducerCreator.createProducer(producerCreatorConfig);
        producerMap.put(producerCreatorConfig, producer);
    }

    public void closeClusterConnection(String clusterName) {

        Admin adminClient = adminMap.get(clusterName);
        adminClient.close();
        new HashMap<>(producerMap).forEach((producerCreatorConfig, producer) -> {
            if (producerCreatorConfig.getClusterName().equals(clusterName)) {
                producer.close();
                producerMap.remove(producerCreatorConfig);
            }
        });
        adminMap.remove(clusterName);
    }

    public Set<String> getAllTopics(String clusterName) throws ExecutionException, InterruptedException, TimeoutException {
        Admin adminClient = adminMap.get(clusterName);
        ListTopicsResult result = adminClient.listTopics();
        return result.names().get(60, TimeUnit.SECONDS);
    }

    public TopicDescription getTopicDesc(String clusterName, String topic) throws ExecutionException, InterruptedException {
        Admin adminClient = adminMap.get(clusterName);
        DescribeTopicsResult result = adminClient.describeTopics(Set.of(topic));
        return result.topicNameValues().get(topic).get();
    }

    public List<TopicPartitionInfo> getTopicPartitions(String clusterName, String topic) throws ExecutionException, InterruptedException, TimeoutException {
        return getTopicDesc(clusterName, topic).partitions();
    }

    public TopicPartitionInfo getTopicPartitionInfo(String clusterName, String topic, int partition) throws ExecutionException, InterruptedException {
        return getTopicDesc(clusterName, topic).partitions().stream().filter(tpi -> tpi.partition() == partition).findFirst().orElse(null);
    }

    public Collection<ConfigEntry> getTopicConfig(String clusterName, String topic) throws ExecutionException, InterruptedException, TimeoutException {
        Admin adminClient = adminMap.get(clusterName);
        Config config = (Config) adminClient.describeConfigs(Set.of(new ConfigResource(ConfigResource.Type.TOPIC, topic))).all().get().values().toArray()[0];
        return config.entries();
    }

    public Consumer createConsumer(Map<String, Object> consumerProperties) {
        return ConsumerCreator.createConsumer(consumerProperties);
    }

    public KafkaProducer getProducer(ProducerCreator.ProducerCreatorConfig producerCreatorConfig) {
        if (producerMap.containsKey(producerCreatorConfig)) {
            return producerMap.get(producerCreatorConfig);
        } else {
            KafkaProducer producer = ProducerCreator.createProducer(producerCreatorConfig);
            producerMap.put(producerCreatorConfig, producer);
            return producer;
        }
    }

    public DeleteTopicsResult deleteTopic(String clusterName, String topicName) {
        return adminMap.get(clusterName).deleteTopics(Set.of(topicName));
    }

    public CreateTopicsResult addTopic(String clusterName, NewTopic newTopic) {
        return adminMap.get(clusterName).createTopics(Set.of(newTopic));
    }

    public Pair<Long, Long> getPartitionOffsetInfo(String clusterName, TopicPartition topicPartition, Long startTimestamp, Long endTimestamp) throws ExecutionException, InterruptedException {
        Admin adminClient = adminMap.get(clusterName);
        OffsetSpec startOffsetSpec = OffsetSpec.earliest();
        if (startTimestamp != null) {
            startOffsetSpec = OffsetSpec.forTimestamp(startTimestamp);
        }
        OffsetSpec endOffsetSpec = OffsetSpec.latest();
        if (endTimestamp != null) {
            endOffsetSpec = OffsetSpec.forTimestamp(endTimestamp);
        }
        ListOffsetsResult.ListOffsetsResultInfo earliestOffsetsResultInfo = adminClient.listOffsets(Map.of(topicPartition, startOffsetSpec))
                .partitionResult(topicPartition).get();
        ListOffsetsResult.ListOffsetsResultInfo latestOffsetsResultInfo = adminClient.listOffsets(Map.of(topicPartition, endOffsetSpec))
                .partitionResult(topicPartition).get();
        if (latestOffsetsResultInfo.offset() < 0) { // if it return -1 , mean endTimestamp > the last offset timestamp
            latestOffsetsResultInfo = adminClient.listOffsets(Map.of(topicPartition, OffsetSpec.latest()))
                    .partitionResult(topicPartition).get();
        }
        return Pair.of(earliestOffsetsResultInfo.offset(), latestOffsetsResultInfo.offset());
    }

    public Map<TopicPartition, Pair<Long, Long>> getAllPartitionOffsetInfo(String clusterName, String topicName, Long startTimestamp, Long endTimestamp) throws ExecutionException, InterruptedException, TimeoutException {
        List<TopicPartitionInfo> partitionInfoList = getTopicPartitions(clusterName, topicName);
        return partitionInfoList.stream().collect(Collectors.toMap(p -> new TopicPartition(topicName, p.partition()), p -> {
            try {
                return getPartitionOffsetInfo(clusterName, new TopicPartition(topicName, p.partition()), startTimestamp, endTimestamp);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public Collection<ConsumerGroupListing> getConsumerGroupList(String clusterName) throws ExecutionException, InterruptedException {
        return adminMap.get(clusterName).listConsumerGroups().all().get();
    }


    public Map<String, ConsumerGroupDescription> getConsumerGroup(String clusterName, List<String> consumerGroupIds) throws ExecutionException, InterruptedException {
        return adminMap.get(clusterName).describeConsumerGroups(consumerGroupIds).all().get();
    }

    public List<ConsumerGroupOffsetTableItem> listConsumerGroupOffsets(String clusterName, String consumerGroupId) throws ExecutionException, InterruptedException {
        CompletableFuture<Map<TopicPartition, OffsetAndMetadata>> cgOffsetFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return adminMap.get(clusterName).listConsumerGroupOffsets(consumerGroupId).partitionsToOffsetAndMetadata().get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error when list consumer group offsets {}", consumerGroupId, e);
                throw new RuntimeException(e);
            }
        });
        CompletableFuture<Map<String, ConsumerGroupDescription>> cgDetailsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return adminMap.get(clusterName).describeConsumerGroups(List.of(consumerGroupId)).all().get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error when describe consumer group {}", consumerGroupId, e);
                throw new RuntimeException(e);
            }
        });

        return CompletableFuture.allOf(cgOffsetFuture, cgDetailsFuture).thenApply((v) -> {
            try {
                Map<TopicPartition, OffsetAndMetadata> cgOffsets = cgOffsetFuture.get();
                Map<String, ConsumerGroupDescription> cgDetails = cgDetailsFuture.get();
                if (cgDetails != null && !cgDetails.isEmpty()) {
                    ConsumerGroupDescription consumerGroupDescription = cgDetails.get(consumerGroupId);
                    return consumerGroupDescription.members().stream().flatMap(member -> member.assignment().topicPartitions().stream().map(tp -> {
                        OffsetAndMetadata metadata = cgOffsets.get(tp);
                        Pair<Long, Long> startAndEndOffset;
                        try {
                            startAndEndOffset = getPartitionOffsetInfo(clusterName, tp, null, null);
                        } catch (ExecutionException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        String offset = null;
                        String lag = null;
                        String leaderEpoch = null;
                        Long endOffset = startAndEndOffset.getRight();
                        if (metadata != null) {
                            offset = String.valueOf(metadata.offset());
                            lag = String.valueOf(endOffset - metadata.offset());
                            leaderEpoch = metadata.leaderEpoch().orElse(0).toString();
                        }
                        return new ConsumerGroupOffsetTableItem(member.consumerId(), tp.topic(), tp.partition(), startAndEndOffset.getLeft(), endOffset, offset, lag, leaderEpoch, member.host());
                    })).toList();
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            return List.<ConsumerGroupOffsetTableItem>of();
        }).get();
    }

    public void purgePartition(KafkaPartition kafkaPartition) throws ExecutionException, InterruptedException {
        String clusterName = kafkaPartition.topic().cluster().getName();
        TopicPartition topicPartition = new TopicPartition(kafkaPartition.topic().name(), kafkaPartition.id());
        long endOffset = getPartitionOffsetInfo(clusterName, topicPartition, null, null).getRight();
        Map<TopicPartition, RecordsToDelete> map = Map.of(
                topicPartition,
                RecordsToDelete.beforeOffset(endOffset)
        );
        adminMap.get(clusterName).deleteRecords(map).all().get();
    }

    public void purgeTopic(KafkaTopic kafkaTopic) throws ExecutionException, InterruptedException, TimeoutException {
        String clusterName = kafkaTopic.cluster().getName();
        Map<TopicPartition, RecordsToDelete> map = getAllPartitionOffsetInfo(clusterName, kafkaTopic.name(), null, null).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, (entry) -> RecordsToDelete.beforeOffset(entry.getValue().getRight())));
        adminMap.get(clusterName).deleteRecords(map).all().get();
    }
}

