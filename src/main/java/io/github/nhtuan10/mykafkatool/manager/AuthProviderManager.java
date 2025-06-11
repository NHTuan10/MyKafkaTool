package io.github.nhtuan10.mykafkatool.manager;

import io.github.nhtuan10.mykafkatool.api.auth.AuthConfig;
import io.github.nhtuan10.mykafkatool.api.auth.AuthProvider;
import io.github.nhtuan10.mykafkatool.api.auth.NoAuthProvider;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.Map;

@AppScoped
public class AuthProviderManager {
    //    private static final AuthProviderManager instance;
    private final Map<String, AuthProvider> authProviderMap;
    private final NoAuthProvider noAuthProvider;

//    @Inject
//    @Named("noAuthProvider")
//    NoAuthProvider noAuthProvider;
//    private static final SaslProvider saslProvider;

    //    static {
//        noAuthProvider = new NoAuthProvider();
//        saslProvider = new SaslProvider();
//        instance = new AuthProviderManager();
//    }
    @Inject
    public AuthProviderManager(Map<String, AuthProvider> authProviderMap) {
//        authProviderMap = ImmutableMap.of(noAuthProvider.getName(), noAuthProvider
//                , saslProvider.getName(), saslProvider
//        );
        this.authProviderMap = authProviderMap;
        noAuthProvider = (NoAuthProvider) this.authProviderMap.values().stream()
                .filter(p -> p instanceof NoAuthProvider).findFirst().orElse(new NoAuthProvider());
    }

//    private static AuthProviderManager getInstance() {
//        return instance;
//    }

    public AuthProvider getAuthProvider(String name) {
        return this.authProviderMap.get(name);
    }

    public Collection<AuthProvider> getAllAuthProviders() {
        return authProviderMap.values();
    }

    public Map<String, Object> getKafkaAuthProperties(AuthConfig authConfig) throws Exception {
        return authProviderMap.getOrDefault(authConfig.name(), noAuthProvider).getKafkaProperties(authConfig);
    }

    public AuthProvider getNoAuthProvider() {
        return noAuthProvider;
    }
}
