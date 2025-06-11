package io.github.nhtuan10.mykafkatool.manager;

import io.github.nhtuan10.mykafkatool.api.auth.AuthConfig;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.consumer.creator.ConsumerCreator;
import io.github.nhtuan10.mykafkatool.exception.ClusterNameExistedException;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.producer.creator.ProducerCreator;
import io.github.nhtuan10.mykafkatool.ui.cg.ConsumerGroupOffsetTableItem;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
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
@AppScoped
public class ClusterManager {

    private final Map<String, Admin> adminMap;
    private final Map<ProducerCreator.ProducerCreatorConfig, KafkaProducer> producerMap;
    private final AuthProviderManager authProviderManager;
    private final ProducerCreator producerCreator;
    private final ConsumerCreator consumerCreator;

    //    private static class InstanceHolder {
//        private static  ClusterManager INSTANCE = new ClusterManager(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    private static ClusterManager INSTANCE;
//    }

    public static ClusterManager getInstance() {
//        return DaggerAppComponent.create().clusterManager();
        return INSTANCE;
    }

    @Inject
    public ClusterManager(AuthProviderManager authProviderManager, ProducerCreator producerCreator, ConsumerCreator consumerCreator) {
//        this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        this.adminMap = new ConcurrentHashMap<>();
        this.producerMap = new ConcurrentHashMap<>();
        this.authProviderManager = authProviderManager;
        this.producerCreator = producerCreator;
        this.consumerCreator = consumerCreator;
        INSTANCE = this;
    }

//    private ClusterManager(Map<String, Admin> adminMap, Map<ProducerCreator.ProducerCreatorConfig, KafkaProducer> producerMap) {
//        this.adminMap = adminMap;
//        this.producerMap = producerMap;
//    }

    @SneakyThrows
    public void connectToCluster(KafkaCluster cluster) throws ClusterNameExistedException {
        String clusterName = cluster.getName();
        if (adminMap.containsKey(clusterName)) {
            throw new ClusterNameExistedException(clusterName, "Cluster already exists");
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServer());
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, AppConstant.DEFAULT_ADMIN_REQUEST_TIMEOUT);
        AuthConfig authConfig = cluster.getAuthConfig();
        properties.putAll(authProviderManager.getKafkaAuthProperties(authConfig));
        Admin adminClient = Admin.create(properties);
        adminMap.put(clusterName, adminClient);
        ProducerCreator.ProducerCreatorConfig producerCreatorConfig = ProducerCreator.ProducerCreatorConfig.builder().cluster(cluster).build();
        KafkaProducer producer = producerCreator.createProducer(producerCreatorConfig);
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
        return consumerCreator.createConsumer(consumerProperties);
    }

    public KafkaProducer getProducer(ProducerCreator.ProducerCreatorConfig producerCreatorConfig) {
        if (producerMap.containsKey(producerCreatorConfig)) {
            return producerMap.get(producerCreatorConfig);
        } else {
            KafkaProducer producer = producerCreator.createProducer(producerCreatorConfig);
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
        return getPartitionOffsetInfo(clusterName, List.of(topicPartition), startTimestamp, endTimestamp).get(topicPartition);
    }

    public Map<TopicPartition, Pair<Long, Long>> getPartitionOffsetInfo(String clusterName, List<TopicPartition> topicPartitions, Long startTimestamp, Long endTimestamp) throws ExecutionException, InterruptedException {
        Admin adminClient = adminMap.get(clusterName);
        OffsetSpec startOffsetSpec = startTimestamp != null ? OffsetSpec.forTimestamp(startTimestamp) : OffsetSpec.earliest();
        OffsetSpec endOffsetSpec = endTimestamp != null ? OffsetSpec.forTimestamp(endTimestamp) : OffsetSpec.latest();

        Map<TopicPartition, Long> earliestOffsetsResultMap = getPartitionOffsetsBySpec(topicPartitions, adminClient, startOffsetSpec);

        Map<TopicPartition, Long> latestOffsetsResultMap = getPartitionOffsetsBySpec(topicPartitions, adminClient, endOffsetSpec);

        List<TopicPartition> negativeOffsetlist = latestOffsetsResultMap.entrySet().stream()
                .filter(entry -> entry.getValue() < 0)
                .map(Map.Entry::getKey)
                .toList();

        if (!negativeOffsetlist.isEmpty()) { // if it return -1 , mean endTimestamp > the last offset timestamp
            latestOffsetsResultMap.putAll(getPartitionOffsetsBySpec(negativeOffsetlist, adminClient, OffsetSpec.latest()));
        }
        return topicPartitions.stream().collect(Collectors.toMap(tp -> tp, tp -> Pair.of(earliestOffsetsResultMap.get(tp), latestOffsetsResultMap.get(tp))));
//        return Pair.of(earliestOffsetsResultMap.offset(), latestOffsetsResultMap.offset());
    }

    private Map<TopicPartition, Long> getPartitionOffsetsBySpec(List<TopicPartition> topicPartitions, Admin adminClient, OffsetSpec startOffsetSpec) throws InterruptedException, ExecutionException {
        Map<TopicPartition, Long> earliestOffsetsResultInfo = adminClient
                .listOffsets(topicPartitions.stream().collect(Collectors.toMap(tp -> tp, tp -> startOffsetSpec)))
                .all().get().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().offset()));
        return earliestOffsetsResultInfo;
    }

