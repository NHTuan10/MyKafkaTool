package io.github.nhtuan10.mykafkatool.userpreference;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

public interface UserPreferenceRepo {
    UserPreference loadUserPreference() throws IOException;

    void saveUserPreference(UserPreference userPreference) throws IOException;

    String getUserPrefFilePath();

    UserPreference parseUserPreference(String data) throws JsonProcessingException;
}
