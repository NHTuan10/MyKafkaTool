package com.example.mytool.producer;

import com.example.mytool.manager.ClusterManager;
import com.example.mytool.model.kafka.KafkaCluster;
import com.example.mytool.model.kafka.KafkaPartition;
import com.example.mytool.model.kafka.KafkaTopic;
import com.example.mytool.producer.creator.ProducerCreator;
import com.example.mytool.serde.SerdeUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.ExecutionException;

@Slf4j
public class ProducerUtil {
    public static void sendMessage(@NonNull KafkaTopic kafkaTopic, KafkaPartition partition, Message message) throws ExecutionException, InterruptedException {
        KafkaCluster cluster = partition != null ? partition.getTopic().getCluster() : kafkaTopic.getCluster();
        ProducerCreator.ProducerCreatorConfig producerConfig = ProducerCreator.ProducerCreatorConfig.builder()
                .cluster(cluster)
                .keySerializer(SerdeUtil.getSerializeClass(message.keyContentType()))
                .valueSerializer(SerdeUtil.getSerializeClass(message.valueContentType()))
                .build();
        KafkaProducer producer = ClusterManager.getInstance().getProducer(producerConfig);
        producer.flush();
        String key = StringUtils.isBlank(message.key()) ? null : message.key();
        Object value = SerdeUtil.convert(message.valueContentType(), message.value(), message.schema());
        ProducerRecord record;
        if (partition != null) {
            record = new ProducerRecord<>(partition.getTopic().getName(), partition.getId(), key, value);
        } else {
            record = new ProducerRecord<>(kafkaTopic.getName(), key, value);
        }

        RecordMetadata metadata = (RecordMetadata) producer.send(record).get();
        log.info("record sent with key " + message.key() + " to partition " + metadata.partition()
                + " with offset " + metadata.offset());

    }
}
