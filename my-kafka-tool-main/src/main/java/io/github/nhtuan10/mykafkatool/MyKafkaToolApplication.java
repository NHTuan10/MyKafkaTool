package io.github.nhtuan10.mykafkatool;

import io.github.nhtuan10.modular.api.Modular;
import io.github.nhtuan10.modular.context.ModularContext;
import io.github.nhtuan10.mykafkatool.configuration.AppComponent;
import io.github.nhtuan10.mykafkatool.configuration.DaggerAppComponent;
import io.github.nhtuan10.mykafkatool.constant.AppConstant;
import io.github.nhtuan10.mykafkatool.constant.Theme;
import io.github.nhtuan10.mykafkatool.ui.UIErrorHandler;
import io.github.nhtuan10.mykafkatool.ui.controller.MainController;
import io.github.nhtuan10.mykafkatool.userpreference.UserPreference;
import io.github.nhtuan10.mykafkatool.userpreference.UserPreferenceManager;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;

import static io.github.nhtuan10.mykafkatool.constant.AppConstant.APP_NAME;

@Slf4j
public class MyKafkaToolApplication extends javafx.application.Application {
    public static final AppComponent DAGGER_APP_COMPONENT = DaggerAppComponent.create();
    private static UserPreferenceManager userPreferenceManager;
    public static final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>(Theme.LIGHT);

    @Override
    public void start(Stage stage) throws IOException {
//        context.init();
        Thread.setDefaultUncaughtExceptionHandler(UIErrorHandler::showError);
        FXMLLoader fxmlLoader = DAGGER_APP_COMPONENT.loader(MyKafkaToolApplication.class.getResource("main-view.fxml"));
//        FXMLLoader fxmlLoader = new FXMLLoader(MyKafkaToolApplication.class.getResource("main-view.fxml"));
//        fxmlLoader.setLocation( MyKafkaToolApplication.class.getResource("main-view.fxml"));
        Parent parent = fxmlLoader.load();
        MainController mainController = fxmlLoader.getController();
        mainController.setStage(stage);
        Scene scene = new Scene(parent);
//        URL cssResource = MyKafkaToolApplication.class.getResource(UIStyleConstant.APP_CSS_FILE);
//        scene.getStylesheets().add(cssResource.toExternalForm());
        userPreferenceManager = DAGGER_APP_COMPONENT.userPreferenceManager();
        applyThemeFromCurrentUserPreference(scene);
        stage.setTitle(AppConstant.APP_NAME);
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(event -> {
            exit();
        });
        if (Modular.isManaged(this)) {
            List<String> params = getParameters().getUnnamed();
            ModularContext.notifyModuleReady(params.get(params.size() - 1));
        }
    }

    public static void applyTheme(Scene scene, Theme theme) {
        javafx.application.Application.setUserAgentStylesheet(theme.getUserAgentStyleSheet());
        theme.getStyleSheets().forEach(styleSheet -> {
            URL cssResource = MyKafkaToolApplication.class.getResource(styleSheet);
            scene.getStylesheets().add(cssResource.toExternalForm());
        });
        try {
            userPreferenceManager.changeUserPreferenceTheme(theme);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        themeProperty.set(theme);
    }

    public static void applyThemeFromCurrentUserPreference(Scene scene) {
        UserPreference userPreference = userPreferenceManager.loadUserPreference();
        Theme theme = userPreference.theme() != null ? userPreference.theme() : Theme.LIGHT;
        applyTheme(scene, theme);
    }


    public static void changeTheme(Scene scene, Theme newTheme) {
        Theme currentTheme = userPreferenceManager.loadUserPreference().theme();
        if (currentTheme == null) currentTheme = Theme.LIGHT;
        if (newTheme != currentTheme) {
            currentTheme.getStyleSheets().forEach(styleSheet -> {
                URL cssResource = MyKafkaToolApplication.class.getResource(styleSheet);
                scene.getStylesheets().remove(cssResource.toExternalForm());
            });
            applyTheme(scene, newTheme);
        }
    }

    public static Theme getCurrentTheme() {
        return userPreferenceManager.loadUserPreference().theme();
    }
    public static void exit() {
        Platform.exit();
    }

    @Override
    public void stop() {
        log.info("App is closing");
//        System.exit(0);
    }

    public static void main(String[] args) {
        if (Modular.isManaged(MyKafkaToolLauncher.class)) {
            String moduleName = ModularContext.getCurrentModuleName();
            launch(ArrayUtils.add(args, moduleName));
        } else {
            launch(args);
        }
    }

    public static String getLogsPath() {
        String userHome = System.getProperty("user.home");
        return MessageFormat.format("{0}/{1}/logs/{2}.log", userHome, APP_NAME, APP_NAME);
    }
}