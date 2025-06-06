package io.github.nhtuan10.mykafkatool.api;

import java.util.Properties;

/**
 * @param properties will be added in consumer & producer properties
 */
public record AuthConfig(String name, Properties properties, Properties extraProperties) {
}