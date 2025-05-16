package com.example.mytool.serdes.serializer;

import com.example.mytool.api.DisplayType;
import com.example.mytool.api.PluggableSerializer;
import com.example.mytool.serdes.AvroUtil;

import java.io.IOException;
import java.util.Map;

public class SchemaRegistryAvroSerializer implements PluggableSerializer {
    @Override
    public String getName() {
        return "Schema Registry Avro";
    }

    @Override
    public String getSerializerClass() {
        return "io.confluent.kafka.serializers.KafkaAvroSerializer";
    }

    @Override
    public Object convertStringToObject(String str, Map<String, Object> optionalParams) throws IOException {
        String schema = optionalParams.get("schema").toString();
        return AvroUtil.convertJsonToAvro(str, schema);
    }

    @Override
    public boolean mayUseSchema() {
        return true;
    }

    @Override
    public Object parseSchema(String schema) {
        return AvroUtil.parseSchema(schema);
    }

    @Override
    public DisplayType getDisplayType() {
        return DisplayType.JSON;
    }
}
