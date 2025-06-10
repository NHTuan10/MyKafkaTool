package io.github.nhtuan10.mykafkatool.dagger;

import dagger.Component;
import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageViewController;
import javafx.fxml.FXMLLoader;

import java.net.URL;
import java.util.Objects;
import java.util.function.Function;

@AppScoped
@Component(modules = {AppModule.class, ControllerModule.class})
public interface AppComponent {
    Function<URL, FXMLLoader> fxmlLoaderFactory();

    KafkaMessageViewController kafkaMessageViewController();

    abstract ClusterManager clusterManager();

    default FXMLLoader loader(URL fxmlUrl) {
        return fxmlLoaderFactory().apply(Objects.requireNonNull(fxmlUrl));
    }
}
