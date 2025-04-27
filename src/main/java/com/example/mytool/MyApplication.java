package com.example.mytool;

import com.example.mytool.ui.UIConfigurer;
import com.example.mytool.ui.UIErrorHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MyApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Thread.setDefaultUncaughtExceptionHandler(UIErrorHandler::showError);
        FXMLLoader fxmlLoader = new FXMLLoader(MyApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("MyTool");
        stage.setScene(scene);
        stage.show();
        UIConfigurer.initClusterPanel(stage);

    }

    public static void main(String[] args) {
        launch();
    }
}