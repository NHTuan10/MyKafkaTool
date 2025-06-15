package io.github.nhtuan10.mykafkatool.serdes.deserializer;

import io.github.nhtuan10.mykafkatool.api.serdes.PluggableDeserializer;

public class StringDeserializer implements PluggableDeserializer {
    @Override
    public String getName() {
        return "String";
    }

    @Override
    public String getDeserializerClass() {
        return "org.apache.kafka.common.serialization.StringDeserializer";
    }
}
