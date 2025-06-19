package io.github.nhtuan10.mykafkatool.util;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public enum HandleBarsHelpers implements Helper<Object> {
    randomInt {
        @Override
        public Object apply(Object context, Options options) throws IOException {
            if (context instanceof Number) {
                int i = ((Number) context).intValue();
                if (options.params.length > 0) {
                    return ThreadLocalRandom.current().nextInt(i, options.param(0));
                } else {
                    return ThreadLocalRandom.current().nextInt(i);
                }
            } else {
                return ThreadLocalRandom.current().nextInt();
            }
        }
    },
    randomDouble {
        @Override
        public Object apply(Object context, Options options) throws IOException {
            if (context instanceof Number) {
                double i = ((Number) context).doubleValue();
                if (options.params.length > 0) {
                    return ThreadLocalRandom.current().nextDouble(i, options.param(0));
                } else {
                    return ThreadLocalRandom.current().nextDouble(i);
                }
            } else {
                return ThreadLocalRandom.current().nextDouble();
            }
        }
    },
    uuid {
        @Override
        public Object apply(Object context, Options options) throws IOException {
            return UUID.randomUUID().toString();
        }
    },
    randomString {
        @Override
        public Object apply(Object context, Options options) throws IOException {
            Random random = new Random();
            if (context instanceof Number) {
                int i = ((Number) context).intValue();
                if (options.params.length > 0) {
                    return generator(ALPHA_NUMERICS, random.nextInt(i, options.param(1)));
                } else {
                    return generator(ALPHA_NUMERICS, i);
                }
            } else {
                return generator(ALPHA_NUMERICS, random.nextInt(0, 36));
            }
        }
    },

    currentDate {
        @Override
        public Object apply(Object context, Options options) throws IOException {
            if (context instanceof String fmt) {
                return LocalDate.now().format(DateTimeFormatter.ofPattern(fmt));
            } else {
                return LocalDate.now();
            }
        }
    },
    currentTime {
        @Override
        public Object apply(Object context, Options options) throws IOException {
            if (context instanceof String fmt) {
                return LocalTime.now().format(DateTimeFormatter.ofPattern(fmt));
            } else {
                return LocalTime.now();
            }
        }

    },
    currentDateTime {
        @Override
        public Object apply(Object context, Options options) throws IOException {
            if (context instanceof String fmt) {
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern(fmt));
            } else {
                return LocalDateTime.now();
            }
        }

    },
    epochMillis {
        @Override
        public Object apply(Object context, Options options) throws IOException {
            return Instant.now().toEpochMilli();
        }
    },
    epochSeconds {
        @Override
        public Object apply(Object context, Options options) throws IOException {

            return Instant.now().getEpochSecond();
        }
    },
    round {
        @Override
        public Object apply(Object context, Options options) throws IOException {
            return roundNumber(context, options, RoundingMode.HALF_UP);
        }
    },
    ceil {
        @Override
        public Object apply(Object context, Options options) throws IOException {
            return roundNumber(context, options, RoundingMode.CEILING);
        }
    },
    floor {
        @Override
        public Object apply(Object context, Options options) throws IOException {
            return roundNumber(context, options, RoundingMode.FLOOR);
        }
    },
    math {
        @RequiredArgsConstructor
        @Getter
        enum BiOp {
            add("+", (a, b) -> BigDecimal.valueOf(a.doubleValue()).add(BigDecimal.valueOf(b.doubleValue()))), sub("-", (a, b) -> BigDecimal.valueOf(a.doubleValue()).add(BigDecimal.valueOf(b.doubleValue()).negate())), mul("*", (a, b) -> BigDecimal.valueOf(a.doubleValue()).multiply(BigDecimal.valueOf(b.doubleValue()))), div("/", (a, b) -> BigDecimal.valueOf(a.doubleValue()).divide(BigDecimal.valueOf(b.doubleValue()), 10, RoundingMode.HALF_UP)), mod("%", (a, b) -> a.longValue() % b.longValue());
            private final String symbol;
            private final BiFunction<Number, Number, Number> function;

            public static BiOp fromSymbol(String symbol) {
                return Arrays.stream(BiOp.values()).filter(biOp -> biOp.symbol.equals(symbol))
                        .findFirst().orElse(null);
            }
        }
        @RequiredArgsConstructor
        @Getter
        enum TriOp {
            div("/", (a, b, c) -> BigDecimal.valueOf(a.doubleValue()).divide(BigDecimal.valueOf(b.doubleValue()), c.intValue(), RoundingMode.HALF_UP));

            private final String symbol;
            private final TriFunction<Number, Number, Number, Number> function;

            public static TriOp fromSymbol(String symbol) {
                return Arrays.stream(TriOp.values()).filter(biOp -> biOp.symbol.equals(symbol))
                        .findFirst().orElse(null);
            }
        }

        @Override
        public Object apply(Object context, Options options) throws IOException {
            if (context instanceof Number) {
                double d = ((Number) context).doubleValue();
                BiOp biOp;
                TriOp triOp;
                if (options.params.length == 2 && (biOp = BiOp.fromSymbol(options.param(0))) != null && options.param(1) instanceof Number) {
                    return biOp.getFunction().apply(d, options.param(1));
                } else if (options.params.length == 3 && (triOp = TriOp.fromSymbol(options.param(0))) != null
                        && options.param(1) instanceof Number opt2 && options.param(2) instanceof Number opt3) {
                    return triOp.getFunction().apply(d, opt2, opt3);
                } else {
                    throw new IllegalArgumentException("math helpers doesn't support your expressions");
                }
            } else {
                throw new IllegalArgumentException("math helpers requires numbers, got " + context);
            }
        }

    };

    static Object roundNumber(Object context, Options options, RoundingMode mode) {
        if (context instanceof Number) {
            double d = ((Number) context).doubleValue();
            if (options.params.length > 0 && options.param(0) instanceof Integer scale) {
                return BigDecimal.valueOf(d).setScale(scale, mode);
            } else {
                return BigDecimal.valueOf(d).setScale(0, mode);
            }
        } else {
            throw new IllegalArgumentException("round requires a number, got " + context);
        }
    }

    //        counter {
//            @Override
//            public Object apply(Object context, Options options) throws IOException {
//                if (context instanceof Map c) {
//                    if (c.containsKey(COUNTER) && c.get(COUNTER) instanceof Integer) {
//                        int next = (int) c.get(COUNTER) + 1;
//                        c.put(COUNTER, next);
//                        return next;
//                    } else {
//                        return "";
//                    }
//                }
//                return "";
//            }
//        };
//        public static final String COUNTER = "counter";
    static final String ALPHA_NUMERICS = Stream.concat(Stream.concat(IntStream.range('A', 'Z' + 1).mapToObj(c -> (char) c), IntStream.range('a', 'z' + 1).mapToObj(c -> (char) c))
                    , IntStream.range('0', '9' + 1).mapToObj(c -> (char) c))
            .map(String::valueOf)
            .collect(Collectors.joining());

    static String generator(String alphabet, int n) {
        Random random = new Random();
        return IntStream.range(0, n).mapToObj(i -> String.valueOf(alphabet.charAt(random.nextInt(alphabet.length()))))
                .collect(Collectors.joining());
    }


    @FunctionalInterface
    interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
