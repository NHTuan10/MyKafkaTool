package io.github.nhtuan10.mykafkatool;

import atlantafx.base.theme.PrimerLight;
import io.github.nhtuan10.mykafkatool.ui.UIErrorHandler;
import io.github.nhtuan10.mykafkatool.ui.cluster.KafkaClusterTree;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MyKafkaToolApplication extends javafx.application.Application {

    @Override
    public void start(Stage stage) throws IOException {
        Thread.setDefaultUncaughtExceptionHandler(UIErrorHandler::showError);
        FXMLLoader fxmlLoader = new FXMLLoader(MyKafkaToolApplication.class.getResource("main-view.fxml"));
        Parent parent = fxmlLoader.load();
        Scene scene = new Scene(parent);
//        parent.setStyle("-fx-base: rgba(60, 60, 60, 255);");
        javafx.application.Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        URL cssResource = MyKafkaToolApplication.class.getResource("style.css");
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