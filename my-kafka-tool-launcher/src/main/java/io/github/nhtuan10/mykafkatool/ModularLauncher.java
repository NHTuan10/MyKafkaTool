package io.github.nhtuan10.mykafkatool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.nhtuan10.modular.api.Modular;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.lang3.StringUtils;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenArtifactInfo;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ModularLauncher {
    public static final String ARTIFACT = "io.github.nhtuan10:my-kafka-tool-main";
    public static final String MINIMUM_VERSION = "0.1.0-SNAPSHOT";
    public static final String VERSION_PROP_KEY = "main.artifact.version";
    public static final String MAVEN_METADATA_URL_PROP_KEY = "main.artifact.maven-metadata-url";
    public static final String MAIN_ARTIFACT_DIRECTORY_PROP_KEY = "main.artifact.directory";
    public static final String MAIN_ARTIFACT_DOWNLOAD_URL_PROP_KEY = "main.artifact.download-url";
    public static final String MAIN_ARTIFACT_FILE_NAME_PREFIX = "main.artifact.fileName-prefix";
    public static final String MAVEN_SNAPSHOT_METADATA_URL_PROP_KEY = "main.artifact.snapshot-maven-metadata-url";
    public static final String ARCHIVE_FORMAT = "zip";
    public static final String APP_NAME = "MyKafkaTool";
    private static AtomicBoolean shouldUpgrade = new AtomicBoolean(false);
    private static final CountDownLatch waitForUpgrade = new CountDownLatch(1);
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    //    private static CountDownLatch waitForConfirmation = new CountDownLatch(1);
    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        String userHome = System.getProperty("user.home");
//        String configLocation = MessageFormat.format("{0}/{1}/config/{2}", userHome, "MyKafkaTool", "config.properties");
        String configLocation = "config.properties";
//        Files.createDirectories(Paths.get(configLocation).getParent());
        Properties properties = new Properties();
        String installedVer = MINIMUM_VERSION;
        try (InputStream is = new FileInputStream(configLocation)) {
            properties.load(is);
            installedVer = Optional.ofNullable(properties.getProperty(VERSION_PROP_KEY)).orElse(MINIMUM_VERSION);
        } catch (Exception e) {
            log.error("Cannot load config.properties file", e);
        }
        String newVersion = installedVer;
        try {
            newVersion = getLatestVersionFromMaven(properties);
        } catch (IOException | InterruptedException e) {
            log.error("Cannot get latest version from maven", e);
        }
//        Optional<String> locationOptional = Optional.ofNullable();
        String uri = Paths
                .get((properties.getProperty(MAIN_ARTIFACT_DIRECTORY_PROP_KEY) + "/" + properties.get(MAIN_ARTIFACT_FILE_NAME_PREFIX) + ".jar").replace("${version}", installedVer))
                .toUri().toString();
//        if (locationOptional.isPresent()) {
//            uri = Paths.get(locationOptional.get().replace("${version}", installedVer)).toUri().toString();
//        } else {
//            uri = getJarLocationUri(installedVer, properties);
//        }
        String versionToUpgrade = installedVer;
        if (newVersion.compareTo(installedVer) > 0) {
            boolean agreeToUpgrade = showDialog(newVersion);
//            UpgradeDialog.main(new String[]{newVersion});
            if (agreeToUpgrade) {
                versionToUpgrade = newVersion;
                try {
                    uri = getJarLocationUri(versionToUpgrade, properties);
                } catch (URISyntaxException | IOException | InterruptedException e) {
                    waitForUpgrade.countDown();
                    log.error("Cannot get jar location uri for the new version {}", versionToUpgrade, e);
                    versionToUpgrade = installedVer;
                    JOptionPane.showMessageDialog(null, "Cannot upgrade to the new version %s, fallback to the current version %s. Please check the logs at %s/%s/logs for details".formatted(newVersion, installedVer, userHome, APP_NAME), "Upgrade Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        try (OutputStream os = new FileOutputStream(configLocation)) {
            properties.setProperty(VERSION_PROP_KEY, versionToUpgrade);
            properties.store(os, "Global MyKafkaTool Properties");
        } catch (IOException e) {
            System.err.println("Failed to save config.properties file");
        }
        Modular.startModuleSyncWithMainClass("my-kafka-tool", List.of(uri), "io.github.nhtuan10.mykafkatool.MyKafkaToolApplication", List.of(""));
//        moduleLoader.startModuleSyncWithMainClass("my-kafka-tool", "http://localhost:8080/my-kafka-tool-main-%s-jar-with-dependencies.jar".formatted(versionToUpgrade), "io.github.nhtuan10.mykafkatool.MyKafkaToolApplication", "");
        waitForUpgrade.countDown();

        //        moduleLoader.startModuleSyncWithMainClass("my-kafka-tool", "mvn://io.github.nhtuan10/my-kafka-tool-main/0.1.0-SNAPSHOT", "io.github.nhtuan10.mykafkatool.MyKafkaToolLauncher", "*");

    }

    private static String getJarLocationUri(String version, Properties properties) throws URISyntaxException, IOException, InterruptedException {
        if (properties.get(MAIN_ARTIFACT_DOWNLOAD_URL_PROP_KEY) != null) {
            String zipFileVersion = version;
            if (isSnapShotVersion(version)) {
                // TODO: snapshot may have maven-metadata file, so need to handle it
                String metadata = properties.getProperty(MAVEN_SNAPSHOT_METADATA_URL_PROP_KEY).replace("${version}", version);
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(metadata)).GET().build();
                String res = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
                XmlMapper xmlMapper = new XmlMapper();
                JsonNode node = xmlMapper.readTree(res);
                for (var n : node.at("/versioning/snapshotVersions/snapshotVersion")) {
                    if (n.at("/extension").asText().equals(ARCHIVE_FORMAT)) {
                        zipFileVersion = n.at("/value").asText();
                        break;
                    }
                }
            }
            String downloadFileName = properties.getProperty(MAIN_ARTIFACT_FILE_NAME_PREFIX).replace("${version}", zipFileVersion) + "." + ARCHIVE_FORMAT;
            String downloadUrl = properties.getProperty(MAIN_ARTIFACT_DOWNLOAD_URL_PROP_KEY) + "/" + version + "/" + downloadFileName;
            Path parentPath = Paths.get(properties.getProperty(MAIN_ARTIFACT_DIRECTORY_PROP_KEY));
            Path zipFilePath = parentPath.resolve(downloadFileName);
            ReadableByteChannel rbc = Channels.newChannel(new URI(downloadUrl).toURL().openStream());
            try (FileOutputStream fos = new FileOutputStream(zipFilePath.toFile())) {
                long size = fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                log.info("Downloaded main zip file with {} bytes from {}", size, downloadUrl);
                try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
                    zipFile.extractAll(parentPath.toAbsolutePath().toString());
                }
            }
            Files.deleteIfExists(zipFilePath);
            return parentPath.resolve(properties.getProperty(MAIN_ARTIFACT_FILE_NAME_PREFIX).replace("${version}", version) + ".jar").toUri().toString();

        } else {
            return "mvn://" + ARTIFACT.replace(":", "/") + "/" + version;
        }
    }

    private static boolean isSnapShotVersion(String version) {
        return version.endsWith("-SNAPSHOT");
    }

    private static String getLatestVersionFromMaven(Properties properties) throws IOException, InterruptedException {
        if (StringUtils.isBlank(properties.getProperty(MAVEN_METADATA_URL_PROP_KEY))) {
            return Arrays.stream(Maven.resolver()
                            .resolve(getMavenLatestVersionQuery()).withoutTransitivity().asResolvedArtifact())
                    .map(MavenArtifactInfo::getCoordinate)
                    .filter(artifact -> ARTIFACT.equals(artifact.getGroupId() + ":" + artifact.getArtifactId()))
                    .findFirst().map(MavenCoordinate::getVersion).orElse(MINIMUM_VERSION);
        } else {
            //TODO: replace hard-code with logic to parse maven-metadata files
            String metadata = properties.getProperty(MAVEN_METADATA_URL_PROP_KEY);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(metadata)).GET().build();
            String res = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            XmlMapper xmlMapper = new XmlMapper();
            JsonNode node = xmlMapper.readTree(res);
            List<String> versions = new ArrayList<>();
            JsonNode versionNode = node.at("/versioning/versions/version");
            if (versionNode.isArray()) {
                for (var n : versionNode) {
                    versions.add(n.asText());
                }
            } else if (versionNode.isTextual()) {
                versions.add(versionNode.asText());
            }
            versions.sort(Comparator.reverseOrder());
            return versions.getFirst() != null ? versions.getFirst() : MINIMUM_VERSION;
        }
    }

//    private static boolean showUpdateConfirmationDialog(String newVersion) {
//        ButtonType yesBtn = new ButtonType("Yes", ButtonBar.ButtonData.YES);
//        ButtonType noBtn = new ButtonType("No", ButtonBar.ButtonData.NO);
//        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to upgrade to the new version %s?".formatted(newVersion), yesBtn, noBtn);
//        return alert.showAndWait().filter(response -> response == yesBtn).isPresent();
//    }

    private static String getMavenLatestVersionQuery() {
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