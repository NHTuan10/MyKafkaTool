package io.github.nhtuan10.mykafkatool.serdes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.ui.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class AvroUtil {
    public static Schema parseSchema(String schemaStr) {
        return new Schema.Parser().parse(schemaStr);
    }

    public static final ObjectMapper OBJECT_MAPPER = Utils.constructRichtexFxPrettyPrintObjectMapper();

    public static Object convertJsonToAvro(String json, String schemaStr) throws IOException {
        Schema schema = null;
        if (StringUtils.isNotBlank(schemaStr)) {
            try {
                Schema.Parser parser = new Schema.Parser();
                schema = parser.parse(schemaStr);
            } catch (Exception e) {
                log.error("Error parse schema {}", schema, e);
            }
        }
        if (schema != null) {
            DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
            Decoder decoder = DecoderFactory.get().jsonDecoder(schema, json);
            return reader.read(null, decoder);
        } else {
//            try {
//                return objectMapper.readValue(json, Object.class);
//            } catch (JsonProcessingException e) {
//                return json;
//            }
            return json;
        }
    }

    //  Deserialized Methods
    public static String deserializeToJsonString(byte[] data, String schemaStr) throws IOException {
        Schema schema = parseSchema(schemaStr);
        GenericRecord avroRecord = deserialize(data, schema);
        return convertObjectToJsonString(avroRecord);
    }

    public static String convertObjectToJsonString(Object deserializedObject) throws IOException {
        String result;
        result = toString(deserializedObject);
        return result;
    }

    public static String toString(Object deserializedObject) throws IOException {
        String result;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        switch (deserializedObject) {
            case null -> result = null;
            case String str -> result = str;
            case byte[] bytes -> result = new String(bytes);
            case GenericRecord avroRecord -> {
                Schema schema = avroRecord.getSchema();
                JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(schema, outputStream);
                GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
                writer.write(avroRecord, jsonEncoder);
                jsonEncoder.flush();
                result = outputStream.toString();
            }
            default -> {
//                objectMapper.writeValue(outputStream, deserializedObject);
//                result = outputStream.toString();
                result = deserializedObject.toString(); // TODO: find a better approach than toString
            }
        }
        outputStream.close();
        return result;
    }

    public static GenericRecord deserialize(byte[] data, Schema schema) throws IOException {
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        return reader.read(null, decoder);

    }
}
