package io.github.nhtuan10.mykafkatool.manager;

import com.google.common.collect.ImmutableMap;
import io.github.nhtuan10.mykafkatool.api.auth.AuthConfig;
import io.github.nhtuan10.mykafkatool.api.auth.AuthProvider;
import io.github.nhtuan10.mykafkatool.api.auth.NoAuthProvider;
import io.github.nhtuan10.mykafkatool.api.auth.SaslProvider;

import java.util.Collection;
import java.util.Map;

public class AuthProviderManager {
    private static final AuthProviderManager instance;
    private final Map<String, AuthProvider> authProviderMap;
    private static final NoAuthProvider noAuthProvider;
    private static final SaslProvider saslProvider;

    static {
        noAuthProvider = new NoAuthProvider();
        saslProvider = new SaslProvider();
        instance = new AuthProviderManager();
    }

    private AuthProviderManager() {
        authProviderMap = ImmutableMap.of(noAuthProvider.getName(), noAuthProvider
                , saslProvider.getName(), saslProvider
        );
    }

    private static AuthProviderManager getInstance() {
        return instance;
    }

    public static AuthProvider getAuthProvider(String name) {
        return getInstance().authProviderMap.get(name);
    }

    public static Collection<AuthProvider> getAllAuthProviders() {
        return getInstance().authProviderMap.values();
    }

    public static Map<String, Object> getKafkaAuthProperties(AuthConfig authConfig) throws Exception {
        return getInstance().authProviderMap.getOrDefault(authConfig.name(), noAuthProvider).getKafkaProperties(authConfig);
    }

    public static AuthProvider getNoAuthProvider() {
        return noAuthProvider;
    }
}
