package io.github.nhtuan10.mykafkatool.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import io.github.nhtuan10.mykafkatool.configuration.annotation.AppScoped;
import io.github.nhtuan10.mykafkatool.configuration.annotation.RichTextFxObjectMapper;
import io.github.nhtuan10.mykafkatool.configuration.annotation.ViewControllerMap;
import io.github.nhtuan10.mykafkatool.configuration.annotation.WindowControllerMap;
import io.github.nhtuan10.mykafkatool.producer.ProducerUtil;
import io.github.nhtuan10.mykafkatool.schemaregistry.SchemaRegistryManager;
import io.github.nhtuan10.mykafkatool.serdes.SerDesHelper;
import io.github.nhtuan10.mykafkatool.ui.codehighlighting.JsonHighlighter;
import io.github.nhtuan10.mykafkatool.ui.controller.AddConnectionModalController;
import io.github.nhtuan10.mykafkatool.ui.controller.AddOrViewMessageModalController;
import io.github.nhtuan10.mykafkatool.ui.controller.MainController;
import io.github.nhtuan10.mykafkatool.ui.event.EventDispatcher;
import io.github.nhtuan10.mykafkatool.ui.messageview.KafkaMessageViewController;
import io.github.nhtuan10.mykafkatool.ui.schemaregistry.SchemaRegistryViewController;
import javafx.fxml.FXMLLoader;
import javafx.util.Callback;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Map;
import java.util.function.Function;

@Module
abstract class ControllerModule {
    @Binds
    @IntoMap
    @ClassKey(MainController.class)
    @WindowControllerMap
    @AppScoped
    abstract Object mainController(MainController controller);

    @Binds
    @IntoMap
    @ClassKey(AddConnectionModalController.class)
    @WindowControllerMap
    @AppScoped
    abstract Object addConnectionModalController(AddConnectionModalController controller);


    //    @IntoMap
//    @ClassKey(AddOrViewMessageModalController.class)
//    @WindowControllerMap

//    @Binds
//    abstract Object addOrViewMessageModalController(AddOrViewMessageModalController controller);

    @Binds
    @IntoMap
    @ClassKey(KafkaMessageViewController.class)
    @ViewControllerMap
    @AppScoped
    abstract Object kafkaMessageViewController(KafkaMessageViewController controller);

    @Binds
    @IntoMap
    @ClassKey(SchemaRegistryViewController.class)
    @ViewControllerMap
    @AppScoped
    abstract Object schemaRegistryViewController(SchemaRegistryViewController controller);

//    @Binds
//    @IntoMap
//    @ClassKey(KafkaMessageView.class)
//    @Binds
//    abstract Object kafkaMessageView(KafkaMessageView controller);

    @AppScoped
    @Provides
    @WindowControllerMap
    static Function<URL, FXMLLoader> fxmlLoaderFactory(@WindowControllerMap Callback<Class<?>, Object> controllerFactory) {
        return url -> {
            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(controllerFactory);
            return loader;
        };
    }

    @AppScoped
    @Provides
    @WindowControllerMap
//    static Callback<Class<?>, Object> controllerFactory(@WindowControllerMap Map<Class<?>, Object> controllerMap, AddOrViewMessageModalController addOrViewMessageModalController) {
    static Callback<Class<?>, Object> controllerFactory(@WindowControllerMap Map<Class<?>, Object> controllerMap, SerDesHelper serDesHelper,
                                                        JsonHighlighter jsonHighlighter, @RichTextFxObjectMapper ObjectMapper objectMapper,
                                                        ProducerUtil producerUtil, EventDispatcher eventDispatcher, SchemaRegistryManager schemaRegistryManager) {
//        return new DaggerCallback() {
//            @Override
//            public Object call(Class<?> clazz) {
//                useDagger =controllerFactory.containsKey(clazz);
//                return controllerFactory.get(clazz);
//            }
//        };
        return (clazz) -> {
            if (controllerMap.containsKey(clazz)) {
                return controllerMap.get(clazz);
            } else if (clazz.equals(AddOrViewMessageModalController.class)) {
                return new AddOrViewMessageModalController(serDesHelper, jsonHighlighter, objectMapper, producerUtil, eventDispatcher, schemaRegistryManager);
            } else {
                try {
                    return clazz.getConstructor().newInstance();
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                         IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
