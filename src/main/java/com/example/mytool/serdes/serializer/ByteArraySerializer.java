package com.example.mytool.serdes.serializer;

import com.example.mytool.api.PluggableSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ByteArraySerializer implements PluggableSerializer {
    @Override
    public String getName() {
        return "Byte Array";
    }

    @Override
    public Object convertStringToObject(String str, Map<String, Object> optionalParams) throws IOException {
        return str.getBytes(StandardCharsets.UTF_8);
    }
}
