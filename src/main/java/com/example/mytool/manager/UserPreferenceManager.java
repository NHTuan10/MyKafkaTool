package com.example.mytool.manager;

import com.example.mytool.dao.UserPreferenceDao;
import com.example.mytool.model.kafka.KafkaCluster;
import com.example.mytool.model.preference.UserPreference;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;

import static com.example.mytool.constant.AppConstant.APP_NAME;
import static com.example.mytool.constant.AppConstant.USER_PREF_FILENAME;

public class UserPreferenceManager {
    @Getter
    private static final String userPrefFilePath = getDefaultUserPreferenceFilePath();
    private static final UserPreferenceDao userPreferenceDao = new UserPreferenceDao(userPrefFilePath);

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

    public synchronized static void addClusterToUserPreference(KafkaCluster cluster) throws IOException {
        UserPreference userPreference = loadUserPreference();
        userPreference.connections().add(cluster);
        saveUserPreference(userPreference);
    }

    public synchronized static void removeClusterFromUserPreference(String clusterName) throws IOException {
        UserPreference userPreference = loadUserPreference();
        userPreference.connections().removeIf(cluster -> cluster.getName().equals(clusterName));
        saveUserPreference(userPreference);
    }
}
