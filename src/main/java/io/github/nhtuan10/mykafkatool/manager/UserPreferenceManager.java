package io.github.nhtuan10.mykafkatool.manager;

import io.github.nhtuan10.mykafkatool.dao.UserPreferenceDao;
import io.github.nhtuan10.mykafkatool.model.kafka.KafkaCluster;
import io.github.nhtuan10.mykafkatool.model.preference.UserPreference;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.github.nhtuan10.mykafkatool.constant.AppConstant.APP_NAME;
import static io.github.nhtuan10.mykafkatool.constant.AppConstant.USER_PREF_FILENAME;

public class UserPreferenceManager {
    @Getter
    private static final String userPrefFilePath = getDefaultUserPreferenceFilePath();
    private static final UserPreferenceDao userPreferenceDao = new UserPreferenceDao(userPrefFilePath);
    private static final Lock lock = new ReentrantLock();
    public static void saveUserPreference(UserPreference userPreference) throws IOException {
        userPreferenceDao.saveUserPreference(userPreference);
    }

    public static UserPreference loadUserPreference() {
        UserPreference userPreference;
        try {
            userPreference = userPreferenceDao.loadUserPreference();
        } catch (IOException e) {
            userPreference = getDefaultUserPreference();
        }
        return userPreference;
    }

    public static String getDefaultUserPreferenceFilePath() {
        String userHome = System.getProperty("user.home");
        return userHome + "/" + APP_NAME + "/" + USER_PREF_FILENAME;
    }

    public static UserPreference getDefaultUserPreference() {
        return new UserPreference(new ArrayList<>());
    }

    public static void addClusterToUserPreference(KafkaCluster cluster) throws IOException {
        lock.lock();
        try {
            UserPreference userPreference = loadUserPreference();
            userPreference.connections().add(cluster);
            saveUserPreference(userPreference);
        } finally {
            lock.unlock();
        }
    }

    public static void removeClusterFromUserPreference(String clusterName) throws IOException {
        lock.lock();
        try {
            UserPreference userPreference = loadUserPreference();
            userPreference.connections().removeIf(cluster -> cluster.getName().equals(clusterName));
            saveUserPreference(userPreference);
        } finally {
            lock.unlock();
        }
    }
}
