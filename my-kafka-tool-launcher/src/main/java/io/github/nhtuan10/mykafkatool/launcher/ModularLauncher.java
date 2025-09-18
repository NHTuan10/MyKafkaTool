package io.github.nhtuan10.mykafkatool.launcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.nhtuan10.modular.api.Modular;
import io.github.nhtuan10.modular.api.module.ModuleLoadConfiguration;
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
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ModularLauncher {
    public static final String ARTIFACT = "io.github.nhtuan10:my-kafka-tool-main";

    public static final String MINIMUM_VERSION = "0.1.1.7-SNAPSHOT";
    public static final String ARTIFACT_URI_PROP_KEY = "artifact.override.uri";
    public static final String ARTIFACT_VERSION_PROP_KEY = "artifact.version";
    public static final String MAVEN_METADATA_FILE_NAME_PROP_KEY = "artifact.maven-metadata-fileName";
    public static final String ARTIFACT_DIRECTORY_PROP_KEY = "artifact.directory";
    public static final String ARTIFACT_DOWNLOAD_URL_PROP_KEY = "artifact.download-url";
    public static final String ARTIFACT_FILE_NAME_PREFIX = "artifact.fileName-prefix";
    public static final String MAVEN_SNAPSHOT_METADATA_FILE_NAME_PROP_KEY = "artifact.snapshot-maven-metadata-fileName";
    public static final String IS_DEPLOYED = "isDeployed";
    public static final String ARCHIVE_FORMAT = "zip";
    public static final String APP_NAME = "MyKafkaTool";
    public static final String VERSION_PLACEHOLDER = "${version}";
    public static final String ARTIFACT_NAME_PLACEHOLDER = "${artifact-name}";
    public static final String MAIN_ARTIFACT_NAME = "main";
    public static final String EXT_ARTIFACT_NAME = "ext";
    public static final String MAVEN_HTTP_TIMEOUT = "maven.http.timeout";

    private static AtomicBoolean shouldUpgrade = new AtomicBoolean(false);
    private static final CountDownLatch waitForUpgrade = new CountDownLatch(1);
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    //    private static CountDownLatch waitForConfirmation = new CountDownLatch(1);
    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        String userHome = System.getProperty("user.home");
//        String configLocation = MessageFormat.format("{0}/{1}/config/{2}", userHome, "MyKafkaTool", "config.properties");
        String configLocation = getArtifactDirectory(new Properties()) + "/config.properties";
//        Files.createDirectories(Paths.get(configLocation).getParent());
        Properties properties = new Properties();
        String installedVer = MINIMUM_VERSION;
        try (InputStream is = new FileInputStream(configLocation)) {
            properties.load(is);
            installedVer = Optional.ofNullable(properties.getProperty(ARTIFACT_VERSION_PROP_KEY)).orElse(MINIMUM_VERSION);
        } catch (Exception e) {
            log.error("Cannot load config.properties file", e);
        }

        boolean isUpgraded = false;
        Map<String, String> uris = new HashMap<>();
        String newVersion = installedVer;
        try {
            newVersion = getLatestVersionFromMaven(MAIN_ARTIFACT_NAME, properties);
        } catch (IOException | InterruptedException e) {
            log.error("Cannot get latest version from maven", e);
        }
        String versionToUpgrade = installedVer;
        if (newVersion.compareTo(installedVer) > 0) {

            boolean agreeToUpgrade = showDialog(newVersion);
//            UpgradeDialog.main(new String[]{newVersion});
            if (agreeToUpgrade) {
                versionToUpgrade = newVersion;
                try {
                    for (String artifactName : List.of(MAIN_ARTIFACT_NAME, EXT_ARTIFACT_NAME)) {
                        String uri = getJarLocationUri(artifactName, versionToUpgrade, properties);
                        uris.put(artifactName, uri);
                    }
                    isUpgraded = true;
                } catch (URISyntaxException | IOException | InterruptedException e) {
                    waitForUpgrade.countDown();
                    log.error("Cannot get jar location uri for the new version {}", versionToUpgrade, e);
                    versionToUpgrade = installedVer;
                    JOptionPane.showMessageDialog(null, "Cannot upgrade to the new version %s, fallback to the current version %s. Please check the logs at %s/%s/logs for details".formatted(newVersion, installedVer, userHome, APP_NAME), "Upgrade Error", JOptionPane.ERROR_MESSAGE);
                    isUpgraded = false;
                }
            } else {
                isUpgraded = false;
            }
        } else {
            isUpgraded = false;
        }
        if (!isUpgraded) {
            uris.clear();
            for (String artifactName : List.of(MAIN_ARTIFACT_NAME, EXT_ARTIFACT_NAME)) {
                String uri;
                Optional<String> uriFromPropertyFileOptional = Optional.ofNullable(properties.getProperty(ARTIFACT_URI_PROP_KEY));

                if (uriFromPropertyFileOptional.isPresent()) {
                    uri = uriFromPropertyFileOptional.get().replace(VERSION_PLACEHOLDER, installedVer).replace(ARTIFACT_NAME_PLACEHOLDER, artifactName);
                } else {
                    uri = Paths
                            .get((getArtifactDirectory(properties) + "/" + properties.get(ARTIFACT_FILE_NAME_PREFIX) + ".jar")
                                    .replace(VERSION_PLACEHOLDER, installedVer).replace(ARTIFACT_NAME_PLACEHOLDER, artifactName))
                            .toUri().toString();
                }
                uris.put(artifactName, uri);
            }
        }

        try (OutputStream os = new FileOutputStream(configLocation)) {
            properties.setProperty(ARTIFACT_VERSION_PROP_KEY, versionToUpgrade);
            properties.store(os, "Global MyKafkaTool Properties");
        } catch (IOException e) {
            System.err.println("Failed to save config.properties file");
        }
        Modular.startModuleSync("my-kafka-tool-ext",
                ModuleLoadConfiguration.builder()
                        .locationUris(List.of(uris.get(EXT_ARTIFACT_NAME)))
                        .packagesToScan(List.of("*"))
                        .modularClassLoaderName("main-class-loader")
                        .build());
        Modular.startModuleSync("my-kafka-tool-main",
                ModuleLoadConfiguration.builder()
                        .locationUris(List.of(uris.get(MAIN_ARTIFACT_NAME)))
                        .packagesToScan(List.of("*"))
                        .modularClassLoaderName("main-class-loader")
                        .mainClass("io.github.nhtuan10.mykafkatool.MyKafkaToolApplication")
                        .build());
//        Modular.startModuleSyncWithMainClass("my-kafka-tool-main", uris, "io.github.nhtuan10.mykafkatool.MyKafkaToolApplication", List.of("*"));
//        Modular.startModuleSync("my-kafka-tool-ext", List.of("mvn://io.github.nhtuan10/my-kafka-tool-ext/0.1.1.7-SNAPSHOT"), List.of("io.github.nhtuan10.mykafkatool.ext"));
//        Modular.startModuleSync("my-kafka-tool-ext", List.of("file:///Users/tuan/Library/CloudStorage/OneDrive-Personal/CS/Java/MyKafkaTool/my-kafka-tool-launcher/target/my-kafka-tool-launcher-0.1.1.7-SNAPSHOT/lib/my-kafka-tool-ext-0.1.1.7-SNAPSHOT.jar"), List.of("io.github.nhtuan10.mykafkatool.ext"));
//        moduleLoader.startModuleSyncWithMainClass("my-kafka-tool", "http://localhost:8080/my-kafka-tool-main-%s-jar-with-dependencies.jar".formatted(versionToUpgrade), "io.github.nhtuan10.mykafkatool.MyKafkaToolApplication", "");
        waitForUpgrade.countDown();
        long endTime = System.currentTimeMillis();
        log.info("App start time: {} ms", endTime - startTime);

        //        moduleLoader.startModuleSyncWithMainClass("my-kafka-tool", "mvn://io.github.nhtuan10/my-kafka-tool-main/0.1.0-SNAPSHOT", "io.github.nhtuan10.mykafkatool.MyKafkaToolLauncher", "*");

    }

    private static String getArtifactDirectory(Properties properties) {
        boolean isDeployed = Boolean.parseBoolean(getPropertyValue(IS_DEPLOYED, properties, "true"));
        String dir = getPropertyValue(ARTIFACT_DIRECTORY_PROP_KEY, properties, ".");
        if (isDeployed) {
           dir = new File(URLDecoder.decode(ModularLauncher.class.getProtectionDomain().getCodeSource().getLocation().getPath(), StandardCharsets.UTF_8)).getParentFile().toPath().resolve(dir).toString();
        }
        return dir;
    }

    private static String getPropertyValue(String key, Properties properties) {
        return getPropertyValue(key, properties, null);
    }

    private static String getPropertyValue(String key, Properties properties, String defaultValue) {
        return Optional.ofNullable(System.getProperty(key))
                .orElse(Optional.ofNullable(properties.getProperty(key)).orElse(defaultValue));
    }
    private static String getJarLocationUri(String artifactName, String version, Properties properties) throws URISyntaxException, IOException, InterruptedException {
        if (properties.get(ARTIFACT_DOWNLOAD_URL_PROP_KEY) != null) {
            String zipFileVersion = version;
            if (isSnapShotVersion(version)) {
                String metadata = properties.getProperty(ARTIFACT_DOWNLOAD_URL_PROP_KEY).replace(ARTIFACT_NAME_PLACEHOLDER, artifactName) + "/"
                        + properties.getProperty(MAVEN_SNAPSHOT_METADATA_FILE_NAME_PROP_KEY).replace(VERSION_PLACEHOLDER, version);
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(metadata)).GET().timeout(getMavenHttpTimeout(properties)).build();
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
            String downloadFileName = properties.getProperty(ARTIFACT_FILE_NAME_PREFIX).replace(VERSION_PLACEHOLDER, zipFileVersion).replace(ARTIFACT_NAME_PLACEHOLDER, artifactName) + "." + ARCHIVE_FORMAT;
            String downloadUrl = properties.getProperty(ARTIFACT_DOWNLOAD_URL_PROP_KEY).replace(ARTIFACT_NAME_PLACEHOLDER, artifactName) + "/" + version + "/" + downloadFileName;
            Path parentPath = Paths.get(getArtifactDirectory(properties));
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
            return parentPath.resolve(properties.getProperty(ARTIFACT_FILE_NAME_PREFIX).replace(ARTIFACT_NAME_PLACEHOLDER, artifactName).replace(VERSION_PLACEHOLDER, version) + ".jar").toUri().toString();

        } else {
            return "mvn://" + ARTIFACT.replace(":", "/") + "/" + version;
        }
    }

    private static Duration getMavenHttpTimeout(Properties properties) {
        Duration timeout = Duration.ofSeconds(Long.parseLong(getPropertyValue(MAVEN_HTTP_TIMEOUT, properties)));
        return timeout;
    }

    private static boolean isSnapShotVersion(String version) {
        return version.endsWith("-SNAPSHOT");
    }

    private static String getLatestVersionFromMaven(String artifactName, Properties properties) throws IOException, InterruptedException {
        // TODO: implement checking Github for the latest release files
// Consider using Shrinkwrap maven resolver or not.Alternative way is directly checking maven metadata files
        if (StringUtils.isBlank(properties.getProperty(ARTIFACT_DOWNLOAD_URL_PROP_KEY))) {
            return Arrays.stream(Maven.resolver()
                            .resolve(getMavenLatestVersionQuery()).withoutTransitivity().asResolvedArtifact())
                    .map(MavenArtifactInfo::getCoordinate)
                    .filter(artifact -> ARTIFACT.equals(artifact.getGroupId() + ":" + artifact.getArtifactId()))
                    .findFirst().map(MavenCoordinate::getVersion).orElse(MINIMUM_VERSION);
        } else {
            String metadata = properties.getProperty(ARTIFACT_DOWNLOAD_URL_PROP_KEY).replace(ARTIFACT_NAME_PLACEHOLDER, artifactName) + "/" + properties.getProperty(MAVEN_METADATA_FILE_NAME_PROP_KEY);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(metadata)).GET().timeout(getMavenHttpTimeout(properties)).build();
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
            return versions.get(0) != null ? versions.get(0) : MINIMUM_VERSION;
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
            log.info("Users accepted to upgrade to the new version {}. Processing...", version);
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
            log.info("Users declined to upgrade to the new version {}", version);
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