package io.github.nhtuan10.mykafkatool.producer.creator;

import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

class ProducerCreatorPefTest {
    public static void main(String[] args) throws InterruptedException {
        ProducerCreator.ProducerCreatorConfig config = ProducerCreator.ProducerCreatorConfig.builder()
                .cluster(new KafkaCluster("local", "localhost:9092", "http://localhost:8081", false, null))
                .build();
        Producer producer = ProducerCreator.createProducer(config);
        for (int i = 0; i < 10000; i++) {
            Thread.sleep(50);
            ProducerRecord record = new ProducerRecord("perf", String.valueOf(i) + "-new", "liveupdate-2-test-msg-second-times" + i);
            producer.send(record);
        }
        producer.close();
    }
}