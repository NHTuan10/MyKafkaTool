package io.github.nhtuan10.mykafkatool.ui.util;

import io.github.nhtuan10.mykafkatool.MyKafkaToolApplication;
import io.github.nhtuan10.mykafkatool.configuration.AppComponent;
import io.github.nhtuan10.mykafkatool.ui.controller.ModalController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Cleanup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ModalUtils {
    public static boolean confirmAlert(String title, String text, String okDoneText, String cancelCloseText) {
        ButtonType yes = new ButtonType(okDoneText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(cancelCloseText, ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, yes, cancel);
        alert.setTitle(title);
        Optional<ButtonType> result = alert.showAndWait();

        return result.orElse(cancel) == yes;
    }

    public static void showPopUpModal(String modalFxml, String title, AtomicReference<Object> modelRef, final Map<String, Object> inputVarMap, Window parentWindow) throws IOException {
        showPopUpModal(modalFxml, title, modelRef, inputVarMap, true, false, parentWindow, true);
    }

    public static void showPopUpModal(final String modalFxml, final String title, final AtomicReference<Object> modelRef, final Map<String, Object> inputVarMap, final boolean editable, final boolean resizable, Window parentWindow, final boolean showAndWait) throws IOException {
        Stage stage = new Stage();
        AppComponent appComponent = MyKafkaToolApplication.DAGGER_APP_COMPONENT;
        FXMLLoader modalLoader = appComponent.loader(MyKafkaToolApplication.class.getResource(modalFxml));
//        FXMLLoader modalLoader = new FXMLLoader(
//                MyKafkaToolApplication.class.getResource(modalFxml));
        Scene scene = new Scene(modalLoader.load());
        MyKafkaToolApplication.applyThemeFromCurrentUserPreference(scene);

        ModalController modalController = modalLoader.getController();
        modalController.setModelRef(modelRef);
        modalController.setFields(modalController, stage, inputVarMap);
        modalController.launch(editable);
        stage.setTitle(title);
        if (editable) {
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(parentWindow);
        }
        stage.setResizable(resizable);
        stage.setScene(scene);
//        URL cssResource = MyKafkaToolApplication.class.getResource(UIStyleConstant.LIGHT_STYLE_CSS_FILE);
//        scene.getStylesheets().add(cssResource.toExternalForm());
        if (showAndWait) {
            stage.showAndWait();
        } else {
            stage.show();
        }
    }

    public static void showAlertDialog(Alert.AlertType alertType, String text, String title, ButtonType... buttonTypes) {
        Alert alert = new Alert(alertType, text, buttonTypes);
        if (title != null) {
            alert.setTitle(title);
        }

        alert.showAndWait();
    }

    public static Alert buildHelpDialog(String resourceFileName, String title) throws IOException {
        @Cleanup
        InputStream is = ModalUtils.class.getClassLoader().getResourceAsStream(resourceFileName);
        assert is != null;
        String handlebarsHelp = new String(is.readAllBytes());
        TextArea textArea = new TextArea(handlebarsHelp);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setWrapText(true);
        AnchorPane anchorPane = new AnchorPane();
        AnchorPane.setTopAnchor(textArea, 0.0);
        AnchorPane.setBottomAnchor(textArea, 0.0);
        AnchorPane.setLeftAnchor(textArea, 0.0);
        AnchorPane.setRightAnchor(textArea, 0.0);

//        GridPane gridPane = new GridPane();
//        gridPane.setMaxWidth(Double.MAX_VALUE);
//        gridPane.add(textArea, 0, 0);
//        gridPane.
        anchorPane.getChildren().add(textArea);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.getDialogPane().setContent(anchorPane);
        alert.setResizable(true);
        alert.initModality(Modality.WINDOW_MODAL);
        return alert;
    }
}
