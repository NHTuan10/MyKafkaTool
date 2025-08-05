package io.github.nhtuan10.mykafkatool.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Component;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.configuration.annotation.SharedObjectMapper;
import io.github.nhtuan10.mykafkatool.configuration.annotation.ViewControllerMap;
import io.github.nhtuan10.mykafkatool.configuration.annotation.WindowControllerMap;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.schemaregistry.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.ui.event.EventDispatcher;
import io.github.nhtuan10.mykafkatool.userpreference.UserPreferenceManager;
import javafx.fxml.FXMLLoader;

import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@AppScoped
@Component(modules = {AppModule.class, ControllerModule.class})
public interface AppComponent {
    @WindowControllerMap
    Function<URL, FXMLLoader> fxmlLoaderFactory();

//    KafkaMessageViewController kafkaMessageViewController();

//    SchemaRegistryViewController schemaRegistryController();

    @ViewControllerMap
    Map<Class<?>, Object> viewControllers();

    ClusterManager clusterManager();

    UserPreferenceManager userPreferenceManager();

    SchemaRegistryManager schemaRegistryManager();

    default FXMLLoader loader(URL fxmlUrl) {
        return fxmlLoaderFactory().apply(Objects.requireNonNull(fxmlUrl));
    }

    @SharedObjectMapper
    ObjectMapper sharedObjectMapper();

    EventDispatcher eventDispatcher();
}
