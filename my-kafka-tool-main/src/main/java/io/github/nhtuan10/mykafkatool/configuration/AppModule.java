package io.github.nhtuan10.mykafkatool.configuration;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import io.github.nhtuan10.modular.api.Modular;
import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.mykafkatool.api.Config;
import io.github.nhtuan10.mykafkatool.api.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.api.auth.AuthProvider;
import io.github.nhtuan10.mykafkatool.api.serdes.PluggableDeserializer;
import io.github.nhtuan10.mykafkatool.api.serdes.PluggableSerializer;
import io.github.nhtuan10.mykafkatool.auth.NoAuthProvider;
import io.github.nhtuan10.mykafkatool.auth.SaslPlaintextProvider;
import io.github.nhtuan10.mykafkatool.auth.SaslSslProvider;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.configuration.annotation.RichTextFxObjectMapper;
import io.github.nhtuan10.mykafkatool.configuration.annotation.SharedObjectMapper;
import io.github.nhtuan10.mykafkatool.configuration.annotation.SharedPrettyPrintObjectMapper;
import io.github.nhtuan10.mykafkatool.consumer.KafkaConsumerService;
import io.github.nhtuan10.mykafkatool.producer.ProducerUtil;
import io.github.nhtuan10.mykafkatool.schemaregistry.SchemaRegistryManagerImpl;
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
import jakarta.inject.Named;

import java.util.*;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.Collectors;

@Module
public abstract class AppModule {

    @AppScoped
    @Provides
    static EventDispatcher eventDispatcher() {
        return new EventDispatcher(new SubmissionPublisher<>()
                , new SubmissionPublisher<>(), new SubmissionPublisher<>(), new SubmissionPublisher<>(), new SubmissionPublisher<>(), new SubmissionPublisher<>());
    }

    @AppScoped
    @Provides
    static SerDesHelper serDesHelper() {
//        StringSerializer stringSerializer = new StringSerializer();
//        StringDeserializer stringDeserializer = new StringDeserializer();
//        ByteArraySerializer byteArraySerializer = new ByteArraySerializer();
//        ByteArrayDeserializer byteArrayDeserializer = new ByteArrayDeserializer();
//        SchemaRegistryAvroSerializer schemaRegistryAvroSerializer = new SchemaRegistryAvroSerializer();
//        SchemaRegistryAvroDeserializer schemaRegistryAvroDeserializer = new SchemaRegistryAvroDeserializer();
//        Map<String, PluggableSerializer> serializers = new LinkedHashMap<>();
//        Map<String, PluggableDeserializer> deserializers = new LinkedHashMap<>();

        List<PluggableSerializer> serializers = new ArrayList<>(List.of(new StringSerializer(), new ByteArraySerializer(), new SchemaRegistryAvroSerializer()));

        List<PluggableDeserializer> deserializers = new ArrayList<>(List.of(new StringDeserializer(), new ByteArrayDeserializer(), new SchemaRegistryAvroDeserializer()));

//        serializers.put(stringSerializer.getName(), stringSerializer);
//        serializers.put(byteArraySerializer.getName(), byteArraySerializer);
//        serializers.put(schemaRegistryAvroSerializer.getName(), schemaRegistryAvroSerializer);
//
//        deserializers.put(stringDeserializer.getName(), stringDeserializer);
//        deserializers.put(byteArrayDeserializer.getName(), byteArrayDeserializer);
//        deserializers.put(schemaRegistryAvroDeserializer.getName(), schemaRegistryAvroDeserializer);

        if (Modular.isManaged(AppModule.class)) {
            serializers.addAll(Modular.getModularServices(PluggableSerializer.class));
            deserializers.addAll(Modular.getModularServices(PluggableDeserializer.class));
        }
        return new SerDesHelper(
                Collections.unmodifiableMap((Map<? extends String, ? extends PluggableSerializer>) serializers.stream().collect(Collectors.toMap(PluggableSerializer::getName, s -> s, (x, y) -> y, LinkedHashMap::new))),
                Collections.unmodifiableMap((Map<? extends String, ? extends PluggableDeserializer>) deserializers.stream().collect(Collectors.toMap(PluggableDeserializer::getName, s -> s, (x, y) -> y, LinkedHashMap::new)))
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
        return Config.constructPrettyPrintObjectMapper();
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
//    @IntoMap
//    @StringKey(AuthProvider.NO_AUTH)
    @AppScoped
    @Named("noAuthProvider")
    abstract AuthProvider noAuthProvider(NoAuthProvider provider);

    @Binds
//    @IntoMap
//    @StringKey(SaslProvider.SASL)
    @AppScoped
    @Named("saslPlaintextProvider")
    abstract AuthProvider saslPlaintextProvider(SaslPlaintextProvider provider);


    @Binds
//    @IntoMap
//    @StringKey(SaslProvider.SASL)
    @AppScoped
    @Named("saslSslProvider")
    abstract AuthProvider saslSslProvider(SaslSslProvider provider);

    @Provides
    @AppScoped
    static Map<String, AuthProvider> authProviderMap(@Named("noAuthProvider") AuthProvider noAuthProvide,
                                                     @Named("saslPlaintextProvider") AuthProvider saslPlaintextProvider,
                                                     @Named("saslSslProvider") AuthProvider saslSslProvider) {
        Map<String, AuthProvider> authProviderMap = new LinkedHashMap<>();
        authProviderMap.put(noAuthProvide.getName(), noAuthProvide);
        authProviderMap.put(saslPlaintextProvider.getName(), saslPlaintextProvider);
        authProviderMap.put(saslSslProvider.getName(), saslSslProvider);
        if (Modular.isManaged(AppModule.class)) {
            List<AuthProvider> extAuthProviders = Modular.getModularServices(AuthProvider.class);
            extAuthProviders.forEach(authProvider -> authProviderMap.put(authProvider.getName(), authProvider));
        }
        return Collections.unmodifiableMap(authProviderMap);
    }

    @Provides
    @ModularService
    @AppScoped
    static SchemaRegistryManager schemaRegistryManager(){
        return new SchemaRegistryManagerImpl();
    }
//    @Provides
//    @IntoMap
//    @StringKey(SaslProvider.SASL)
//    @AppScoped
//    @Named("saslProvider")
//    abstract AuthProvider modularSampleSaslProvider(){

//    }
//    @Provides
//    Callback<Class<?>, Object>  provideControllerFactory(Map<Class<?>, Object> controllerFactory) {
//        return controllerFactory::get;
//    }

//    public static abstract class DaggerCallback implements Callback<Class<?>, Object> {
//        protected boolean useDagger;
//    }
}

