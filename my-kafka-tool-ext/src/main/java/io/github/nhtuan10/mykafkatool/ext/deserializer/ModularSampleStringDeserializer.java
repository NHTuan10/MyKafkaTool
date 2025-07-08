package io.github.nhtuan10.mykafkatool.ext.deserializer;

import io.github.nhtuan10.mykafkatool.api.serdes.PluggableDeserializer;

//@ModularService
public class ModularSampleStringDeserializer implements PluggableDeserializer {
    @Override
    public String getName() {
        return "Modular Sample String";
    }

    @Override
    public String getDeserializerClass() {
        return "org.apache.kafka.common.serialization.StringDeserializer";
    }
}
