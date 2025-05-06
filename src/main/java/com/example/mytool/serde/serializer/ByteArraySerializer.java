package com.example.mytool.serde.serializer;

import com.example.mytool.api.PluggableSerializer;

public class ByteArraySerializer implements PluggableSerializer {
    @Override
    public String getName() {
        return "Byte Array";
    }

}
