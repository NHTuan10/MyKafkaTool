package io.github.nhtuan10.mykafkatool.dagger;

import com.google.common.collect.ImmutableMap;
import dagger.Module;
import dagger.Provides;
import io.github.nhtuan10.mykafkatool.consumer.KafkaConsumerService;
import io.github.nhtuan10.mykafkatool.producer.ProducerUtil;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import io.github.nhtuan10.mykafkatool.serdes.deserializer.ByteArrayDeserializer;
import io.github.nhtuan10.mykafkatool.serdes.deserializer.SchemaRegistryAvroDeserializer;
import io.github.nhtuan10.mykafkatool.serdes.deserializer.StringDeserializer;
import io.github.nhtuan10.mykafkatool.serdes.serializer.ByteArraySerializer;
import io.github.nhtuan10.mykafkatool.serdes.serializer.SchemaRegistryAvroSerializer;
import io.github.nhtuan10.mykafkatool.serdes.serializer.StringSerializer;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.event.EventDispatcher;

import java.util.concurrent.SubmissionPublisher;

@Module
public abstract class AppModule {
//    @Provides
//    ClusterManager clusterManager() {
//        return ClusterManager.getInstance();
//    }

    @AppScoped
    @Provides
    static EventDispatcher eventDispatcher() {
        return new EventDispatcher(new SubmissionPublisher<>()
                , new SubmissionPublisher<>(), new SubmissionPublisher<>());
    }

//    @Provides
//    JsonHighlighter jsonHighlighter() {
//        return new JsonHighlighter();
//    }

//    @Provides
//    ObjectProperty<KafkaCluster> kafkaClusterObjectProperty() {
//        return new SimpleObjectProperty<>();
//    }


    @AppScoped
    @Provides
    static SerDesHelper serDesHelper() {
        StringSerializer stringSerializer = new StringSerializer();
        StringDeserializer stringDeserializer = new StringDeserializer();
        ByteArraySerializer byteArraySerializer = new ByteArraySerializer();
        ByteArrayDeserializer byteArrayDeserializer = new ByteArrayDeserializer();
        SchemaRegistryAvroSerializer schemaRegistryAvroSerializer = new SchemaRegistryAvroSerializer();
        SchemaRegistryAvroDeserializer schemaRegistryAvroDeserializer = new SchemaRegistryAvroDeserializer();
        return new SerDesHelper(
                ImmutableMap.of(stringSerializer.getName(), stringSerializer,
                        byteArraySerializer.getName(), byteArraySerializer,
                        schemaRegistryAvroSerializer.getName(), schemaRegistryAvroSerializer),
                ImmutableMap.of(stringDeserializer.getName(), stringDeserializer,
                        byteArrayDeserializer.getName(), byteArrayDeserializer,
                        schemaRegistryAvroDeserializer.getName(), schemaRegistryAvroDeserializer
                )
        );
    }

    abstract JsonHighlighter jsonHighlighter(JsonHighlighter jsonHighlighter);

    abstract ProducerUtil producerUtil(ProducerUtil producerUtil);

    abstract KafkaConsumerService kafkaConsumerService(KafkaConsumerService kafkaConsumerService);

//    abstract KafkaMessageView kafkaMessageView(KafkaMessageView kafkaMessageView);

//    @Provides
//    Callback<Class<?>, Object>  provideControllerFactory(Map<Class<?>, Object> controllerFactory) {
//        return controllerFactory::get;
//    }

//    public static abstract class DaggerCallback implements Callback<Class<?>, Object> {
//        protected boolean useDagger;
//    }
}

