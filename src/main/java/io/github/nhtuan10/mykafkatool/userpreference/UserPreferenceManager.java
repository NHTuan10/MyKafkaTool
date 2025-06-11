package io.github.nhtuan10.mykafkatool.userpreference;

import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.constant.Theme;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@AppScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UserPreferenceManager {
    private final UserPreferenceRepo userPreferenceRepo;
    private final Lock lock = new ReentrantLock();

    public void saveUserPreference(UserPreference userPreference) throws IOException {
        userPreferenceRepo.saveUserPreference(userPreference);
    }

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

    public void addClusterToUserPreference(KafkaCluster cluster) throws IOException {
        lock.lock();
        try {
            UserPreference userPreference = loadUserPreference();
            userPreference.connections().add(cluster);
            saveUserPreference(userPreference);
        } finally {
            lock.unlock();
        }
    }

    public void removeClusterFromUserPreference(String clusterName) throws IOException {
        lock.lock();
        try {
            UserPreference userPreference = loadUserPreference();
            userPreference.connections().removeIf(cluster -> cluster.getName().equals(clusterName));
            saveUserPreference(userPreference);
        } finally {
            lock.unlock();
        }
    }

    public String getUserPrefFilePath() {
        return userPreferenceRepo.getUserPrefFilePath();
    }
}
