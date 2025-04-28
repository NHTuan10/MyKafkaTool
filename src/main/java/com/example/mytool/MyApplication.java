package com.example.mytool;

import atlantafx.base.theme.PrimerLight;
import com.example.mytool.ui.KafkaClusterTree;
import com.example.mytool.ui.UIErrorHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MyApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Thread.setDefaultUncaughtExceptionHandler(UIErrorHandler::showError);
        FXMLLoader fxmlLoader = new FXMLLoader(MyApplication.class.getResource("main-view.fxml"));
        Parent parent = fxmlLoader.load();
        Scene scene = new Scene(parent);
//        parent.setStyle("-fx-base: rgba(60, 60, 60, 255);");
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
//        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        stage.setTitle("MyTool");
        stage.setScene(scene);
        stage.show();
        KafkaClusterTree.initClusterPanel(stage);
//        TableViewConfigurer.initTableView(stage);
    }

    public static void main(String[] args) {
        launch();
    }
}