package com.example.mytool.manager;

import com.example.mytool.model.kafka.KafkaCluster;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class ProducerCreator {
    public static KafkaProducer createProducer(ProducerCreatorConfig producerCreatorConfig) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerCreatorConfig.cluster.getBootstrapServer());
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "MyTool");
//        properties.put("schema.registry.url", "http://localhost:8081");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producerCreatorConfig.keySerializer);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerCreatorConfig.valueSerializer);
        return new KafkaProducer<>(properties);
    }

    @Builder
    @EqualsAndHashCode
    public static final class ProducerCreatorConfig {
        private final KafkaCluster cluster;
        @Builder.Default
        private String keySerializer = StringSerializer.class.getName();
        @Builder.Default
        private String valueSerializer = StringSerializer.class.getName();
    }
}
