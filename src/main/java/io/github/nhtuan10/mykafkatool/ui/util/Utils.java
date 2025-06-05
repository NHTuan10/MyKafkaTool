package io.github.nhtuan10.mykafkatool.ui.util;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Utils {

    public static ObjectMapper constructPrettyPrintObjectMapper() {
        DefaultPrettyPrinter p = new DefaultPrettyPrinter();
        DefaultPrettyPrinter.Indenter i = new DefaultIndenter("  ", "\n");
        p.indentArraysWith(i);
        p.indentObjectsWith(i);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return objectMapper.setDefaultPrettyPrinter(p);
    }

    public static ObjectMapper contructObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
