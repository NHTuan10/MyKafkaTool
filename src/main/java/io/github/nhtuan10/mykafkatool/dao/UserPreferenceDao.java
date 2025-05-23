package io.github.nhtuan10.mykafkatool.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.model.preference.UserPreference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UserPreferenceDao {
    private final String filePath;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    Lock lock = new ReentrantLock();
    public UserPreferenceDao(String filePath) {
        this.filePath = filePath;
    }

    public UserPreference loadUserPreference() throws IOException {
//        String data = Files.asCharSource(new File(filePath), StandardCharsets.UTF_8).read();
        String data = Files.readString(Paths.get(filePath));
        return objectMapper.readValue(data, UserPreference.class);
    }

    public void saveUserPreference(UserPreference userPreference) throws IOException {
        lock.lock();
        try {
//        Files.createParentDirs(new File(filePath));
            Files.createDirectories(Paths.get(filePath).getParent());
            String data = objectMapper.writeValueAsString(userPreference);
//        Files.write(data.getBytes(StandardCharsets.UTF_8), new File(filePath));
            Files.writeString(Paths.get(filePath), data);
        } finally {
            lock.unlock();
        }
    }


}
