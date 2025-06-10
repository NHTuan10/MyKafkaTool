package io.github.nhtuan10.mykafkatool.dagger;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import io.github.nhtuan10.mykafkatool.ui.controller.AddConnectionModalController;
import io.github.nhtuan10.mykafkatool.ui.controller.AddOrViewMessageModalController;
import io.github.nhtuan10.mykafkatool.ui.controller.MainController;
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
    abstract Object mainController(MainController controller);

    @Binds
    @IntoMap
    @ClassKey(AddConnectionModalController.class)
    abstract Object addConnectionModalController(AddConnectionModalController controller);


    @Binds
    @IntoMap
    @ClassKey(AddOrViewMessageModalController.class)
    abstract Object addOrViewMessageModalController(AddOrViewMessageModalController controller);

//    @Binds
//    @IntoMap
//    @ClassKey(KafkaMessageView.class)
//    @Binds
//    abstract Object kafkaMessageView(KafkaMessageView controller);

    @AppScoped
    @Provides
    static Function<URL, FXMLLoader> fxmlLoaderFactory(Callback<Class<?>, Object> controllerFactory) {
        return url -> {
            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(controllerFactory);
            return loader;
        };
    }

    @AppScoped
    @Provides
    static Callback<Class<?>, Object> controllerFactory(Map<Class<?>, Object> controllerFactory) {
//        return new DaggerCallback() {
//            @Override
//            public Object call(Class<?> clazz) {
//                useDagger =controllerFactory.containsKey(clazz);
//                return controllerFactory.get(clazz);
//            }
//        };
        return (clazz) -> {
            if (controllerFactory.containsKey(clazz)) {
                return controllerFactory.get(clazz);
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
