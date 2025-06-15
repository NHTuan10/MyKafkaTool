package io.github.nhtuan10.mykafkatool.api.auth;

import java.util.Map;

/**
 * @param properties will be added in consumer & producer properties
 */
public record AuthConfig(String name, Map<String, Object> properties, Object extraConfig) {
}