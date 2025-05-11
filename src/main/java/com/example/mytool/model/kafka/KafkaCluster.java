package com.example.mytool.model.kafka;

import com.example.mytool.model.common.Connection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KafkaCluster extends Connection {
    private String name;
    private String bootstrapServer;
    private String schemaRegistryUrl;

}
