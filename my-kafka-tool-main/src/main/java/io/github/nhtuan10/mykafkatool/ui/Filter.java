package io.github.nhtuan10.mykafkatool.ui;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Data
@AllArgsConstructor
public class Filter {
    private String filterText;
    private boolean isRegexFilter;

    public static <T> Predicate<T> buildFilterPredicate(@NonNull Filter filter, List<Function<T, String>> fieldGetters) {
        assert (filter.getFilterText() != null);
        String filterText = filter.getFilterText().trim();
        if (filter.isRegexFilter()) {
            Pattern pattern = Pattern.compile(filterText, Pattern.CASE_INSENSITIVE);
            return item -> {
                boolean isMatched = false;
                for (Function<T, String> fieldGetter : fieldGetters) {
                    isMatched = isMatched || (fieldGetter.apply(item) != null && pattern.matcher(fieldGetter.apply(item)).find());
                }
                return isMatched;
            };
        } else {
            return item -> {
                boolean isMatched = false;
                for (Function<T, String> fieldGetter : fieldGetters) {
                    isMatched = isMatched || (fieldGetter.apply(item) != null && fieldGetter.apply(item).toLowerCase().contains(filterText.toLowerCase()));
                }
                return isMatched;
            };

        }
    }

    @SafeVarargs
    public static <T> Predicate<T> buildFilterPredicate(@NonNull Filter filter, Function<T, String>... fieldGetters) {
        return buildFilterPredicate(filter, Arrays.asList(fieldGetters));
    }

}
