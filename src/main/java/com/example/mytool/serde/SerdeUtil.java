package com.example.mytool.serde;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SerdeUtil {

    public static final String SERDE_STRING = "String";
    public static final String SERDE_AVRO = "AVRO";

    public static String getSerializeClass(String contentType) {
        switch (contentType) {
            case SERDE_AVRO:
                return AvroSerializer.class.getName();
            default:
                return StringSerializer.class.getName();
        }
    }

    public static String getDeserializeClass(String contentType) {
        switch (contentType) {
            case SERDE_AVRO:
                return ByteArrayDeserializer.class.getName();
            default:
                return StringDeserializer.class.getName();
        }
    }

    public static GenericRecord convertJsonToAvro(String json, String schemaStr) throws IOException {
        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse(schemaStr);
        try {
            DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
            Decoder decoder = DecoderFactory.get().jsonDecoder(schema, json);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new IOException("Error converting JSON to Avro", e);
        }
    }

    public static Object convert(String serdeName, String content, String schemaStr) {
        switch (serdeName) {
            case SERDE_STRING:
                return content;
            case SERDE_AVRO:
                try {
                    return convertJsonToAvro(content, schemaStr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return content;
    }

    public static String deserializeAsJsonString(byte[] data, String schemaStr) throws IOException {
        Schema schema = new Schema.Parser().parse(schemaStr);
        GenericRecord avroRecord = deserialize(data, schema);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(schema, outputStream);
        GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        writer.write(avroRecord, jsonEncoder);
        jsonEncoder.flush();
        String result = outputStream.toString();
        outputStream.close();
        return result;
    }

    public static GenericRecord deserialize(byte[] data, Schema schema) throws IOException {
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        return reader.read(null, decoder);

    }
}
