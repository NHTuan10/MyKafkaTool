package io.github.nhtuan10.mykafkatool.dao;

import io.github.nhtuan10.mykafkatool.model.preference.UserPreference;

import java.io.IOException;

public interface UserPreferenceRepo {
    UserPreference loadUserPreference() throws IOException;

    void saveUserPreference(UserPreference userPreference) throws IOException;
}
