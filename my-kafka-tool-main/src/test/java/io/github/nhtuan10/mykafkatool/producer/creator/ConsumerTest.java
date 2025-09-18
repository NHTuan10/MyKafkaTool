package io.github.nhtuan10.mykafkatool.producer.creator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.api.auth.AuthProvider;
import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;
import io.github.nhtuan10.mykafkatool.auth.SaslPlaintextProvider;
import io.github.nhtuan10.mykafkatool.consumer.creator.ConsumerCreator;
import io.github.nhtuan10.mykafkatool.manager.AuthProviderManager;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
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

    public static void main(String[] args) throws Exception {
        AuthProvider noAuthProvider = new SaslPlaintextProvider(new ObjectMapper());
        AuthProviderManager authProviderManager = new AuthProviderManager(Map.of(noAuthProvider.getName(), noAuthProvider));
        KafkaCluster cluster = new KafkaCluster(new SimpleStringProperty("local"), "localhost:9092", "http://localhost:8081", false,
                noAuthProvider.fromConfigText("    { \"sasl.mechanism\" : \"PLAIN\", \"sasl.jaas.config\" : \"org.apache.kafka.common.security.plain.PlainLoginModule required username=\\\"bob\\\" password=\\\"bob\\\";\"  }"), null);
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
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "CG2");
        Consumer<String, Object> consumer2 = clusterManager.createConsumer(consumerProps);
        consumer.subscribe(List.of("test", "test-2"));
        consumer2.subscribe(List.of("test", "test-2"));
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
