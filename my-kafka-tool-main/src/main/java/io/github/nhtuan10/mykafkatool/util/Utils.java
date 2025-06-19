package io.github.nhtuan10.mykafkatool.util;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class Utils {
    private static final Handlebars handlebars = new Handlebars();

    static {
        initHandleBars(handlebars);
    }

    public static void initHandleBars(Handlebars handlebars) {
        handlebars.registerHelpers(HandleBarsHelpers.class);
        handlebars.registerHelpers(StringHelpers.class);
    }

    public static List<Field> getAllFields(Class<?> clazz) {
        Stream<Field> r = Arrays.stream(clazz.getDeclaredFields());
        Class<?> p = clazz.getSuperclass();
        while (p != null) {
            r = Stream.concat(Arrays.stream(p.getDeclaredFields()), r);
            p = p.getSuperclass();
        }
        return r.toList();
    }

    public static Script groovyParse(String code) throws IOException, ClassNotFoundException {
        Binding binding = new Binding();
        binding.setProperty("counter", 0);
        GroovyShell shell = new GroovyShell(binding);
        return shell.parse(code);
    }

    public static String evalHandleBar(String templateStr) throws IOException {
        return evalHandleBar(templateStr, 1).get(0);
    }

    public static List<String> evalHandleBar(String templateStr, int numOfLoop) throws IOException {
        Template template = handlebars.compileInline(templateStr);
        List<String> r = new ArrayList<>();
        for (int i = 0; i < numOfLoop; i++) {
            Map<String, Object> context = Map.of("counter", i + 1);
            r.add(template.apply(context));
        }
        return r;
    }


    public static class HandleBarsHelpersNotWorking {
//        public static int randomInt() {
//            return ThreadLocalRandom.current().nextInt();
//        }

        public static int randomInt(int[] b1) {

            return ThreadLocalRandom.current().nextInt(b1[0]);
        }
//
//        public static int randomInt(int b1, int b2) {
//
//            return ThreadLocalRandom.current().nextInt(b1, b2);
//        }
//
//        public static double randomDouble() {
//            return ThreadLocalRandom.current().nextDouble();
//        }
//
//        public static double randomDouble(double b1) {
//
//            return ThreadLocalRandom.current().nextDouble(b1);
//        }
//
//        public static double randomDouble(double b1, double b2) {
//
//            return ThreadLocalRandom.current().nextDouble(b1, b2);
//        }
//
//        private static String generator(String alphabet, int n) {
//            Random random = new Random();
//            return IntStream.range(0, n).mapToObj(i -> alphabet.substring(random.nextInt(alphabet.length())))
//                    .collect(Collectors.joining());
//        }
//
//        public static String randomUUID() {
//            return UUID.randomUUID().toString();
//        }
//
//        public static String randomString(int numChars) {
//            String a = Stream.concat(Stream.concat(IntStream.range('A', 'Z' + 1).mapToObj(c -> (char) c), IntStream.range('a', 'z' + 1).mapToObj(c -> (char) c))
//                            , IntStream.range('0', '9' + 1).mapToObj(c -> (char) c))
//                    .map(String::valueOf)
//                    .collect(Collectors.joining());
//            return generator(a, numChars);
//        }
//
//        public static String randomString(int minChars, int maxChars) {
//           return randomString(randomInt(minChars, maxChars));
//        }
//
//        public static String  currentDate (String fmt) {
//            return LocalDate.now().format(DateTimeFormatter.ofPattern(fmt));
//        }
//
//        public static LocalDate  currentDate () {
//            return LocalDate.now();
//        }
//
//        public static String  currentDateTime (String fmt) {
//            return LocalDateTime.now().format(DateTimeFormatter.ofPattern(fmt));
//        }
//
//        public static LocalDateTime  currentDateTIme () {
//            return LocalDateTime.now();
//        }
    }

}
