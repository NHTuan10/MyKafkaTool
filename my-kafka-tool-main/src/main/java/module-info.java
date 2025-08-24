module io.github.nhtuan10.mykafkatool {
    requires atlantafx.base;
    requires com.fasterxml.jackson.databind;
//    requires com.google.common;
    requires javafx.controls;
    requires transitive javafx.fxml;
    requires java.scripting;
    requires javafx.graphics;
//    requires static kafka.avro.serializer;
    requires static lombok;
//    requires org.apache.avro;
    requires org.apache.commons.lang3;
//    requires org.fxmisc.richtext;
    requires org.slf4j;
    requires jakarta.inject;
    requires io.github.nhtuan10.modular;
    requires io.github.nhtuan10.modular.impl;
    requires io.github.nhtuan10.mykafkatool.api;
    requires static io.github.nhtuan10.mykafkatool.annotationprocessor;
    requires org.apache.commons.io;
    requires com.fasterxml.jackson.dataformat.csv;
    requires com.github.jknack.handlebars;
//    requires kafka.clients;
//    requires kafka.clients;
//    requires static kafka.schema.serializer;
//    requires static kafka.schema.registry.client;

    opens io.github.nhtuan10.mykafkatool to javafx.fxml, io.github.nhtuan10.modular, io.github.nhtuan10.modular.impl;
    opens io.github.nhtuan10.mykafkatool.ui.controller to javafx.fxml;
    opens io.github.nhtuan10.mykafkatool.ui.control to javafx.fxml;
    exports io.github.nhtuan10.mykafkatool;
    exports io.github.nhtuan10.mykafkatool.model.kafka to com.fasterxml.jackson.databind;
    opens io.github.nhtuan10.mykafkatool.ui to javafx.base;
    opens io.github.nhtuan10.mykafkatool.ui.consumergroup to javafx.base, javafx.fxml;
    opens io.github.nhtuan10.mykafkatool.ui.cluster to javafx.base;
    opens io.github.nhtuan10.mykafkatool.ui.topic to javafx.base, javafx.fxml;
    opens io.github.nhtuan10.mykafkatool.ui.messageview to javafx.base, javafx.fxml;
    opens io.github.nhtuan10.mykafkatool.ui.schemaregistry to javafx.base, javafx.fxml;
    opens io.github.nhtuan10.mykafkatool.userpreference to com.fasterxml.jackson.databind;
    opens io.github.nhtuan10.mykafkatool.constant to com.fasterxml.jackson.databind;


}