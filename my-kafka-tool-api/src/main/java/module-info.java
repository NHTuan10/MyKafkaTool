module io.github.nhtuan10.mykafkatool.api {
    requires io.github.nhtuan10.modular;
    requires com.fasterxml.jackson.databind;
    exports io.github.nhtuan10.mykafkatool.api.model;
    exports io.github.nhtuan10.mykafkatool.api.auth;
    exports io.github.nhtuan10.mykafkatool.api.serdes;
    exports io.github.nhtuan10.mykafkatool.api;
    opens io.github.nhtuan10.mykafkatool.api to io.github.nhtuan10.modular;

}