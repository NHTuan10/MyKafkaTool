package com.example.mytool;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ModalController {
    //    protected Object parentController;
    protected AtomicReference<Object> modelRef;

//    public void setParentController(Object parentController) {
//        this.parentController = parentController;
//    }

    public void setModelRef(AtomicReference<Object> modelRef) {
        this.modelRef = modelRef;
    }

    public void setTextFieldOrAreaText(ModalController modalController, Map<String, String> text) {
        text.forEach((fieldName, value) -> {
            try {
                Field field = this.getClass().getDeclaredField(fieldName);
                Class fieldClass = field.getType();
                Object fieldObject = field.get(modalController);
                if (fieldClass.equals(TextField.class) || fieldClass.equals(TextArea.class)) {
                    fieldClass.getDeclaredMethod("setText").invoke(fieldObject);
                } else {
                    throw new RuntimeException("Field is not supported");
                }
            } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });

    }
}
