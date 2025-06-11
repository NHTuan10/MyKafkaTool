package io.github.nhtuan10.mykafkatool.producer.creator;

import io.github.nhtuan10.mykafkatool.api.auth.AuthConfig;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.manager.AuthProviderManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import jakarta.inject.Inject;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

@AppScoped
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProducerCreator {
    private final AuthProviderManager authProviderManager;

    @SneakyThrows
    public KafkaProducer createProducer(ProducerCreatorConfig producerCreatorConfig) {
        Properties properties = new Properties();
        KafkaCluster cluster = producerCreatorConfig.getCluster();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServer());
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, AppConstant.APP_NAME);
        String schemaRegistryUrl = cluster.getSchemaRegistryUrl();
        if (StringUtils.isNotBlank(schemaRegistryUrl)) {
            properties.put("schema.registry.url", schemaRegistryUrl);
        }

        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producerCreatorConfig.getKeySerializer());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerCreatorConfig.getValueSerializer());
        AuthConfig authConfig = cluster.getAuthConfig();
        properties.putAll(authProviderManager.getKafkaAuthProperties(authConfig));
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
