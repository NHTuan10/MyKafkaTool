package com.example.mytool.serde;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;

public class Util {

    public static final String SERDE_STRING = "String";
    public static final String SERDE_AVRO = "AVRO";

    public static String getSerdeClass(String serde) {
        switch (serde){
            case SERDE_STRING:
                return StringSerializer.class.getName();
            case SERDE_AVRO:
                return AvroSerializer.class.getName();
            default:
                return StringSerializer.class.getName();
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


}
