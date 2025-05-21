package com.example.mytool.producer.creator;

import com.example.mytool.model.kafka.KafkaCluster;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

class ProducerCreatorPefTest {
    public static void main(String[] args) {
        ProducerCreator.ProducerCreatorConfig config = ProducerCreator.ProducerCreatorConfig.builder()
                .cluster(new KafkaCluster("local", "localhost:9092", "http://localhost:8081", false))
                .build();
        Producer producer = ProducerCreator.createProducer(config);
        for (int i = 0; i < 100_000; i++) {
            ProducerRecord record = new ProducerRecord("perf", "test-msg-" + i);
            producer.send(record);
        }
    }
}