package io.github.nhtuan10.mykafkatool;

import io.github.nhtuan10.mykafkatool.util.Utils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class UtilsTest {

    @Test
    void evalHandleBar() throws IOException {
//        String template = "{{randomInt}}, {{randomInt 30}}, {{randomInt 30 435}},, {{randomString}}, {{randomString 12}}, {{randomUUID}}, {{counter}}, {{counter}}";
        String template = "{{randomInt}} {{randomInt 30}} {{randomDouble 23.6 157.99}}  {{counter}} {{randomString 10}}  ?\u20DD {{currentDate}} {{currentDateTime}}";
//        System.out.println(Utils.evalHandleBar(template,  3));
        System.out.println(Utils.evalHandleBar("{{currentDate 'YYYY'}} {{epochMillis}}  {{epochSeconds}} {{notInContext}}", 1));
    }
}