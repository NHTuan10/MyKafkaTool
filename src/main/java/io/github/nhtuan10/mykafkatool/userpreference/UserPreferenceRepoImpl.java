package io.github.nhtuan10.mykafkatool.userpreference;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.configuration.annotation.SharedObjectMapper;
import jakarta.inject.Inject;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.github.nhtuan10.mykafkatool.constant.AppConstant.APP_NAME;
import static io.github.nhtuan10.mykafkatool.constant.AppConstant.USER_PREF_FILENAME;

@AppScoped
public class UserPreferenceRepoImpl implements UserPreferenceRepo {
    @Getter
    private final String userPrefFilePath = getDefaultUserPreferenceFilePath();
    //    private final String filePath;
    private final ObjectMapper objectMapper;

    Lock lock = new ReentrantLock();

    @Inject
    public UserPreferenceRepoImpl(@SharedObjectMapper ObjectMapper objectMapper) {
//        this.filePath = filePath;
        this.objectMapper = objectMapper;
//        this.objectMapper = Utils.contructObjectMapper();
    }

    @Override
    public UserPreference loadUserPreference() throws IOException {
//        String data = Files.asCharSource(new File(filePath), StandardCharsets.UTF_8).read();
        String data = Files.readString(Paths.get(userPrefFilePath));
        return objectMapper.readValue(data, UserPreference.class);
    }

    @Override
    public void saveUserPreference(UserPreference userPreference) throws IOException {
        lock.lock();
        try {
//        Files.createParentDirs(new File(filePath));
            Files.createDirectories(Paths.get(userPrefFilePath).getParent());
            String data = objectMapper.writeValueAsString(userPreference);
//        Files.write(data.getBytes(StandardCharsets.UTF_8), new File(filePath));
            Files.writeString(Paths.get(userPrefFilePath), data);
        } finally {
            lock.unlock();
        }
    }

    public static String getDefaultUserPreferenceFilePath() {
        String userHome = System.getProperty("user.home");
        return MessageFormat.format("{0}/{1}/config/{2}", userHome, APP_NAME, USER_PREF_FILENAME);
    }

}
