package com.example.mytool.dao;

import com.example.mytool.model.preference.UserPreference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class UserPreferenceDao {
    private String filePath;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public UserPreferenceDao(String filePath) {
        this.filePath = filePath;
    }

    public UserPreference loadUserPreference() throws IOException {
        String data = Files.asCharSource(new File(filePath), StandardCharsets.UTF_8).read();
        return objectMapper.readValue(data, UserPreference.class);
    }

    public synchronized void saveUserPreference(UserPreference userPreference) throws IOException {
        Files.createParentDirs(new File(filePath));
        String data = objectMapper.writeValueAsString(userPreference);
        Files.write(data.getBytes(StandardCharsets.UTF_8), new File(filePath));
    }


}
