package io.github.nhtuan10.mykafkatool.api;

import java.util.HashMap;

public class NoAuthProvider implements AuthProvider {

    @Override
    public String getName() {
        return NO_AUTH;
    }

    @Override
    public AuthConfig fromConfigText(String configText) {
        return new AuthConfig(getName(), new HashMap<>(), new HashMap<>());
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
