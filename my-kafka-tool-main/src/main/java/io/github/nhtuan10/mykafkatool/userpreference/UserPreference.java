package io.github.nhtuan10.mykafkatool.userpreference;

import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;
import io.github.nhtuan10.mykafkatool.constant.Theme;

import java.util.List;

public record UserPreference(List<KafkaCluster> connections, Theme theme) {
    public UserPreference(List<KafkaCluster> connections) {
        this(connections, Theme.LIGHT);
    }
}
