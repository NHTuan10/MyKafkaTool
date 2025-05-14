package com.example.mytool.ui.controller;

import javafx.collections.ObservableList;
import javafx.scene.control.*;
import lombok.Setter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Setter
public abstract class ModalController {

    protected AtomicReference<Object> modelRef;


    public void setTextFieldOrAreaText(ModalController modalController, Map<String, Object> text) {
        text.forEach((fieldName, value) -> {
            try {
                Field field = this.getClass().getDeclaredField(fieldName);
                Class<?> fieldClass = field.getType();
                field.setAccessible(true);
                Object fieldObject = field.get(modalController);
                if (fieldClass.equals(TextField.class) || fieldClass.equals(TextArea.class)) {
                    TextInputControl.class.getDeclaredMethod("setText", String.class).invoke(fieldObject, value);
                } else if ((fieldClass.equals(TableView.class) || fieldClass.equals(ComboBox.class)) && value instanceof ObservableList) {
                    fieldClass.getDeclaredMethod("setItems", ObservableList.class).invoke(fieldObject, value);
                } else {
                    field.set(modalController, value);
//                    throw new RuntimeException("Field is not supported");
                }
            } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void configureEditableControls(boolean editable) {

    }
}
