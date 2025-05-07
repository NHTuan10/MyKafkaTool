package com.example.mytool.serdes;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroUtil {
    public static Schema parseSchema(String schemaStr) {
        return new Schema.Parser().parse(schemaStr);
    }

    public static GenericRecord convertJsonToAvro(String json, String schemaStr) throws IOException {
        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse(schemaStr);

        DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        Decoder decoder = DecoderFactory.get().jsonDecoder(schema, json);
        return reader.read(null, decoder);
    }

    //  Deserialized Methods
    public static String deserializeAsJsonString(byte[] data, String schemaStr) throws IOException {
        Schema schema = parseSchema(schemaStr);
        GenericRecord avroRecord = deserialize(data, schema);
        return convertGenericRecordToJson(avroRecord, schema);
    }

    public static String convertGenericRecordToJson(GenericRecord avroRecord, Schema schema) throws IOException {
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
