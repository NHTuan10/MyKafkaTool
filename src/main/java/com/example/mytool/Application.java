package com.example.mytool;

import atlantafx.base.theme.PrimerLight;
import com.example.mytool.ui.KafkaClusterTree;
import com.example.mytool.ui.UIErrorHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Application extends javafx.application.Application {

    @Override
    public void start(Stage stage) throws IOException {
        Thread.setDefaultUncaughtExceptionHandler(UIErrorHandler::showError);
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("main-view.fxml"));
        Parent parent = fxmlLoader.load();
        Scene scene = new Scene(parent);
//        parent.setStyle("-fx-base: rgba(60, 60, 60, 255);");
        javafx.application.Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        URL cssResource = Application.class.getResource("style.css");
        scene.getStylesheets().add(cssResource.toExternalForm());
//        Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet()); //PrimerDark
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