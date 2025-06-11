package io.github.nhtuan10.mykafkatool.producer;

import io.github.nhtuan10.mykafkatool.api.Config;
import io.github.nhtuan10.mykafkatool.api.model.KafkaMessage;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.producer.creator.ProducerCreator;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@AppScoped
public class ProducerUtil {
    private final SerDesHelper serDesHelper;
    private final ClusterManager clusterManager;

    @Inject
    public ProducerUtil(SerDesHelper serDesHelper, ClusterManager clusterManager) {
        this.serDesHelper = serDesHelper;
        this.clusterManager = clusterManager;
    }

    public void sendMessage(@NonNull KafkaTopic kafkaTopic, KafkaPartition partition, KafkaMessage kafkaMessage)
            throws Exception {

        KafkaCluster cluster = kafkaTopic.cluster();

        ProducerCreator.ProducerCreatorConfig producerConfig = createProducerConfig(cluster, kafkaMessage);
        KafkaProducer producer = clusterManager.getProducer(producerConfig);
        producer.flush();

        ProducerRecord<String, Object> producerRecord = createProducerRecord(kafkaTopic, partition, kafkaMessage);

        RecordMetadata metadata = (RecordMetadata) producer.send(producerRecord).get();
        log.info("Record sent with key '{}' to partition {} with offset {}",
                kafkaMessage.key(), metadata.partition(), metadata.offset());
    }

    private ProducerCreator.ProducerCreatorConfig createProducerConfig(KafkaCluster cluster, KafkaMessage kafkaMessage) {
        return ProducerCreator.ProducerCreatorConfig.builder()
                .cluster(cluster)
                .keySerializer(serDesHelper.getSerializeClass(kafkaMessage.keyContentType()))
                .valueSerializer(serDesHelper.getSerializeClass(kafkaMessage.valueContentType()))
                .build();
    }

    private ProducerRecord<String, Object> createProducerRecord(@NonNull KafkaTopic kafkaTopic, KafkaPartition partition,
                                                                KafkaMessage kafkaMessage) throws Exception {
        Integer partitionId = partition != null ? partition.id() : null;
        String key = StringUtils.isBlank(kafkaMessage.key()) ? null : kafkaMessage.key();
        Map<String, Object> others = Map.of(Config.IS_KEY_PROP, false, Config.AUTH_CONFIG_PROP, kafkaTopic.cluster().getAuthConfig());
        Object value = serDesHelper.convertStringToObjectBeforeSerialize(kafkaTopic.name(), partitionId, kafkaMessage, others);

        List<Header> headers = kafkaMessage.headers().entrySet().stream().map(entry -> new RecordHeader(entry.getKey(), entry.getValue())).collect(Collectors.toList());

        return new ProducerRecord<>(kafkaTopic.name(), partitionId, key, value, headers);
    }
}
