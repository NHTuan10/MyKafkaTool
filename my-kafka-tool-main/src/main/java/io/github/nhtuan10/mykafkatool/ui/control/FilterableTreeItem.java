package io.github.nhtuan10.mykafkatool.ui.control;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;

import java.util.function.Predicate;

public class FilterableTreeItem<T> extends TreeItem<T> {
    private final ObservableList<TreeItem<T>> sourceChildren = FXCollections.observableArrayList();
    private final FilteredList<TreeItem<T>> filteredChildren = new FilteredList<>(sourceChildren);
    private final ObjectProperty<Predicate<T>> predicate = new SimpleObjectProperty<>();

    public FilterableTreeItem(T value, Node graphic) {
        super(value, graphic);
    }

    public FilterableTreeItem(T value) {
        super(value);

        filteredChildren.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            Predicate<TreeItem<T>> p = child -> {
                if (child instanceof FilterableTreeItem) {
                    ((FilterableTreeItem<T>) child).predicateProperty().set(predicate.get());
                }
                if (predicate.get() == null || (!child.getChildren().isEmpty() && child.getChildren().get(0) instanceof FilterableTreeItem)) {
                    return true;
                }
                return predicate.get().test(child.getValue());
            };
            return p;
        }, predicate));

        filteredChildren.addListener((ListChangeListener<TreeItem<T>>) c -> {
            while (c.next()) {
                getChildren().removeAll(c.getRemoved());
                getChildren().addAll(c.getAddedSubList());
            }
        });
    }

    public ObservableList<TreeItem<T>> getSourceChildren() {
        return sourceChildren;
    }

    public ObjectProperty<Predicate<T>> predicateProperty() {
        return predicate;
    }

}