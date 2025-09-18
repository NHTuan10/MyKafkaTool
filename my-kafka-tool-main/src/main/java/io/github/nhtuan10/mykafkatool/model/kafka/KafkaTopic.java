package io.github.nhtuan10.mykafkatool.model.kafka;

import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;

import java.util.List;
import java.util.Objects;


public record KafkaTopic(String name, KafkaCluster cluster, List<KafkaPartition> partitions) {
    @Override
    public String toString() {
//        return "▤ " + name;
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        KafkaTopic that = (KafkaTopic) o;
        return Objects.equals(name, that.name) && Objects.equals(cluster, that.cluster);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(cluster);
        return result;
    }
}
