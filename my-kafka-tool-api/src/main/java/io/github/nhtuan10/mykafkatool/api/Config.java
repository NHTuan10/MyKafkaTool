package io.github.nhtuan10.mykafkatool.api;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Config {
    public static final String IS_KEY_PROP = "isKey";
    public static final String SCHEMA_PROP = "schema";
    public static final String AUTH_EXTRA_CONFIG_PROP = "authExtraConfig";
    public static final String AUTH_CONFIG_PROP = "authConfig";
    public static final String SCHEMA_REGISTRY_URL_PROP = "schema.registry.url";

    private Config() {
    }

    public static ObjectMapper constructPrettyPrintObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, false)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }
}
