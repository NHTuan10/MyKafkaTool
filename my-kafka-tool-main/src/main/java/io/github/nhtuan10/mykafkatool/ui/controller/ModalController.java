package io.github.nhtuan10.mykafkatool.ui.controller;

import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageHeaderTable;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Setter;
import org.fxmisc.richtext.CodeArea;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Setter
public abstract class ModalController {

    protected AtomicReference<Object> modelRef;

    protected Stage stage;

    public void setFields(ModalController modalController, Stage stage, Map<String, Object> text) {
        this.stage = stage;
        text.forEach((fieldName, value) -> {
            try {
                Field field = this.getClass().getDeclaredField(fieldName);
                Class<?> fieldClass = field.getType();
                field.setAccessible(true);
                Object fieldObject = field.get(modalController);
                if (fieldClass.equals(TextField.class) || fieldClass.equals(TextArea.class)) {
                    fieldClass.getMethod("setText", String.class).invoke(fieldObject, value);
                } else if (fieldObject instanceof CodeArea codeArea && value instanceof String str) {
                    codeArea.replaceText(str);
                } else if ((fieldClass.equals(TableView.class) || fieldClass.equals(ComboBox.class)) && value instanceof ObservableList) {
                    fieldClass.getMethod("setItems", ObservableList.class).invoke(fieldObject, value);
                } else if (fieldClass.equals(KafkaMessageHeaderTable.class) && value instanceof ObservableList) {
                    fieldClass.getMethod("setItems", ObservableList.class).invoke(fieldObject, value);
                } else if (fieldObject instanceof ObjectProperty<?> property) {
                    fieldClass.getMethod("setValue", Object.class).invoke(property, value);
                } else {
                    field.set(modalController, value);
                }
            } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void launch(boolean editable) {

    }
}
