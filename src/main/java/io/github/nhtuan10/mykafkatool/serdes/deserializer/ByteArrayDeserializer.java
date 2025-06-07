package io.github.nhtuan10.mykafkatool.serdes.deserializer;

import io.github.nhtuan10.mykafkatool.api.serdes.PluggableDeserializer;

public class ByteArrayDeserializer implements PluggableDeserializer {
    @Override
    public String getName() {
        return "Byte Array Base64";
    }

}
