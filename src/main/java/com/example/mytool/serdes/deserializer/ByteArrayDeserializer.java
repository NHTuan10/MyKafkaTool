package com.example.mytool.serdes.deserializer;

import com.example.mytool.api.PluggableDeserializer;

public class ByteArrayDeserializer implements PluggableDeserializer {
    @Override
    public String getName() {
        return "Byte Array";
    }

}
