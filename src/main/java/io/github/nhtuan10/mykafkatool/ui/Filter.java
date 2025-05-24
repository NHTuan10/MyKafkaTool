package io.github.nhtuan10.mykafkatool.ui;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Data
@AllArgsConstructor
public class Filter {
    private String filterText;
    private boolean isRegexFilter;

    @SafeVarargs
    public static <T> Predicate<T> buildFilterPredicate(@NonNull Filter filter, Function<T, String>... fieldGetters) {
        assert (filter.getFilterText() != null);
        if (filter.isRegexFilter()) {
            Pattern pattern = Pattern.compile(filter.getFilterText(), Pattern.CASE_INSENSITIVE);
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
                    isMatched = isMatched || (fieldGetter.apply(item) != null && fieldGetter.apply(item).toLowerCase().contains(filter.getFilterText().toLowerCase()));
                }
                return isMatched;
            };

        }
    }
}
