package com.example.mytool.consumer.creator;

import com.example.mytool.model.kafka.KafkaCluster;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Properties;
import java.util.UUID;

import static com.example.mytool.constant.AppConstant.DEFAULT_MAX_POLL_RECORDS;
import static com.example.mytool.constant.AppConstant.OFFSET_RESET_EARLIER;

public class ConsumerCreator {
    //    public static Consumer<Long,String> createConsumer(String consumerGroup){
    public static Consumer createConsumer(ConsumerCreatorConfig consumerCreatorConfig) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerCreatorConfig.cluster.getBootstrapServer());
//        properties.put(ConsumerConfig.GROUP_ID_CONFIG,"MyTool"+ UUID.randomUUID());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerCreatorConfig.groupId + UUID.randomUUID());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, consumerCreatorConfig.keyDeserializer);
//        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,  consumerCreatorConfig.valueDeserializer);
        properties.put("schema.registry.url", "http://localhost:8081");
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerCreatorConfig.maxPollRecords != null ? consumerCreatorConfig.maxPollRecords : DEFAULT_MAX_POLL_RECORDS);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_RESET_EARLIER);

//        Consumer<Long,String> consumer = new KafkaConsumer<Long, String>(properties);
        Consumer consumer = new KafkaConsumer<>(properties);
//        consumer.subscribe(Collections.singletonList(topic));
        return consumer;
    }

    @Builder(builderMethodName = "")
    @EqualsAndHashCode
    public static final class ConsumerCreatorConfig {

        private final KafkaCluster cluster;
        private final Integer maxPollRecords;
        @Builder.Default
        private Class<? extends Deserializer> keyDeserializer = StringDeserializer.class;
        @Builder.Default
        private Class<? extends Deserializer> valueDeserializer = StringDeserializer.class;
        @Builder.Default
        private String groupId=  "MyTool";

        public static ConsumerCreatorConfigBuilder builder(KafkaCluster cluster) {
            return new ConsumerCreatorConfigBuilder().cluster(cluster);
        }
    }
}
