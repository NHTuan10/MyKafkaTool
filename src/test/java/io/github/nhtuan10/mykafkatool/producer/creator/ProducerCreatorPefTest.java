package io.github.nhtuan10.mykafkatool.producer.creator;

import io.github.nhtuan10.mykafkatool.api.auth.NoAuthProvider;
import io.github.nhtuan10.mykafkatool.manager.AuthProviderManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import javafx.beans.property.SimpleStringProperty;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Map;

class ProducerCreatorPefTest {
    public static void main(String[] args) throws InterruptedException {
        NoAuthProvider noAuthProvider = new NoAuthProvider();
        ProducerCreator.ProducerCreatorConfig config = ProducerCreator.ProducerCreatorConfig.builder()
                .cluster(new KafkaCluster(new SimpleStringProperty("local"), "localhost:9092", "http://localhost:8081", false,
                        noAuthProvider.fromConfigText(""), null))
                .build();
        Producer producer = new ProducerCreator(new AuthProviderManager(Map.of(noAuthProvider.getName(), noAuthProvider))).createProducer(config);
        for (int i = 0; i < 10000; i++) {
            Thread.sleep(50);
            ProducerRecord record = new ProducerRecord("perf", String.valueOf(i) + "-new", "liveupdate-2-test-msg-second-times" + i);
            producer.send(record);
        }
        producer.close();
    }
}