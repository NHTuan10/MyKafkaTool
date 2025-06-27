package io.github.nhtuan10.mykafkatool.ui;

import io.github.nhtuan10.mykafkatool.annotationprocessor.FXModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

//@Data
//@AllArgsConstructor
@FXModel
public final class Filter implements FilterFXModel {
    StringProperty filterText;
    BooleanProperty isRegexFilter;
    BooleanProperty isCaseSensitive;
    BooleanProperty isNegative;

    public Filter() {
        this.filterText = new SimpleStringProperty("");
        this.isRegexFilter = new SimpleBooleanProperty(false);
        this.isCaseSensitive = new SimpleBooleanProperty(false);
        this.isNegative = new SimpleBooleanProperty(false);
    }

    public Filter(String filterText, boolean isRegexFilter, boolean isCaseSensitive, boolean isNegative) {
        this.filterText = new SimpleStringProperty(filterText);
        this.isRegexFilter = new SimpleBooleanProperty(isRegexFilter);
        this.isCaseSensitive = new SimpleBooleanProperty(isCaseSensitive);
        this.isNegative = new SimpleBooleanProperty(isNegative);
    }

    public Filter copy() {
        return FilterFXModel.builder().filterText(filterText.get())
                .isRegexFilter(isRegexFilter.get())
                .isCaseSensitive(isCaseSensitive.get())
                .isNegative(isNegative.get()).build();
    }

    public static <T> Predicate<T> buildFilterPredicate(@NonNull Filter filter, List<Function<T, String>> fieldGetters) {
        assert (filter.getFilterText() != null);
        String filterText = filter.getFilterText().trim();
        if (filter.getIsRegexFilter()) {
            Pattern pattern = filter.getIsCaseSensitive() ? Pattern.compile(filterText) : Pattern.compile(filterText, Pattern.CASE_INSENSITIVE);
            return item -> {
                boolean isMatched = false;
                for (Function<T, String> fieldGetter : fieldGetters) {
                    isMatched = isMatched || (fieldGetter.apply(item) != null && pattern.matcher(fieldGetter.apply(item)).find());
                }
                return filter.getIsNegative() ? !isMatched : isMatched;
            };
        } else {
            return item -> {
                boolean isMatched = false;
                for (Function<T, String> fieldGetter : fieldGetters) {
                    isMatched = isMatched || (fieldGetter.apply(item) != null &&
                            (filter.getIsCaseSensitive() ? fieldGetter.apply(item).contains(filterText) : fieldGetter.apply(item).toLowerCase().contains(filterText.toLowerCase())));
                }
                return filter.getIsNegative() ? !isMatched : isMatched;
            };

        }
    }

    @SafeVarargs
    public static <T> Predicate<T> buildFilterPredicate(@NonNull Filter filter, Function<T, String>... fieldGetters) {
        return buildFilterPredicate(filter, Arrays.asList(fieldGetters));
    }

}