//    public Pair<Long, Long> getPartitionOffsetInfo(String clusterName, TopicPartition topicPartition, Long startTimestamp, Long endTimestamp) throws ExecutionException, InterruptedException {
//        Admin adminClient = adminMap.get(clusterName);
//        OffsetSpec startOffsetSpec = OffsetSpec.earliest();
//        if (startTimestamp != null) {
//            startOffsetSpec = OffsetSpec.forTimestamp(startTimestamp);
//        }
//        OffsetSpec endOffsetSpec = OffsetSpec.latest();
//        if (endTimestamp != null) {
//            endOffsetSpec = OffsetSpec.forTimestamp(endTimestamp);
//        }
//        ListOffsetsResult.ListOffsetsResultInfo earliestOffsetsResultInfo = adminClient.listOffsets(Map.of(topicPartition, startOffsetSpec))
//                .partitionResult(topicPartition).get();
//        ListOffsetsResult.ListOffsetsResultInfo latestOffsetsResultInfo = adminClient.listOffsets(Map.of(topicPartition, endOffsetSpec))
//                .partitionResult(topicPartition).get();
//        if (latestOffsetsResultInfo.offset() < 0) { // if it return -1 , mean endTimestamp > the last offset timestamp
//            latestOffsetsResultInfo = adminClient.listOffsets(Map.of(topicPartition, OffsetSpec.latest()))
//                    .partitionResult(topicPartition).get();
//        }
//        return Pair.of(earliestOffsetsResultInfo.offset(), latestOffsetsResultInfo.offset());
//    }

    public Map<TopicPartition, Pair<Long, Long>> getAllPartitionOffsetInfo(String clusterName, String topicName, Long startTimestamp, Long endTimestamp) throws ExecutionException, InterruptedException, TimeoutException {
        List<TopicPartition> topicPartitions = getTopicPartitions(clusterName, topicName).stream().map(tpi -> new TopicPartition(topicName, tpi.partition())).toList();
        return getPartitionOffsetInfo(clusterName, topicPartitions, startTimestamp, endTimestamp);
//        return partitionInfoList.stream().collect(Collectors.toMap(p -> new TopicPartition(topicName, p.partition()), p -> {
//            try {
//                return getPartitionOffsetInfo(clusterName, new TopicPartition(topicName, p.partition()), startTimestamp, endTimestamp);
//            } catch (ExecutionException | InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }));
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
                    return consumerGroupDescription.members().stream().flatMap(member -> {
                        try {
                            List<TopicPartition> partitions = member.assignment().topicPartitions().stream().toList();
                            Map<TopicPartition, Pair<Long, Long>> startAndEndOffsets = getPartitionOffsetInfo(clusterName, partitions, null, null);
                            return partitions.stream().map((tp) -> {
                                OffsetAndMetadata metadata = cgOffsets.get(tp);
                                String offset = null;
                                String lag = null;
                                String leaderEpoch = null;
                                Pair<Long, Long> startEndOffsetPair = startAndEndOffsets.get(tp);
                                Long endOffset = startEndOffsetPair.getRight();
                                if (metadata != null) {
                                    offset = String.valueOf(metadata.offset());
                                    lag = String.valueOf(endOffset - metadata.offset());
                                    leaderEpoch = metadata.leaderEpoch().orElse(0).toString();
                                }
                                return new ConsumerGroupOffsetTableItem(member.consumerId(), tp.topic(), tp.partition(), startEndOffsetPair.getLeft(), endOffset, offset, lag, leaderEpoch, member.host());
                            });

                        } catch (ExecutionException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }).toList();
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

