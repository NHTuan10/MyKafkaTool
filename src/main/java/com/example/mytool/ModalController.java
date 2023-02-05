package com.example.mytool;

import java.util.concurrent.atomic.AtomicReference;

public abstract class ModalController {
    protected MainController mainController;
    protected AtomicReference<Object> modelRef;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setModelRef(AtomicReference<Object> modelRef) {
        this.modelRef = modelRef;
    }
}
