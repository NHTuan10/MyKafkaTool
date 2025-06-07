package io.github.nhtuan10.mykafkatool.api.auth;

public interface AuthProvider {
    String NO_AUTH = "No Auth";

    String getName();

    AuthConfig fromConfigText(String configText) throws Exception;

    String toConfigText(AuthConfig authConfig) throws Exception;

    String toString();
}
