package io.github.nhtuan10.mykafkatool.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.configuration.annotation.SharedPrettyPrintObjectMapper;
import jakarta.inject.Inject;

@AppScoped
public class SaslSslProvider extends SaslProvider {
    public static final String SASL_SSL = "SASL_SSL";

    @Inject
    public SaslSslProvider(@SharedPrettyPrintObjectMapper ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    protected String getSecurityProtocol() {
        return SASL_SSL;
    }

}
