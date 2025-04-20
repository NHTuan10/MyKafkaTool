package com.example.mytool.serde;

import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroSerializer implements Serializer<GenericRecord>  {


    @Override
    public byte[] serialize(String topic, GenericRecord data) {
        byte[] bytes = new byte[0];
        try {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                BinaryEncoder binaryEncoder = EncoderFactory.get().binaryEncoder(outputStream, null);
                GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(data.getSchema());
                writer.write(data, binaryEncoder);
                binaryEncoder.flush();
                bytes =  outputStream.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
