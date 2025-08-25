module io.github.nhtuan10.mykafkatool.launcher {
    requires com.fasterxml.jackson.databind;

    requires static lombok;
//    requires org.apache.avro;
    requires org.apache.commons.lang3;
//    requires org.fxmisc.richtext;
    requires org.slf4j;
    requires io.github.nhtuan10.modular;
    requires io.github.nhtuan10.modular.impl;
    requires com.fasterxml.jackson.dataformat.xml;
    requires zip4j;
    requires java.desktop;
    requires java.net.http;
    requires java.scripting;
    requires jdk.dynalink;
//    requires kafka.clients;
//    requires kafka.clients;
//    requires static kafka.schema.serializer;
//    requires static kafka.schema.registry.client;
    opens io.github.nhtuan10.mykafkatool.launcher to io.github.nhtuan10.modular, io.github.nhtuan10.modular.impl;

}