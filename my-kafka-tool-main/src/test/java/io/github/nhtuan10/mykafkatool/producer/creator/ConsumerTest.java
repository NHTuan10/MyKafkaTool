package io.github.nhtuan10.mykafkatool.producer.creator;

import io.github.nhtuan10.mykafkatool.api.auth.NoAuthProvider;
import io.github.nhtuan10.mykafkatool.consumer.creator.ConsumerCreator;
import io.github.nhtuan10.mykafkatool.manager.AuthProviderManager;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import javafx.beans.property.SimpleStringProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static io.github.nhtuan10.mykafkatool.constant.AppConstant.OFFSET_RESET_LATEST;

@Slf4j
public class ConsumerTest {
    public static void main(String[] args) {
        NoAuthProvider noAuthProvider = new NoAuthProvider();
        AuthProviderManager authProviderManager = new AuthProviderManager(Map.of(noAuthProvider.getName(), noAuthProvider));
        KafkaCluster cluster = new KafkaCluster(new SimpleStringProperty("local"), "localhost:9092", "http://localhost:8081", false,
                noAuthProvider.fromConfigText(""), null);
        ConsumerCreator.ConsumerCreatorConfig consumerCreatorConfig = ConsumerCreator.ConsumerCreatorConfig.builder(cluster)
                .keyDeserializer(StringDeserializer.class)
                .valueDeserializer(StringDeserializer.class)
                .groupId("MyKafkaTool-Test-CG")
                .build();
        ConsumerCreator consumerCreator = new ConsumerCreator(authProviderManager);
        Map<String, Object> consumerProps = new ConsumerCreator(authProviderManager).buildConsumerConfigs(consumerCreatorConfig);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_RESET_LATEST);
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        ClusterManager clusterManager = new ClusterManager(authProviderManager,
                new ProducerCreator(authProviderManager), consumerCreator);
        Consumer<String, Object> consumer = clusterManager.createConsumer(consumerProps);
        Consumer<String, Object> consumer2 = clusterManager.createConsumer(consumerProps);
        consumer.subscribe(List.of("perf", "test2"));
        consumer2.subscribe(List.of("perf", "test2"));
        //            consumers.clear();
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        while (true) {
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(5));
            for (ConsumerRecord<String, Object> record : records) {
                log.info("Record, key: {}, value: {}", record.key(), record.value());
            }
            consumer2.poll(Duration.ofSeconds(5));
        }
    }
}
