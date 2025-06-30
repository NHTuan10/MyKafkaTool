package io.github.nhtuan10.mykafkatool.ext.serializer;

import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.mykafkatool.api.serdes.PluggableSerializer;

@ModularService
public class ModularSampleStringSerializer implements PluggableSerializer {
    @Override
    public String getName() {
        return "Modular Sample String";
    }

    @Override
    public String getSerializerClass() {
        return "org.apache.kafka.common.serialization.StringSerializer";
    }
}
