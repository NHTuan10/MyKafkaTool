package io.github.nhtuan10.mykafkatool.configuration.annotation;

import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Qualifier
@Retention(RUNTIME)
public @interface ViewControllerMap {
}
