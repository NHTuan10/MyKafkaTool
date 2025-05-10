package com.example.mytool.dao;

import com.example.mytool.model.preference.UserPreference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UserPreferenceDao {
    private String filePath;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public UserPreferenceDao(String filePath) {
        this.filePath = filePath;
    }

    public UserPreference loadUserPreference() throws IOException {
//        String data = Files.asCharSource(new File(filePath), StandardCharsets.UTF_8).read();
        String data = Files.readString(Paths.get(filePath));
        UserPreference userPreference = objectMapper.readValue(data, UserPreference.class);
        return userPreference;
    }

    public synchronized void saveUserPreference(UserPreference userPreference) throws IOException {
//        Files.createParentDirs(new File(filePath));
        Files.createDirectories(Paths.get(filePath).getParent());
        String data = objectMapper.writeValueAsString(userPreference);
//        Files.write(data.getBytes(StandardCharsets.UTF_8), new File(filePath));
        Files.writeString(Paths.get(filePath), data);
    }


}
