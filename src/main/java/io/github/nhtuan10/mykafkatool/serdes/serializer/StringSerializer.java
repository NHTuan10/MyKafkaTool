package io.github.nhtuan10.mykafkatool.serdes.serializer;

import io.github.nhtuan10.mykafkatool.api.serdes.PluggableSerializer;

public class StringSerializer implements PluggableSerializer {
    @Override
    public String getName() {
        return "String";
    }

    @Override
    public String getSerializerClass() {
        return "org.apache.kafka.common.serialization.StringSerializer";
    }
}
