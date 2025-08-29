package io.github.nhtuan10.mykafkatool.userpreference;

import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.constant.Theme;
import jakarta.inject.Inject;
import lombok.Locked;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@AppScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UserPreferenceManager {
    private final UserPreferenceRepo userPreferenceRepo;

    @Locked.Write
    public void saveUserPreference(UserPreference userPreference) throws IOException {
        userPreferenceRepo.saveUserPreference(userPreference);
    }

    @Locked.Write
    public UserPreference saveUserPreference(String userPrefString) throws IOException {
        UserPreference userPreference = userPreferenceRepo.parseUserPreference(userPrefString);
        userPreferenceRepo.saveUserPreference(userPreference);
        return userPreference;
    }

    @Locked.Read
    public UserPreference loadUserPreference() {
        UserPreference userPreference;
        try {
            userPreference = userPreferenceRepo.loadUserPreference();
        } catch (IOException e) {
            log.warn("Error when load user preference", e);
            userPreference = getDefaultUserPreference();
        }
        return userPreference;
    }

    public void changeUserPreferenceTheme(Theme newTheme) throws IOException {
        UserPreference userPreference = loadUserPreference();
        saveUserPreference(new UserPreference(userPreference.connections(), newTheme));

    }

    public UserPreference getDefaultUserPreference() {
        return new UserPreference(new ArrayList<>());
    }

    @Locked.Write
    public void addClusterToUserPreference(KafkaCluster cluster) throws IOException {
        UserPreference userPreference = loadUserPreference();
        userPreference.connections().add(cluster);
        saveUserPreference(userPreference);
    }

    @Locked.Write
    public void updateClusterToUserPreference(KafkaCluster oldCluster, KafkaCluster cluster) throws IOException {
        UserPreference userPreference = loadUserPreference();
        userPreference.connections().removeIf(c -> c.getName().equals(oldCluster.getName()));
        userPreference.connections().add(cluster);
        saveUserPreference(userPreference);
    }

    @Locked.Write
    public void removeClusterFromUserPreference(String clusterName) throws IOException {
        UserPreference userPreference = loadUserPreference();
        userPreference.connections().removeIf(cluster -> cluster.getName().equals(clusterName));
        saveUserPreference(userPreference);

    }

    public String getUserPrefFilePath() {
        return userPreferenceRepo.getUserPrefFilePath();
    }
}
