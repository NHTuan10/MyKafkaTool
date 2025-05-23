package io.github.nhtuan10.mykafkatool.consumer.creator;

import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.HashMap;
import java.util.Map;

import static io.github.nhtuan10.mykafkatool.constant.AppConstant.DEFAULT_MAX_POLL_RECORDS;
import static io.github.nhtuan10.mykafkatool.constant.AppConstant.OFFSET_RESET_EARLIER;

public class ConsumerCreator {
    public static Consumer createConsumer(ConsumerCreatorConfig consumerCreatorConfig) {
        return createConsumer(buildConsumerConfigs(consumerCreatorConfig));
    }

    public static Consumer createConsumer(Map<String, Object> properties) {
        return new KafkaConsumer<>(properties);
    }

    public static Map<String, Object> buildConsumerConfigs(ConsumerCreatorConfig consumerCreatorConfig) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerCreatorConfig.cluster.getBootstrapServer());
//        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerCreatorConfig.groupId + UUID.randomUUID());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerCreatorConfig.groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, consumerCreatorConfig.keyDeserializer);
//        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,  consumerCreatorConfig.valueDeserializer);
        String schemaRegistryUrl = consumerCreatorConfig.cluster.getSchemaRegistryUrl();
        if (StringUtils.isNotBlank(schemaRegistryUrl)) {
            properties.put("schema.registry.url", schemaRegistryUrl);
        }
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerCreatorConfig.maxPollRecords != null ? consumerCreatorConfig.maxPollRecords : DEFAULT_MAX_POLL_RECORDS);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_RESET_EARLIER);
        return properties;
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
        private String groupId = "MyTool-UI-CG";

        public static ConsumerCreatorConfigBuilder builder(KafkaCluster cluster) {
            return new ConsumerCreatorConfigBuilder().cluster(cluster);
        }
    }
}
