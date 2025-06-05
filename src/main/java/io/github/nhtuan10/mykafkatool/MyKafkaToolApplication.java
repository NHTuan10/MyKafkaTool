package io.github.nhtuan10.mykafkatool;

import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.constant.Theme;
import io.github.nhtuan10.mykafkatool.manager.UserPreferenceManager;
import io.github.nhtuan10.mykafkatool.model.preference.UserPreference;
import io.github.nhtuan10.mykafkatool.ui.UIErrorHandler;
import io.github.nhtuan10.mykafkatool.ui.cluster.KafkaClusterTree;
import io.github.nhtuan10.mykafkatool.ui.controller.MainController;
import javafx.application.Platform;
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
        MainController mainController = fxmlLoader.getController();
        mainController.setStage(stage);
        Scene scene = new Scene(parent);
        applyThemeFromCurrentUserPreference(scene);
        stage.setTitle(AppConstant.APP_NAME);
        stage.setScene(scene);
        stage.show();
        KafkaClusterTree.initClusterPanel(stage);
        stage.setOnCloseRequest(event -> {
            exit();
        });
    }

    public static void applyTheme(Scene scene, Theme theme) {
        javafx.application.Application.setUserAgentStylesheet(theme.getUserAgentStyleSheet());
        theme.getStyleSheets().forEach(styleSheet -> {
            URL cssResource = MyKafkaToolApplication.class.getResource(styleSheet);
            scene.getStylesheets().add(cssResource.toExternalForm());
        });
        try {
            UserPreferenceManager.changeUserPreferenceTheme(theme);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void applyThemeFromCurrentUserPreference(Scene scene) {
        UserPreference userPreference = UserPreferenceManager.loadUserPreference();
        Theme theme = userPreference.theme() != null ? userPreference.theme() : Theme.LIGHT;
        applyTheme(scene, theme);
    }


    public static void changeTheme(Scene scene, Theme newTheme) {
        Theme currentTheme = UserPreferenceManager.loadUserPreference().theme();
        if (currentTheme == null) currentTheme = Theme.LIGHT;
        if (newTheme != currentTheme) {
            currentTheme.getStyleSheets().forEach(styleSheet -> {
                URL cssResource = MyKafkaToolApplication.class.getResource(styleSheet);
                scene.getStylesheets().remove(cssResource.toExternalForm());
            });
            applyTheme(scene, newTheme);
        }
    }

    public static void exit() {
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch();
    }
}