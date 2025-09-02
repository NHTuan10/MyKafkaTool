package io.github.nhtuan10.mykafkatool.manager;

import io.github.nhtuan10.mykafkatool.api.auth.AuthConfig;
import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.consumer.creator.ConsumerCreator;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.producer.creator.ProducerCreator;
import io.github.nhtuan10.mykafkatool.ui.consumergroup.ConsumerGroupTableItem;
import io.github.nhtuan10.mykafkatool.ui.consumergroup.ConsumerGroupTableItemFXModel;
import io.github.nhtuan10.mykafkatool.ui.consumergroup.ConsumerTableItem;
import io.github.nhtuan10.mykafkatool.ui.consumergroup.ConsumerTableItemFXModel;
import io.github.nhtuan10.mykafkatool.ui.util.ViewUtils;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    @Getter
    private final Map<KafkaCluster, List<KafkaTopic>> clusterTopicCache = new ConcurrentHashMap<>();

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
    public void connectToCluster(KafkaCluster cluster) {
        String clusterName = cluster.getName();
//        if (adminMap.containsKey(clusterName)) {
//            throw new ClusterNameExistedException(clusterName, "Cluster already exists");
//        }
        Map<String, Object> properties = new HashMap<>();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServer());
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, AppConstant.DEFAULT_ADMIN_REQUEST_TIMEOUT);
        properties.put(AdminClientConfig.SOCKET_CONNECTION_SETUP_TIMEOUT_MS_CONFIG, AppConstant.DEFAULT_ADMIN_REQUEST_TIMEOUT);
        properties.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, AppConstant.DEFAULT_ADMIN_REQUEST_TIMEOUT);
        properties.put(AdminClientConfig.RETRIES_CONFIG, 2);
        AuthConfig authConfig = cluster.getAuthConfig();
        properties.putAll(authProviderManager.getKafkaAuthProperties(authConfig));
        Admin adminClient = Admin.create(properties);
        adminMap.put(clusterName, adminClient);
        ProducerCreator.ProducerCreatorConfig producerCreatorConfig = ProducerCreator.ProducerCreatorConfig.builder().cluster(cluster).build();
        KafkaProducer producer = producerCreator.createProducer(producerCreatorConfig);
        producerMap.put(producerCreatorConfig, producer);
        ViewUtils.runBackgroundTask(() ->  this.getAllTopics(cluster), (e) -> log.info("Background loaded all topics from cluster {} successfully", clusterName), (e) -> log.error("Error when background get all topics from cluster {}", clusterName, e));
    }

    public void closeClusterConnection(String clusterName) {

        Admin adminClient = adminMap.get(clusterName);
        try {
            adminClient.close();
        } catch (Exception e) {
            log.warn("Error when disconnected an cluster");
        }
        new HashMap<>(producerMap).forEach((producerCreatorConfig, producer) -> {
            if (producerCreatorConfig.getClusterName().equals(clusterName)) {
                producer.close();
                producerMap.remove(producerCreatorConfig);
            }
        });
        adminMap.remove(clusterName);
    }

    public List<KafkaTopic> getAllTopics(KafkaCluster kafkaCluster) throws ExecutionException, InterruptedException, TimeoutException {
        Admin adminClient = adminMap.get(kafkaCluster.getName());
        ListTopicsResult result = adminClient.listTopics();
        Set<String> topicNames = result.names().get(10, TimeUnit.SECONDS);
        List<KafkaTopic> topics = getTopicDesc(kafkaCluster.getName(), topicNames).values().stream()
                .map(topicDescription -> {
                    KafkaTopic topic = new KafkaTopic(topicDescription.name(), kafkaCluster, new ArrayList<>());
                    topic.partitions().addAll(topicDescription.partitions().stream().map(topicPartInfo -> new KafkaPartition(topicPartInfo.partition(), topic)).toList());
                    return topic;
                }).toList();
        clusterTopicCache.put(kafkaCluster, topics);
        return topics;
    }

    public TopicDescription getTopicDesc(String clusterName, String topic) throws ExecutionException, InterruptedException {
        return getTopicDesc(clusterName, Set.of(topic)).get(topic);
    }

    public Map<String, TopicDescription> getTopicDesc(String clusterName, Set<String> topics) {
        Admin adminClient = adminMap.get(clusterName);
        DescribeTopicsResult result = adminClient.describeTopics(topics);
        return result.topicNameValues().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
            try {
                return e.getValue().get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }));
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
        return adminClient
                .listOffsets(topicPartitions.stream().collect(Collectors.toMap(tp -> tp, tp -> startOffsetSpec)))
                .all().get().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().offset()));
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

    public Collection<ConsumerGroupListing> getConsumerGroupList(String clusterName) throws ExecutionException, InterruptedException, TimeoutException {
        return adminMap.get(clusterName).listConsumerGroups().all().get(10, TimeUnit.SECONDS);
    }


    public Map<String, ConsumerGroupDescription> getConsumerGroup(String clusterName, List<String> consumerGroupIds) throws ExecutionException, InterruptedException {
        return adminMap.get(clusterName).describeConsumerGroups(consumerGroupIds).all().get();
    }

    public Collection<ConsumerGroupTableItem> describeConsumerGroupDetails(String clusterName, List<String> consumerGroupId) throws ExecutionException, InterruptedException {
        return describeConsumerDetailMap(clusterName, consumerGroupId).entrySet().stream()
                .flatMap(e ->
                        e.getValue().stream().collect(Collectors.groupingBy(ConsumerTableItem::getTopic, Collectors.collectingAndThen(Collectors.toList(), list -> {
                            if (!list.isEmpty()) {
                                String topic = list.get(0).getTopic();
                                String lag = "";
                                if (list.stream().map(ConsumerTableItem::getLag).allMatch(StringUtils::isNotBlank)) {
                                    lag = String.valueOf(list.stream().map(ConsumerTableItem::getLag).mapToLong(Long::parseLong).sum());
                                }
                                int numberOfMember = list.stream().map(ConsumerTableItem::getConsumerID).filter(Objects::nonNull).collect(Collectors.toSet()).size();
                                String state = list.get(0).getState().toString();

                                return ConsumerGroupTableItemFXModel.builder()
                                        .groupID(e.getKey())
                                        .topic(topic)
                                        .lag(lag)
                                        .numberOfMembers(numberOfMember)
                                        .state(state)
                                        .build();
                            }
                            return null;
                        }))).values().stream()).toList();

    }

    public List<ConsumerTableItem> describeConsumerDetails(String clusterName, List<String> consumerGroupIdList) throws ExecutionException, InterruptedException {
        return describeConsumerDetailMap(clusterName, consumerGroupIdList).values().stream().flatMap(Collection::stream).toList();
    }

    private Map<String, List<ConsumerTableItem>> describeConsumerDetailMap(String clusterName, List<String> consumerGroupIdList) throws ExecutionException, InterruptedException {
        CompletableFuture<Map<String, Map<TopicPartition, OffsetAndMetadata>>> cgOffsetFuture = CompletableFuture.supplyAsync(() ->
                consumerGroupIdList.parallelStream().collect(Collectors.toMap(consumerGroupId -> consumerGroupId, consumerGroupId -> {
                    try {
                        return adminMap.get(clusterName).listConsumerGroupOffsets(consumerGroupId).partitionsToOffsetAndMetadata().get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Error when list consumer group offsets {}", consumerGroupId, e);
                        throw new RuntimeException(e);
                    }
                })));

        CompletableFuture<Map<String, ConsumerGroupDescription>> cgDetailsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return getConsumerGroup(clusterName, consumerGroupIdList);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error when describe consumer group {}", consumerGroupIdList, e);
                throw new RuntimeException(e);
            }
        });

        return CompletableFuture.allOf(cgOffsetFuture, cgDetailsFuture).thenApply((v) -> {
            try {
                Map<String, Map<TopicPartition, OffsetAndMetadata>> cgOffsets = cgOffsetFuture.get();
                Map<String, ConsumerGroupDescription> cgDetails = cgDetailsFuture.get();
                if (cgDetails != null && !cgDetails.isEmpty()) {
                    return consumerGroupIdList.stream().collect(Collectors.toMap(cg -> cg, consumerGroupId ->
                            {
                                try {
                                    return enrichAndMapToConsumerTableItems(clusterName, cgDetails.get(consumerGroupId), cgOffsets.get(consumerGroupId)).stream().toList();
                                } catch (ExecutionException | InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    ));
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            return Map.<String, List<ConsumerTableItem>>of();
        }).get();
    }

    private List<ConsumerTableItem> enrichAndMapToConsumerTableItems(String clusterName, ConsumerGroupDescription
            consumerGroupDescription, Map<TopicPartition, OffsetAndMetadata> cgOffsets) throws ExecutionException, InterruptedException {
//        List<TopicPartition> partitions = consumerGroupDescription.members().stream()
//                .collect(Collectors.toMap(m ->  m, m.assignment().topicPartitions().stream()).toList();
        Map<TopicPartition, MemberDescription> topicPartitionToMemberAssignmentMap = new HashMap<>();
        for (var member : consumerGroupDescription.members()) {
            topicPartitionToMemberAssignmentMap.putAll(member.assignment().topicPartitions().stream().collect(Collectors.toMap(tp -> tp, tp -> member)));
        }
        Map<TopicPartition, Pair<Long, Long>> startAndEndOffsets = getPartitionOffsetInfo(clusterName, cgOffsets.keySet().stream().toList(), null, null);

        return cgOffsets.entrySet().stream().map(entry -> {
//        return consumerGroupDescription.members().parallelStream().flatMap(member -> {
//                List<TopicPartition> partitions = member.assignment().topicPartitions().stream().toList();

//                return partitions.stream().map(tp -> {
            OffsetAndMetadata metadata = entry.getValue();
            String offset = null;
            String lag = null;
            String leaderEpoch = null;
            TopicPartition tp = entry.getKey();
            Pair<Long, Long> startEndOffsetPair = startAndEndOffsets.get(tp);
            Long endOffset = startEndOffsetPair.getRight();
            if (metadata != null) {
                offset = String.valueOf(metadata.offset());
                lag = String.valueOf(endOffset - metadata.offset());
                leaderEpoch = metadata.leaderEpoch().orElse(0).toString();
            }
            var memberOpt = Optional.ofNullable(topicPartitionToMemberAssignmentMap.get(tp));
            return ConsumerTableItemFXModel.builder()
                    .groupID(consumerGroupDescription.groupId())
                    .consumerID(memberOpt.map(MemberDescription::consumerId).orElse(null))
                    .topic(tp.topic())
                    .partition(tp.partition())
                    .start(startEndOffsetPair.getLeft())
                    .end(endOffset)
                    .committedOffset(offset)
                    .lag(lag)
                    .lastCommit(leaderEpoch)
                    .state(consumerGroupDescription.state())
                    .host(memberOpt.map(MemberDescription::host).orElse(null))
                    .build();
        }).toList();
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

    public void resetConsumerGroupOffset(String clusterName, String groupId, String topic, OffsetSpec offsetSpec) throws ExecutionException, InterruptedException, TimeoutException {
        var topicPartitionList = getTopicPartitions(clusterName, topic).stream().map(tpi -> new TopicPartition(topic, tpi.partition())).toList();
        resetConsumerGroupOffset(clusterName, groupId, topicPartitionList, offsetSpec);
    }

    public void resetConsumerGroupOffset(String clusterName, String groupId, List<TopicPartition> topicPartitionList, OffsetSpec offsetSpec) throws InterruptedException, ExecutionException {
        Map<TopicPartition, OffsetAndMetadata> offsets = getPartitionOffsetsBySpec(topicPartitionList, adminMap.get(clusterName), offsetSpec).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new OffsetAndMetadata(entry.getValue())));
        adminMap.get(clusterName).alterConsumerGroupOffsets(groupId, offsets).all().get();
    }

}

