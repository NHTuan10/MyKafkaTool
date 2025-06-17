package io.github.nhtuan10.mykafkatool;

import io.github.nhtuan10.modular.api.module.ModuleLoader;

public class ModularLauncher {
    public static void main(String[] args) {
        ModuleLoader moduleLoader = ModuleLoader.getInstance();
        moduleLoader.startModuleSyncWithMainClass("my-kafka-tool", "mvn://io.github.nhtuan10/my-kafka-tool-main/0.1.0-SNAPSHOT", "io.github.nhtuan10.mykafkatool.MyKafkaToolApplication", "*");
//        moduleLoader.startModuleSyncWithMainClass("my-kafka-tool", "mvn://io.github.nhtuan10/my-kafka-tool-main/0.1.0-SNAPSHOT", "io.github.nhtuan10.mykafkatool.MyKafkaToolLauncher", "*");
    }
}