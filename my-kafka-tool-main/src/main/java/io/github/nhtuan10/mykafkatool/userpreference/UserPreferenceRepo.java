package io.github.nhtuan10.mykafkatool.userpreference;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.text.MessageFormat;

import static io.github.nhtuan10.mykafkatool.constant.AppConstant.APP_NAME;

public interface UserPreferenceRepo {
    UserPreference loadUserPreference() throws IOException;

    void saveUserPreference(UserPreference userPreference) throws IOException;

    String getUserPrefFilePath();

    UserPreference parseUserPreference(String data) throws JsonProcessingException;

    public static String getDefaultUserPrefDir() {
        String userHome = System.getProperty("user.home");
        return MessageFormat.format("{0}/{1}/config", userHome, APP_NAME);
    }
}
