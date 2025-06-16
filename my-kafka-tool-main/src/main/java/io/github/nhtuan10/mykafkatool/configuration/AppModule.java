package io.github.nhtuan10.mykafkatool.configuration;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import io.github.nhtuan10.mykafkatool.api.auth.AuthProvider;
import io.github.nhtuan10.mykafkatool.api.auth.NoAuthProvider;
import io.github.nhtuan10.mykafkatool.api.auth.SaslProvider;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.configuration.annotation.RichTextFxObjectMapper;
import io.github.nhtuan10.mykafkatool.configuration.annotation.SharedObjectMapper;
import io.github.nhtuan10.mykafkatool.configuration.annotation.SharedPrettyPrintObjectMapper;
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
import io.github.nhtuan10.mykafkatool.userpreference.UserPreferenceRepo;
import io.github.nhtuan10.mykafkatool.userpreference.UserPreferenceRepoImpl;

import java.util.concurrent.SubmissionPublisher;

@Module
public abstract class AppModule {

    @AppScoped
    @Provides
    static EventDispatcher eventDispatcher() {
        return new EventDispatcher(new SubmissionPublisher<>()
                , new SubmissionPublisher<>(), new SubmissionPublisher<>(), new SubmissionPublisher<>());
    }

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

    @Binds
    abstract UserPreferenceRepo userPreferenceRepo(UserPreferenceRepoImpl userPreferenceRepo);


    @AppScoped
    @Provides
    @SharedObjectMapper
    static ObjectMapper sharedObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules();
    }

    @AppScoped
    @Provides
    @SharedPrettyPrintObjectMapper
    static ObjectMapper sharedPrettyPrintObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, false)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @AppScoped
    @Provides
    @RichTextFxObjectMapper
    static ObjectMapper richTextFxObjectMapper() {
        DefaultPrettyPrinter p = new DefaultPrettyPrinter();
        DefaultPrettyPrinter.Indenter i = new DefaultIndenter("  ", "\n");
        p.indentArraysWith(i);
        p.indentObjectsWith(i);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return objectMapper.setDefaultPrettyPrinter(p);
    }

    @Binds
    @IntoMap
    @StringKey(AuthProvider.NO_AUTH)
    @AppScoped
//    @Named("noAuthProvider")
    abstract AuthProvider noAuthProvider(NoAuthProvider provider);

    @Binds
    @IntoMap
    @StringKey(SaslProvider.SASL)
    @AppScoped
//    @Named("saslProvider")
    abstract AuthProvider saslProvider(SaslProvider controller);

//    @Provides
//    Callback<Class<?>, Object>  provideControllerFactory(Map<Class<?>, Object> controllerFactory) {
//        return controllerFactory::get;
//    }

//    public static abstract class DaggerCallback implements Callback<Class<?>, Object> {
//        protected boolean useDagger;
//    }
}

