package com.example.mytool.manager;

import com.example.mytool.model.kafka.KafkaCluster;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Properties;

import static com.example.mytool.constant.AppConstant.DEFAULT_MAX_POLL_RECORDS;
import static com.example.mytool.constant.AppConstant.OFFSET_RESET_EARLIER;

public class ConsumerCreator {
    //    public static Consumer<Long,String> createConsumer(String consumerGroup){
    public static Consumer<String, String> createConsumer(KafkaCluster cluster, Integer maxPollRecords) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServer());
//        properties.put(ConsumerConfig.GROUP_ID_CONFIG,"MyTool"+ UUID.randomUUID());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "MyTool");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords != null ? maxPollRecords : DEFAULT_MAX_POLL_RECORDS);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_RESET_EARLIER);

//        Consumer<Long,String> consumer = new KafkaConsumer<Long, String>(properties);
        Consumer<String, String> consumer = new KafkaConsumer<>(properties);
//        consumer.subscribe(Collections.singletonList(topic));
        return consumer;
    }
}
