package io.github.nhtuan10.mykafkatool;

import io.github.nhtuan10.modular.api.module.ModuleLoader;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenArtifactInfo;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;


public class ModularLauncher {
    public static final String ARTIFACT = "io.github.nhtuan10:my-kafka-tool-main:jar:jar-with-dependencies";
    public static final String MINIMUM_VERSION = "0.1.0-SNAPSHOT";
    public static final String VERSION_PROP = "version";
    private static AtomicBoolean shouldUpgrade = new AtomicBoolean(false);
    private static CountDownLatch waitForUpgrade = new CountDownLatch(1);

    //    private static CountDownLatch waitForConfirmation = new CountDownLatch(1);
    public static void main(String[] args) throws IOException {
        String userHome = System.getProperty("user.home");
        String configLocation = MessageFormat.format("{0}/{1}/config/{2}", userHome, "MyKafkaTool", "config.properties");
        Files.createDirectories(Paths.get(configLocation).getParent());
        Properties properties = new Properties();
        String installedVer = MINIMUM_VERSION;
        try (InputStream is = new FileInputStream(configLocation)) {
            properties.load(is);
            installedVer = properties.get(VERSION_PROP).toString();
        } catch (Exception e) {
            System.err.println("Cannot load config.properties file");
        }
        String newVersion = Arrays.stream(Maven.resolver()
                        .resolve(getLatestVersionFromMaven()).withoutTransitivity().asResolvedArtifact())
                .map(MavenArtifactInfo::getCoordinate)
                .filter(artifact -> ARTIFACT.equals(artifact.getGroupId() + ":" + artifact.getArtifactId()))
                .findFirst().map(MavenCoordinate::getVersion).orElse(MINIMUM_VERSION);
        String versionToUpgrade = installedVer;
        if (newVersion.compareTo(installedVer) > 0) {
            showDialog(newVersion);
//            UpgradeDialog.main(new String[]{newVersion});
            versionToUpgrade = newVersion;
        }

        ModuleLoader moduleLoader = ModuleLoader.getInstance();

        moduleLoader.startModuleSyncWithMainClass("my-kafka-tool", "mvn://" + ARTIFACT.replace(":", "/") + "/" + versionToUpgrade, "io.github.nhtuan10.mykafkatool.MyKafkaToolApplication", "");
//        moduleLoader.startModuleSyncWithMainClass("my-kafka-tool", "http://localhost:8080/my-kafka-tool-main-%s-jar-with-dependencies.jar".formatted(versionToUpgrade), "io.github.nhtuan10.mykafkatool.MyKafkaToolApplication", "");
        waitForUpgrade.countDown();
        try (OutputStream os = new FileOutputStream(configLocation)) {
            properties.setProperty(VERSION_PROP, versionToUpgrade);
            properties.store(os, "Global MyKafkaTool Properties");
        } catch (IOException e) {
            System.err.println("Failed to save config.properties file");
        }

        //        moduleLoader.startModuleSyncWithMainClass("my-kafka-tool", "mvn://io.github.nhtuan10/my-kafka-tool-main/0.1.0-SNAPSHOT", "io.github.nhtuan10.mykafkatool.MyKafkaToolLauncher", "*");

    }

//    private static boolean showUpdateConfirmationDialog(String newVersion) {
//        ButtonType yesBtn = new ButtonType("Yes", ButtonBar.ButtonData.YES);
//        ButtonType noBtn = new ButtonType("No", ButtonBar.ButtonData.NO);
//        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to upgrade to the new version %s?".formatted(newVersion), yesBtn, noBtn);
//        return alert.showAndWait().filter(response -> response == yesBtn).isPresent();
//    }

    private static String getLatestVersionFromMaven() {
        return "%s:[%s,)".formatted(ARTIFACT, MINIMUM_VERSION);
    }

    public static boolean showDialog(String version) {
        // Ensure GUI operations run on Event Dispatch Thread
//        SwingUtilities.invokeLater(() -> {
        int result = JOptionPane.showConfirmDialog(
                null,
                "There is a new version available. Do you want to upgrade to the new version %s?".formatted(version),
                "Upgrade Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
//                shouldUpgrade.set(true);
//                waitForConfirmation.countDown();
            // Create and show progress dialog
            Thread t = new Thread(() -> {
                JDialog progressDialog = new JDialog();
                progressDialog.setTitle("Processing...");
                progressDialog.setSize(400, 100);
                progressDialog.setLocationRelativeTo(null);
                progressDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

                // Create progress indicator
                JProgressBar progressBar = new JProgressBar();
                progressBar.setIndeterminate(true); // Makes it spin continuously

                // Add components to dialog
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(progressBar, BorderLayout.CENTER);
                panel.add(new JLabel("Please wait...", SwingConstants.CENTER), BorderLayout.NORTH);
                panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                progressDialog.add(panel);
                progressDialog.setVisible(true);
                try {
                    waitForUpgrade.await();
                    progressDialog.dispose();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            t.setDaemon(true);
            t.start();
            return true;
            // You can add your next processing steps here
            // For example:
            // new Thread(() -> {
            //     // Do your processing here
            //     // When done:
            //     // progressDialog.dispose();
            // }).start();

        } else {
//                waitForConfirmation.countDown();
            // Handle 'No' option
            System.out.println("Operation cancelled");
            // Add your next code here for 'No' option
            return false;
        }
//        });
    }


//        public static class UpgradeDialog extends Application {
//        @Override
//        public void start(Stage stage) throws Exception {
//            String newVersion = this.getParameters().getUnnamed().get(0);
//            if (showUpdateConfirmationDialog(newVersion)){
//                shouldUpgrade.set(true);
//                Stage progressStage = new Stage();
//                ProgressIndicator progressIndicator = new ProgressIndicator();
//                progressIndicator.setMaxSize(100, 100);
//
//                VBox vbox = new VBox(progressIndicator);
//                vbox.setAlignment(Pos.CENTER);
//                vbox.setMinSize(200, 200);
//
//                Scene scene = new Scene(vbox);
//                progressStage.setScene(scene);
//                progressStage.setTitle("Processing...");
//                progressStage.show();
////                waitForUpgrade.await();
//
//                Platform.exit();
//            }
//        }
//
//        public static void main(String[] args) {
//            launch(args);
//        }
//
//
//    }
}