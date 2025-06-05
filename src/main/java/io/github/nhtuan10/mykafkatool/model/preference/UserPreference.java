package io.github.nhtuan10.mykafkatool.model.preference;

import io.github.nhtuan10.mykafkatool.constant.Theme;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;

import java.util.List;

public record UserPreference(List<KafkaCluster> connections, Theme theme) {
    public UserPreference(List<KafkaCluster> connections) {
        this(connections, Theme.LIGHT);
    }
}
