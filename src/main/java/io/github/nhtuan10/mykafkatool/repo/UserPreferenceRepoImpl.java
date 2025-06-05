package io.github.nhtuan10.mykafkatool.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.model.preference.UserPreference;
import io.github.nhtuan10.mykafkatool.ui.util.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UserPreferenceRepoImpl implements UserPreferenceRepo {
    private final String filePath;
    private static final ObjectMapper objectMapper = Utils.contructObjectMapper();

    Lock lock = new ReentrantLock();

    public UserPreferenceRepoImpl(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public UserPreference loadUserPreference() throws IOException {
//        String data = Files.asCharSource(new File(filePath), StandardCharsets.UTF_8).read();
        String data = Files.readString(Paths.get(filePath));
        return objectMapper.readValue(data, UserPreference.class);
    }

    @Override
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
