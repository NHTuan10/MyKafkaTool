package com.example.mytool.manager;

import com.example.mytool.model.ConsumerType;
import com.example.mytool.model.kafka.KafkaCluster;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.ui.ConsumerGroupOffsetTableItem;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ClusterManager {

    private Map<String, Admin> adminMap;
    private Map<Tuple2<String, ConsumerType>, Consumer<String, String>> consumerMap;
    private Map<ProducerCreator.ProducerCreatorConfig, KafkaProducer> producerMap;

    private static class InstanceHolder {
        private static final ClusterManager INSTANCE = new ClusterManager(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    public static ClusterManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public ClusterManager(Map<String, Admin> adminMap, Map<Tuple2<String, ConsumerType>, Consumer<String, String>> consumerMap, Map<ProducerCreator.ProducerCreatorConfig, KafkaProducer> producerMap) {
        this.adminMap = adminMap;
        this.consumerMap = consumerMap;
        this.producerMap = producerMap;
    }

    public void connectToCluster(KafkaCluster cluster) {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServer());
        Admin adminClient = Admin.create(properties);
        adminMap.put(cluster.getName(), adminClient);
//        Consumer<String, String> partitionConsumer = ConsumerCreator.createConsumer(cluster, null);
//        Consumer<String, String> topicConsumer = ConsumerCreator.createConsumer(cluster, null);
//        consumerMap.put(Tuples.of(cluster.getName(), ConsumerType.PARTITION), partitionConsumer);
//        consumerMap.put(Tuples.of(cluster.getName(), ConsumerType.TOPIC), topicConsumer);
        ProducerCreator.ProducerCreatorConfig producerCreatorConfig = ProducerCreator.ProducerCreatorConfig.builder().cluster(cluster).build();
        KafkaProducer producer = ProducerCreator.createProducer(producerCreatorConfig);
        producerMap.put(producerCreatorConfig, producer);
    }

    public Set<String> getAllTopics(String clusterName) throws ExecutionException, InterruptedException, TimeoutException {
        Admin adminClient = adminMap.get(clusterName);
        ListTopicsResult result = adminClient.listTopics();
//        adminClient.close(Duration.ofSeconds(30));
        return result.names().get(60, TimeUnit.SECONDS);
    }

    public TopicDescription getTopicDesc(String clusterName, String topic) throws ExecutionException, InterruptedException {
        Admin adminClient = adminMap.get(clusterName);
        DescribeTopicsResult result = adminClient.describeTopics(Set.of(topic));
//        adminClient.close(Duration.ofSeconds(30));
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

//    public Consumer<String, String> getConsumer(String clusterName, ConsumerType consumerType) {
//        return consumerMap.get(Tuples.of(clusterName, consumerType));
//    }

    public Consumer<String, String> getConsumer(KafkaCluster cluster) {
        return ConsumerCreator.createConsumer( ConsumerCreator.ConsumerCreatorConfig.builder().cluster(cluster).maxPollRecords(null).build());
    }

    public KafkaProducer getProducer(ProducerCreator.ProducerCreatorConfig producerCreatorConfig) {
        if (producerMap.containsKey(producerCreatorConfig)) {
            return producerMap.get(producerCreatorConfig);
        }
        else {
            KafkaProducer producer = ProducerCreator.createProducer(producerCreatorConfig);
            producerMap.put(producerCreatorConfig, producer);
            return producer;
        }
    }

    public DeleteTopicsResult deleteTopic(String clusterName, String topicName) {
        return adminMap.get(clusterName).deleteTopics(Set.of(topicName));
    }

    public CreateTopicsResult addTopic(String clusterName, String topicName, int partitionCount, short replicationFactor) {
        return addTopic(clusterName, new NewTopic(topicName, partitionCount, replicationFactor));
    }

    public CreateTopicsResult addTopic(String clusterName, NewTopic newTopic) {
        return adminMap.get(clusterName).createTopics(Set.of(newTopic));
    }

    public Tuple2<Long, Long> getPartitionOffsetInfo(String clusterName, TopicPartition topicPartition) throws ExecutionException, InterruptedException {
        Admin adminClient = adminMap.get(clusterName);
        ListOffsetsResult.ListOffsetsResultInfo earliestOffsetsResultInfo = adminClient.listOffsets(Map.of(topicPartition, OffsetSpec.earliest()))
                .partitionResult(topicPartition).get();
        ListOffsetsResult.ListOffsetsResultInfo latestOffsetsResultInfo = adminClient.listOffsets(Map.of(topicPartition, OffsetSpec.latest()))
                .partitionResult(topicPartition).get();
        return Tuples.of(earliestOffsetsResultInfo.offset(), latestOffsetsResultInfo.offset());
    }

    public Map<TopicPartition, Tuple2<Long, Long>> getAllPartitionOffsetInfo(String clusterName, String topicName) throws ExecutionException, InterruptedException, TimeoutException {
        List<TopicPartitionInfo> partitionInfoList = getTopicPartitions(clusterName, topicName);
        return partitionInfoList.stream().collect(Collectors.toMap(p -> new TopicPartition(topicName, p.partition()), p -> {
            try {
                return getPartitionOffsetInfo(clusterName, new TopicPartition(topicName, p.partition()));
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
        Map<TopicPartition, OffsetAndMetadata> map = adminMap.get(clusterName).listConsumerGroupOffsets(consumerGroupId).partitionsToOffsetAndMetadata().get();
        return map.entrySet().stream().map(entry -> {

            try {
                TopicPartition tp = entry.getKey();
                OffsetAndMetadata metadata = entry.getValue();
                Tuple2<Long, Long> startAndEndOffset = getPartitionOffsetInfo(clusterName, tp);
                String leaderEpoch = metadata.leaderEpoch().orElse(0).toString();
                return new ConsumerGroupOffsetTableItem(tp.topic(), tp.partition(), startAndEndOffset.getT1(), startAndEndOffset.getT2(), metadata.offset(), startAndEndOffset.getT2() - metadata.offset(), leaderEpoch);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }).toList();
    }

    public void purgePartition(KafkaPartition kafkaPartition) throws ExecutionException, InterruptedException {
        String clusterName = kafkaPartition.getTopic().getCluster().getName();
        TopicPartition topicPartition = new TopicPartition(kafkaPartition.getTopic().getName(), kafkaPartition.getId());
        long endOffset = getPartitionOffsetInfo(clusterName, topicPartition).getT2();
        Map<TopicPartition, RecordsToDelete> map = Map.of(
                topicPartition,
                RecordsToDelete.beforeOffset(endOffset)
        );
        adminMap.get(clusterName).deleteRecords(map).all().get();
    }

    public void purgeTopic(KafkaTopic kafkaTopic) throws ExecutionException, InterruptedException, TimeoutException {
        String clusterName = kafkaTopic.getCluster().getName();
        Map<TopicPartition, RecordsToDelete> map = getAllPartitionOffsetInfo(clusterName, kafkaTopic.getName()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, (entry) -> RecordsToDelete.beforeOffset(entry.getValue().getT2())));
        adminMap.get(clusterName).deleteRecords(map).all().get();
    }
}

