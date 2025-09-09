package io.github.nhtuan10.mykafkatool.userpreference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.configuration.annotation.SharedPrettyPrintObjectMapper;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Locked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;

import static io.github.nhtuan10.mykafkatool.constant.AppConstant.USER_PREF_FILENAME;

@AppScoped
public class UserPreferenceRepoImpl implements UserPreferenceRepo {
    @Getter
    private final String userPrefFilePath = getDefaultUserPreferenceFilePath();
    //    private final String filePath;
    private final ObjectMapper objectMapper;

//    Lock lock = new ReentrantLock();

    @Inject
    public UserPreferenceRepoImpl(@SharedPrettyPrintObjectMapper ObjectMapper objectMapper) {
//        this.filePath = filePath;
        this.objectMapper = objectMapper;
//        this.objectMapper = Utils.contructObjectMapper();
    }

    @Override
    @Locked.Read
    public UserPreference loadUserPreference() throws IOException {
//        String data = Files.asCharSource(new File(filePath), StandardCharsets.UTF_8).read();
        String data = Files.readString(Paths.get(userPrefFilePath));
        return objectMapper.readValue(data, UserPreference.class);
    }

    @Override
    @Locked.Write
    public void saveUserPreference(UserPreference userPreference) throws IOException {
//        lock.lock();
//        try {
//        Files.createParentDirs(new File(filePath));
            Files.createDirectories(Paths.get(userPrefFilePath).getParent());
            String data = objectMapper.writeValueAsString(userPreference);
//        Files.write(data.getBytes(StandardCharsets.UTF_8), new File(filePath));
            Files.writeString(Paths.get(userPrefFilePath), data);
//        } finally {
//            lock.unlock();
//        }
    }

    public static String getDefaultUserPreferenceFilePath() {
        return MessageFormat.format("{0}/{1}", UserPreferenceRepo.getDefaultUserPrefDir(), USER_PREF_FILENAME);
    }

    public UserPreference parseUserPreference(String data) throws JsonProcessingException {
        return objectMapper.readValue(data, UserPreference.class);
    }
}
