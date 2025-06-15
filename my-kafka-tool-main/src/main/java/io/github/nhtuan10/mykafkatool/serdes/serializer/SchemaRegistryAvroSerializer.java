package io.github.nhtuan10.mykafkatool.serdes.serializer;

import io.github.nhtuan10.mykafkatool.api.Config;
import io.github.nhtuan10.mykafkatool.api.model.DisplayType;
import io.github.nhtuan10.mykafkatool.api.serdes.PluggableSerializer;
import io.github.nhtuan10.mykafkatool.serdes.AvroUtil;

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
        String schema = optionalParams.get(Config.SCHEMA_PROP).toString();
        return AvroUtil.convertJsonToAvro(str, schema);
    }

    @Override
    public boolean mayNeedUserInputForSchema() {
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
