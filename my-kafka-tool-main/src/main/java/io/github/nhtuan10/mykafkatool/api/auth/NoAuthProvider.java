package io.github.nhtuan10.mykafkatool.api.auth;

import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class NoAuthProvider implements AuthProvider {

    @Override
    public String getName() {
        return NO_AUTH;
    }

    @Override
    public AuthConfig fromConfigText(String configText) {
        return new AuthConfig(getName(), new HashMap<>(), null);
    }

    @Override
    public String toConfigText(AuthConfig authConfig) {
        return "";
    }

    @Override
    public String toString() {
        return getName();
    }
}
