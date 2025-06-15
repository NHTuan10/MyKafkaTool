package io.github.nhtuan10.mykafkatool.userpreference;

import java.io.IOException;

public interface UserPreferenceRepo {
    UserPreference loadUserPreference() throws IOException;

    void saveUserPreference(UserPreference userPreference) throws IOException;

    String getUserPrefFilePath();
}
