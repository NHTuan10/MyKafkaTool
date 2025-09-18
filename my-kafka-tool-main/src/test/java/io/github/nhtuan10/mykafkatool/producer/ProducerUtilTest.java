package io.github.nhtuan10.mykafkatool.producer;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.github.nhtuan10.mykafkatool.api.Config;
import io.github.nhtuan10.mykafkatool.api.auth.AuthConfig;
import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;
import io.github.nhtuan10.mykafkatool.api.model.KafkaMessage;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
import io.github.nhtuan10.mykafkatool.producer.creator.ProducerCreator;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProducerUtilTest {

    @Mock
    private SerDesHelper serDesHelper;

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private KafkaProducer<String, Object> producer;

    @Mock
    private Future<RecordMetadata> sendFuture;

    private ProducerUtil producerUtil;

    @BeforeEach
    void setUp() {
        producerUtil = new ProducerUtil(serDesHelper, clusterManager);
    }

    @Test
    void testSendMessage_WithPartition_Success() throws Exception {
        // Arrange
        KafkaCluster cluster = createTestCluster();
        KafkaTopic topic = createTestTopic(cluster, "test-topic");
        KafkaPartition partition = createTestPartition(topic, 1);
        KafkaMessage message = createTestMessage();

        ProducerCreator.ProducerCreatorConfig expectedConfig = ProducerCreator.ProducerCreatorConfig.builder()
                .cluster(cluster)
                .keySerializer(StringSerializer.class)
                .valueSerializer(StringSerializer.class)
                .build();
        doReturn(StringSerializer.class).when(serDesHelper).getSerializeClass("String");
        when(clusterManager.getProducer(any(ProducerCreator.ProducerCreatorConfig.class))).thenReturn(producer);
        when(serDesHelper.convertStringToObjectBeforeSerialize(anyString(), anyInt(), any(KafkaMessage.class), anyMap()))
                .thenReturn("processed-value");

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test-topic", 1), 0, 0, 0L, 0L, 0, 0);
        when(sendFuture.get()).thenReturn(metadata);
        when(producer.send(any(ProducerRecord.class))).thenReturn(sendFuture);

        // Act
        producerUtil.sendMessage(topic, partition, message);

        // Assert
        verify(producer).flush();
        verify(producer).send(any(ProducerRecord.class));
        verify(serDesHelper, times(2)).getSerializeClass("String");
        verify(serDesHelper, times(2)).getSerializeClass(anyString());
        verify(clusterManager).getProducer(any(ProducerCreator.ProducerCreatorConfig.class));
        verify(serDesHelper).convertStringToObjectBeforeSerialize(eq("test-topic"), eq(1), eq(message), anyMap());
    }

    @Test
    void testSendMessage_WithoutPartition_Success() throws Exception {
        // Arrange
        KafkaCluster cluster = createTestCluster();
        KafkaTopic topic = createTestTopic(cluster, "test-topic");
        KafkaMessage message = createTestMessage();

        doReturn(StringSerializer.class).when(serDesHelper).getSerializeClass("String");
        when(clusterManager.getProducer(any(ProducerCreator.ProducerCreatorConfig.class))).thenReturn(producer);
        when(serDesHelper.convertStringToObjectBeforeSerialize(anyString(), isNull(), any(KafkaMessage.class), anyMap()))
                .thenReturn("processed-value");

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test-topic", 0), 0, 0, 0L, 0L, 0, 0);
        when(sendFuture.get()).thenReturn(metadata);
        when(producer.send(any(ProducerRecord.class))).thenReturn(sendFuture);

        // Act
        producerUtil.sendMessage(topic, null, message);

        // Assert
        verify(producer).flush();
        verify(producer).send(any(ProducerRecord.class));
        verify(serDesHelper).convertStringToObjectBeforeSerialize(eq("test-topic"), isNull(), eq(message), anyMap());
    }

    @Test
    void testSendMessage_WithNullTopic_ThrowsException() {
        // Arrange
        KafkaMessage message = createTestMessage();

        // Act & Assert
        assertThrows(Exception.class, () -> {
            producerUtil.sendMessage(null, null, message);
        });
    }

    @Test
    void testSendMessage_ProducerThrowsException() throws Exception {
        // Arrange
        KafkaCluster cluster = createTestCluster();
        KafkaTopic topic = createTestTopic(cluster, "test-topic");
        KafkaMessage message = createTestMessage();

        doReturn(StringSerializer.class).when(serDesHelper).getSerializeClass("String");
        when(clusterManager.getProducer(any(ProducerCreator.ProducerCreatorConfig.class))).thenReturn(producer);
        when(serDesHelper.convertStringToObjectBeforeSerialize(anyString(), isNull(), any(KafkaMessage.class), anyMap()))
                .thenReturn("processed-value");

        ExecutionException executionException = new ExecutionException("Producer error", new RuntimeException("Kafka error"));
        when(sendFuture.get()).thenThrow(executionException);
        when(producer.send(any(ProducerRecord.class))).thenReturn(sendFuture);

        // Act & Assert
        assertThrows(ExecutionException.class, () -> {
            producerUtil.sendMessage(topic, null, message);
        });

        verify(producer).flush();
        verify(producer).send(any(ProducerRecord.class));
    }

    @Test
    void testSendMessage_SerDesHelperThrowsException() throws Exception {
        // Arrange
        KafkaCluster cluster = createTestCluster();
        KafkaTopic topic = createTestTopic(cluster, "test-topic");
        KafkaMessage message = createTestMessage();

        doReturn(StringSerializer.class).when(serDesHelper).getSerializeClass("String");
        when(clusterManager.getProducer(any(ProducerCreator.ProducerCreatorConfig.class))).thenReturn(producer);
        when(serDesHelper.convertStringToObjectBeforeSerialize(anyString(), isNull(), any(KafkaMessage.class), anyMap()))
                .thenThrow(new RuntimeException("SerDes error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            producerUtil.sendMessage(topic, null, message);
        });

        verify(producer).flush();
        verify(serDesHelper).convertStringToObjectBeforeSerialize(eq("test-topic"), isNull(), eq(message), anyMap());
    }

    @Test
    void testSendMessage_VerifyProducerConfig() throws Exception {
        // Arrange
        KafkaCluster cluster = createTestCluster();
        KafkaTopic topic = createTestTopic(cluster, "test-topic");
        KafkaMessage message = createTestMessage();

        doReturn(StringSerializer.class).when(serDesHelper).getSerializeClass("String");
        when(clusterManager.getProducer(any(ProducerCreator.ProducerCreatorConfig.class))).thenReturn(producer);
        when(serDesHelper.convertStringToObjectBeforeSerialize(anyString(), isNull(), any(KafkaMessage.class), anyMap()))
                .thenReturn("processed-value");

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test-topic", 0), 0, 0, 0L, 0L, 0, 0);
        when(sendFuture.get()).thenReturn(metadata);
        when(producer.send(any(ProducerRecord.class))).thenReturn(sendFuture);

        // Act
        producerUtil.sendMessage(topic, null, message);

        // Assert
        ArgumentCaptor<ProducerCreator.ProducerCreatorConfig> configCaptor = ArgumentCaptor.forClass(ProducerCreator.ProducerCreatorConfig.class);
        verify(clusterManager).getProducer(configCaptor.capture());

        ProducerCreator.ProducerCreatorConfig capturedConfig = configCaptor.getValue();
        assertEquals(cluster, capturedConfig.getCluster());
        assertEquals(StringSerializer.class, capturedConfig.getKeySerializer());
        assertEquals(StringSerializer.class, capturedConfig.getValueSerializer());
    }

    @Test
    void testSendMessage_VerifyProducerRecord() throws Exception {
        // Arrange
        KafkaCluster cluster = createTestCluster();
        KafkaTopic topic = createTestTopic(cluster, "test-topic");
        KafkaPartition partition = createTestPartition(topic, 2);
        KafkaMessage message = createTestMessageWithHeaders();

        doReturn(StringSerializer.class).when(serDesHelper).getSerializeClass("String");
        when(clusterManager.getProducer(any(ProducerCreator.ProducerCreatorConfig.class))).thenReturn(producer);
        when(serDesHelper.convertStringToObjectBeforeSerialize(anyString(), anyInt(), any(KafkaMessage.class), anyMap()))
                .thenReturn("processed-value");

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test-topic", 2), 0, 0, 0L, 0L, 0, 0);
        when(sendFuture.get()).thenReturn(metadata);
        when(producer.send(any(ProducerRecord.class))).thenReturn(sendFuture);

        // Act
        producerUtil.sendMessage(topic, partition, message);

        // Assert
        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(recordCaptor.capture());

        ProducerRecord<String, Object> capturedRecord = recordCaptor.getValue();
        assertEquals("test-topic", capturedRecord.topic());
        assertEquals(Integer.valueOf(2), capturedRecord.partition());
        assertEquals("test-key", capturedRecord.key());
        assertEquals("processed-value", capturedRecord.value());
        assertNotNull(capturedRecord.headers());
        assertEquals(2, capturedRecord.headers().toArray().length);
    }

    @Test
    void testSendMessage_WithBlankKey() throws Exception {
        // Arrange
        KafkaCluster cluster = createTestCluster();
        KafkaTopic topic = createTestTopic(cluster, "test-topic");
        KafkaMessage message = createTestMessageWithBlankKey();
        doReturn(StringSerializer.class).when(serDesHelper).getSerializeClass("String");
        when(clusterManager.getProducer(any(ProducerCreator.ProducerCreatorConfig.class))).thenReturn(producer);
        when(serDesHelper.convertStringToObjectBeforeSerialize(anyString(), isNull(), any(KafkaMessage.class), anyMap()))
                .thenReturn("processed-value");

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test-topic", 0), 0, 0, 0L, 0L, 0, 0);
        when(sendFuture.get()).thenReturn(metadata);
        when(producer.send(any(ProducerRecord.class))).thenReturn(sendFuture);

        // Act
        producerUtil.sendMessage(topic, null, message);

        // Assert
        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(recordCaptor.capture());

        ProducerRecord<String, Object> capturedRecord = recordCaptor.getValue();
        assertNull(capturedRecord.key()); // Should be null for blank keys
    }

    @Test
    void testSendMessage_WithDifferentContentTypes() throws Exception {
        // Arrange
        KafkaCluster cluster = createTestCluster();
        KafkaTopic topic = createTestTopic(cluster, "test-topic");
        KafkaMessage message = createTestMessageWithDifferentContentTypes();
        doReturn(StringSerializer.class).when(serDesHelper).getSerializeClass("String");
        doReturn(KafkaAvroSerializer.class).when(serDesHelper).getSerializeClass("Schema Registry Avro");
        when(clusterManager.getProducer(any(ProducerCreator.ProducerCreatorConfig.class))).thenReturn(producer);
        when(serDesHelper.convertStringToObjectBeforeSerialize(anyString(), isNull(), any(KafkaMessage.class), anyMap()))
                .thenReturn("processed-value");

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test-topic", 0), 0, 0, 0L, 0L, 0, 0);
        when(sendFuture.get()).thenReturn(metadata);
        when(producer.send(any(ProducerRecord.class))).thenReturn(sendFuture);

        // Act
        producerUtil.sendMessage(topic, null, message);

        // Assert
        ArgumentCaptor<ProducerCreator.ProducerCreatorConfig> configCaptor = ArgumentCaptor.forClass(ProducerCreator.ProducerCreatorConfig.class);
        verify(clusterManager).getProducer(configCaptor.capture());

        ProducerCreator.ProducerCreatorConfig capturedConfig = configCaptor.getValue();
        assertEquals(StringSerializer.class, capturedConfig.getKeySerializer());
        assertEquals(KafkaAvroSerializer.class, capturedConfig.getValueSerializer());
    }

    @Test
    void testSendMessage_VerifySerDesHelperParameters() throws Exception {
        // Arrange
        KafkaCluster cluster = createTestCluster();
        KafkaTopic topic = createTestTopic(cluster, "test-topic");
        KafkaPartition partition = createTestPartition(topic, 3);
        KafkaMessage message = createTestMessage();

        doReturn(StringSerializer.class).when(serDesHelper).getSerializeClass("String");
        when(clusterManager.getProducer(any(ProducerCreator.ProducerCreatorConfig.class))).thenReturn(producer);
        when(serDesHelper.convertStringToObjectBeforeSerialize(anyString(), anyInt(), any(KafkaMessage.class), anyMap()))
                .thenReturn("processed-value");

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test-topic", 3), 0, 0, 0L, 0L, 0, 0);
        when(sendFuture.get()).thenReturn(metadata);
        when(producer.send(any(ProducerRecord.class))).thenReturn(sendFuture);

        // Act
        producerUtil.sendMessage(topic, partition, message);

        // Assert
        ArgumentCaptor<Map<String, Object>> othersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(serDesHelper).convertStringToObjectBeforeSerialize(eq("test-topic"), eq(3), eq(message), othersCaptor.capture());

        Map<String, Object> capturedOthers = othersCaptor.getValue();
        assertEquals(false, capturedOthers.get(Config.IS_KEY_PROP));
        assertEquals(cluster.getAuthConfig(), capturedOthers.get(Config.AUTH_CONFIG_PROP));
    }

    @Test
    void testSendMessage_WithEmptyHeaders() throws Exception {
        // Arrange
        KafkaCluster cluster = createTestCluster();
        KafkaTopic topic = createTestTopic(cluster, "test-topic");
        KafkaMessage message = createTestMessageWithoutHeaders();

        doReturn(StringSerializer.class).when(serDesHelper).getSerializeClass("String");
        doReturn(KafkaAvroSerializer.class).when(serDesHelper).getSerializeClass("Schema Registry Avro");
        when(clusterManager.getProducer(any(ProducerCreator.ProducerCreatorConfig.class))).thenReturn(producer);
        when(serDesHelper.convertStringToObjectBeforeSerialize(anyString(), isNull(), any(KafkaMessage.class), anyMap()))
                .thenReturn("processed-value");

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test-topic", 0), 0, 0, 0L, 0L, 0, 0);
        when(sendFuture.get()).thenReturn(metadata);
        when(producer.send(any(ProducerRecord.class))).thenReturn(sendFuture);

        // Act
        producerUtil.sendMessage(topic, null, message);

        // Assert
        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(recordCaptor.capture());

        ProducerRecord<String, Object> capturedRecord = recordCaptor.getValue();
        assertNotNull(capturedRecord.headers());
        assertEquals(0, capturedRecord.headers().toArray().length);
    }

    @Test
    void testSendMessage_ClusterManagerThrowsException() throws Exception {
        // Arrange
        KafkaCluster cluster = createTestCluster();
        KafkaTopic topic = createTestTopic(cluster, "test-topic");
        KafkaMessage message = createTestMessage();

        doReturn(StringSerializer.class).when(serDesHelper).getSerializeClass("String");
        when(clusterManager.getProducer(any(ProducerCreator.ProducerCreatorConfig.class)))
                .thenThrow(new RuntimeException("Failed to get producer"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            producerUtil.sendMessage(topic, null, message);
        });

        verify(clusterManager).getProducer(any(ProducerCreator.ProducerCreatorConfig.class));
    }

    // Helper methods to create test objects
    private KafkaCluster createTestCluster() {
        KafkaCluster cluster = new KafkaCluster("test-cluster", "localhost:9092");

        AuthConfig authConfig = new AuthConfig("test-auth", Map.of(), Map.of());
        cluster.setAuthConfig(authConfig);

        return cluster;
    }

    private KafkaTopic createTestTopic(KafkaCluster cluster, String topicName) {
        return new KafkaTopic(topicName, cluster, List.of());
    }

    private KafkaPartition createTestPartition(KafkaTopic topic, int partitionId) {
        return new KafkaPartition(partitionId, topic);
    }

    private KafkaMessage createTestMessage() {
        return new KafkaMessage("test-key", "test-value"
                , "String"
                , "String"
                , new RecordHeaders());

    }

    private KafkaMessage createTestMessageWithHeaders() {
        return new KafkaMessage("test-key", "test-value"
                , "String"
                , "String"
                , new RecordHeaders().add("header1", "value1".getBytes()).add("header2", "value2".getBytes()
        ));
    }

    private KafkaMessage createTestMessageWithBlankKey() {
        return new KafkaMessage("", "test-value"
                , "String"
                , "String"
                , new RecordHeaders().add("header1", "value1".getBytes()).add("header2", "value2".getBytes())
        );
    }

    private KafkaMessage createTestMessageWithDifferentContentTypes() {
        return new KafkaMessage("test-key", "String"
                , "test-value"
                , "Schema Registry Avro"
                , "String"
                , new RecordHeaders().add("header1", "value1".getBytes()).add("header2", "value2".getBytes())
        );
    }

    private KafkaMessage createTestMessageWithoutHeaders() {
        return new KafkaMessage("test-key"
                , "String"
                , "Schema Registry Avro"
                , "String"
                , new RecordHeaders());
    }
}