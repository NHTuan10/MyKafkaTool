package io.github.nhtuan10.mykafkatool.annotationprocessor;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import javafx.beans.property.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

//@javax.annotation.processing.SupportedAnnotationTypes("io.github.nhtuan10.mykafkatool.annotationprocessor.FXDataModel")
//@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_21)
public class FXModelAnnotationProcessor extends AbstractProcessor {
    enum FXPropertyType {
        SIMPLE_STRING_PROPERTY(SimpleStringProperty.class.getName(), "String"),

        SIMPLE_INTEGER_PROPERTY(SimpleIntegerProperty.class.getName(), "int"),

        SIMPLE_LONG_PROPERTY(SimpleLongProperty.class.getName(), "long"),

        SIMPLE_DOUBLE_PROPERTY(SimpleDoubleProperty.class.getName(), "double"),

        SIMPLE_OBJECT_PROPERTY(SimpleObjectProperty.class.getName(), "Object");

        private final String type;
        private final String underlyingType;

        public String getType() {
            return type;
        }

        public String getUnderlyingType() {
            return underlyingType;
        }

        FXPropertyType(String type, String underlyingType) {
            this.type = type;
            this.underlyingType = underlyingType;
        }

        public static FXPropertyType getEnumByType(String type) {
            return Arrays.stream(values())
                    .filter(e -> e.type.equals(type))
                    .findFirst()
                    .orElse(null);
        }
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        // Initialization code, if needed
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(FXModel.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager messager = processingEnv.getMessager();
        TemplateLoader loader = new ClassPathTemplateLoader();
        loader.setPrefix("/templates");
        Handlebars handlebars = new Handlebars(loader);
        handlebars.registerHelpers(StringHelpers.class);
        Template template;
        try {
            template = handlebars.compile("fx-model");
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate class: Errors in loading template " + e.getMessage());
            throw new RuntimeException(e);
        }

        for (TypeElement annotation : annotations) {
            // Find elements annotated with MyCustomAnnotation
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                // Process each element
                messager.printMessage(Diagnostic.Kind.NOTE, "Processing: " + element.getSimpleName());
                Elements elementUtils = processingEnv.getElementUtils();
                Filer filer = processingEnv.getFiler();
                try {
                    if (element instanceof TypeElement typeElement) {
                        String qualifiedName = typeElement.getQualifiedName().toString();
                        String simpleName = typeElement.getSimpleName().toString();
                        var fields = ElementFilter.fieldsIn(elementUtils.getAllMembers(typeElement));
                        List<TemplateContextObject.Field> templateContextObjectFields = fields.stream()
                                .filter(field -> !field.getModifiers().contains(Modifier.STATIC))
                                .filter(field -> FXPropertyType.getEnumByType(field.asType().toString()) != null)
                                .map(field -> new TemplateContextObject.Field(field.getSimpleName().toString(), field.asType().toString()))
                                .toList();
                        String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();
                        TemplateContextObject templateContextObject = new TemplateContextObject(simpleName, packageName, templateContextObjectFields);
                        JavaFileObject fileObject = filer.createSourceFile(qualifiedName + "FXModel");
                        try (Writer writer = fileObject.openWriter()) {
                            String generatedCode = template.apply(templateContextObject);
                            writer.write(generatedCode);
                            messager.printMessage(Diagnostic.Kind.NOTE, "Generated: " + fileObject.toUri());
                        }
                    }
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate class: " + e.getMessage());
                }
                // Your processing logic here
            }
        }
        return true; // No further processing of this annotation type
    }

    public static class TemplateContextObject {
        private final String className;
        private final String packageName;
        private final List<Field> fields;

        public TemplateContextObject(String className, String packageName, List<Field> fields) {
            this.className = className;
            this.packageName = packageName;
            this.fields = fields;
        }

        public String getClassName() {
            return className;
        }

        public String getPackageName() {
            return packageName;
        }

        public List<Field> getFields() {
            return fields;
        }

        public Set<String> getImports() {
            return fields.stream().map(Field::getType).collect(Collectors.toSet());
        }

        public static class Field {
            private final String name;
            private final String type;
            private final String underlyingType;
            private final String simpleTypeName;

            public Field(String name, String type) {
                this.name = name;
                this.type = type;
                this.underlyingType = Optional.ofNullable(FXPropertyType.getEnumByType(type))
                        .map(FXPropertyType::getUnderlyingType)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid field name: " + name));
                try {
                    this.simpleTypeName = Class.forName(type).getSimpleName();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            public String getName() {
                return name;
            }

            public String getType() {
                return type;
            }

            public String getUnderlyingType() {
                return underlyingType;
            }

            public String getSimpleTypeName() {
                return simpleTypeName;
            }
        }
    }
}