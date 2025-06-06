package io.github.nhtuan10.mykafkatool.api;

import java.util.Map;

/**
 * @param properties will be added in consumer & producer properties
 */
public record AuthConfig(String name, Map<String, Object> properties, Map<String, Object> extraProperties) {
}