package io.github.nhtuan10.mykafkatool.api;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.nhtuan10.mykafkatool.api.serdes.KafkaHeaderSerializer;
import org.apache.kafka.common.header.Headers;

public class Config {
    public static final String IS_KEY_PROP = "isKey";
    public static final String SCHEMA_PROP = "schema";
    public static final String AUTH_EXTRA_CONFIG_PROP = "authExtraConfig";
    public static final String AUTH_CONFIG_PROP = "authConfig";
    public static final String SCHEMA_REGISTRY_URL_PROP = "schema.registry.url";
    public static final String SCHEMA_ID_PROP = "schemaId";
    public static final String SCHEMA_SUBJECT_PROP = "schemaSubject";
    public static final String SCHEMA_VERSION_PROP = "schemaVersion";

    private Config() {
    }

    public static ObjectMapper constructPrettyPrintObjectMapper() {
        return constructObjectMapper()
                .configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, false)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static ObjectMapper constructObjectMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Headers.class, new KafkaHeaderSerializer());
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(module);
    }
}
