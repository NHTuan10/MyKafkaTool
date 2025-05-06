package com.example.mytool.serde.serializer;

import com.example.mytool.api.PluggableSerializer;

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
