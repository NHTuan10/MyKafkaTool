package com.example.mytool.producer;

import com.example.mytool.api.KafkaMessage;
import com.example.mytool.manager.ClusterManager;
import com.example.mytool.model.kafka.KafkaCluster;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.producer.creator.ProducerCreator;
import com.example.mytool.serdes.SerdeUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ProducerUtil {
    private final SerdeUtil serdeUtil;

    public void sendMessage(@NonNull KafkaTopic kafkaTopic, KafkaPartition partition, KafkaMessage kafkaMessage)
            throws ExecutionException, InterruptedException, IOException {

        KafkaCluster cluster = kafkaTopic.getCluster();

        ProducerCreator.ProducerCreatorConfig producerConfig = createProducerConfig(cluster, kafkaMessage);
        KafkaProducer producer = ClusterManager.getInstance().getProducer(producerConfig);
        producer.flush();

        ProducerRecord<String, Object> producerRecord = createProducerRecord(kafkaTopic, partition, kafkaMessage);

        RecordMetadata metadata = (RecordMetadata) producer.send(producerRecord).get();
        log.info("Record sent with key '{}' to partition {} with offset {}",
                kafkaMessage.key(), metadata.partition(), metadata.offset());
    }

    private ProducerCreator.ProducerCreatorConfig createProducerConfig(KafkaCluster cluster, KafkaMessage kafkaMessage) {
        return ProducerCreator.ProducerCreatorConfig.builder()
                .cluster(cluster)
                .keySerializer(serdeUtil.getSerializeClass(kafkaMessage.keyContentType()))
                .valueSerializer(serdeUtil.getSerializeClass(kafkaMessage.valueContentType()))
                .build();
    }

    private ProducerRecord<String, Object> createProducerRecord(@NonNull KafkaTopic kafkaTopic, KafkaPartition partition,
                                                                       KafkaMessage kafkaMessage) throws IOException {

        String key = StringUtils.isBlank(kafkaMessage.key()) ? null : kafkaMessage.key();
        Object value = serdeUtil.convert(kafkaMessage.valueContentType(), kafkaMessage.value(), kafkaMessage.schema());
//        ObjectMapper objectMapper = new ObjectMapper();
//        Object value = objectMapper.readValue(kafkaMessage.value(), Object.class);
        List<Header> headers = kafkaMessage.headers().entrySet().stream().map(entry -> new RecordHeader(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8))).collect(Collectors.toList());

//        if (partition != null) {
        return new ProducerRecord<>(kafkaTopic.getName(), partition != null ? partition.getId() : null, key, value, headers);
//        } else {
//            return new ProducerRecord<>(kafkaTopic.getName(), key, value);
//        }
    }
}
