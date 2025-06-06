package io.github.nhtuan10.mykafkatool.producer.creator;

import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class ProducerCreator {
    public static KafkaProducer createProducer(ProducerCreatorConfig producerCreatorConfig) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerCreatorConfig.getCluster().getBootstrapServer());
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, AppConstant.APP_NAME);
        String schemaRegistryUrl = producerCreatorConfig.getCluster().getSchemaRegistryUrl();
        if (StringUtils.isNotBlank(schemaRegistryUrl)) {
            properties.put("schema.registry.url", schemaRegistryUrl);
        }

        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producerCreatorConfig.getKeySerializer());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerCreatorConfig.getValueSerializer());
        properties.putAll(producerCreatorConfig.getCluster().getAuthConfig().properties());
        return new KafkaProducer<>(properties);
    }

    @Builder
    @EqualsAndHashCode
    @Getter
    public static final class ProducerCreatorConfig {

        private final KafkaCluster cluster;
        @Builder.Default
        @Getter
        private Class<? extends Serializer> keySerializer = StringSerializer.class;

        @Builder.Default
        @Getter
        private Class<? extends Serializer> valueSerializer = StringSerializer.class;

        public String getClusterName() {
            return cluster.getName();
        }
    }
}
