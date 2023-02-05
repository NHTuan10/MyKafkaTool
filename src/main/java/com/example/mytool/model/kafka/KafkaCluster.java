package com.example.mytool.model.kafka;

import com.example.mytool.model.common.Connection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KafkaCluster extends Connection {
    private String name;
    private String bootstrapServer;

}
