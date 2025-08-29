package io.github.nhtuan10.mykafkatool.annotationprocessor;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import javafx.beans.property.*;
import org.apache.commons.lang3.StringUtils;

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
import java.util.stream.Stream;

//@javax.annotation.processing.SupportedAnnotationTypes("io.github.nhtuan10.mykafkatool.annotationprocessor.FXDataModel")
//@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_21)
public class FXModelAnnotationProcessor extends AbstractProcessor {
    enum FXPropertyType {
        STRING_PROPERTY(StringProperty.class.getName(), "String", SimpleStringProperty.class.getName()),
        SIMPLE_STRING_PROPERTY(SimpleStringProperty.class.getName(), "String", SimpleStringProperty.class.getName()),
        INTEGER_PROPERTY(IntegerProperty.class.getName(), "int", SimpleIntegerProperty.class.getName()),
        SIMPLE_INTEGER_PROPERTY(SimpleIntegerProperty.class.getName(), "int", SimpleIntegerProperty.class.getName()),
        LONG_PROPERTY(LongProperty.class.getName(), "long", SimpleLongProperty.class.getName()),
        SIMPLE_LONG_PROPERTY(SimpleLongProperty.class.getName(), "long", SimpleLongProperty.class.getName()),
        DOUBLE_PROPERTY(DoubleProperty.class.getName(), "double", SimpleDoubleProperty.class.getName()),
        SIMPLE_DOUBLE_PROPERTY(SimpleDoubleProperty.class.getName(), "double", SimpleDoubleProperty.class.getName()),
        OBJECT_PROPERTY(ObjectProperty.class.getName(), "Object", SimpleObjectProperty.class.getName()),
        SIMPLE_OBJECT_PROPERTY(SimpleObjectProperty.class.getName(), "Object", SimpleObjectProperty.class.getName()),
        BOOLEAN_PROPERTY(BooleanProperty.class.getName(), "boolean", SimpleBooleanProperty.class.getName()),
        SIMPLE_BOOLEAN_PROPERTY(SimpleBooleanProperty.class.getName(), "boolean", SimpleBooleanProperty.class.getName());

        private final String typeName;
        private final String underlyingType;
        private final String implType;

        public String getTypeName() {
            return typeName;
        }

        public String getUnderlyingType() {
            return underlyingType;
        }

        public String getImplType() {
            return implType;
        }

        FXPropertyType(String typeName, String underlyingType, String implType) {
            this.typeName = typeName;
            this.underlyingType = underlyingType;
            this.implType = implType;
        }

        public static FXPropertyType getEnumByType(String type) {
            return Arrays.stream(values())
                    .filter(e -> e.typeName.equals(type))
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
        handlebars.registerHelper("toUpperUnderscore", (obj, opt) -> {
            if (obj instanceof String str) {
                return StringUtils.upperCase(StringUtils.join(
                        StringUtils.splitByCharacterTypeCamelCase(str),
                        '_'));
            }
            return null;
        });
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
                        List<TemplateContextObject.Field> templateFxFields = fields.stream()
                                .filter(field -> (!field.getModifiers().contains(Modifier.STATIC) && field.getAnnotation(FXModelIgnore.class) == null))
                                .filter(field -> FXPropertyType.getEnumByType(field.asType().toString()) != null)
                                .map(field -> new TemplateContextObject.Field(field.getSimpleName().toString(), field.asType().toString(), true))
                                .toList();
                        List<TemplateContextObject.Field> templateNonFxFields = fields.stream()
                                .flatMap(field -> {
                                    if (!field.getModifiers().contains(Modifier.STATIC) && field.getAnnotation(FXModelIgnore.class) == null && templateFxFields.stream().map(TemplateContextObject.Field::getType).noneMatch(f -> f.equals(field.asType().toString()))) {
                                        return Stream.of(new TemplateContextObject.Field(field.getSimpleName().toString(), field.asType().toString(), false));
                                    } else
                                        return Stream.empty();
                                }).toList();
                        String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();
                        TemplateContextObject templateContextObject = new TemplateContextObject(simpleName, packageName, templateFxFields, templateNonFxFields);
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
        private final List<Field> fxFields;
        private final List<Field> nonFxFields;
        static final List<Class<?>> primitiveTypes = List.of(
                Boolean.TYPE,
                Character.TYPE,
                Byte.TYPE,
                Short.TYPE,
                Integer.TYPE,
                Long.TYPE,
                Float.TYPE,
                Double.TYPE);

        public TemplateContextObject(String className, String packageName, List<Field> fxFields, List<Field> nonFxFields) {
            this.className = className;
            this.packageName = packageName;
            this.fxFields = fxFields;
            this.nonFxFields = nonFxFields;
        }

        public String getClassName() {
            return className;
        }

        public String getPackageName() {
            return packageName;
        }

        public List<Field> getFxFields() {
            return fxFields;
        }

        public List<Field> getNonFxFields() {
            return nonFxFields;
        }

        public List<Field> getAllFields() {
            return Stream.concat(fxFields.stream(), nonFxFields.stream()).toList();
        }

        public Set<String> getImports() {
            Stream<String> fxFieldStream = fxFields.stream().flatMap(field -> {
                        var optional = Optional.ofNullable(FXPropertyType.getEnumByType(field.getType()));
                        if (optional.isPresent()) {
                            FXPropertyType fxPropertyType = optional.get();
                            return Stream.of(fxPropertyType.getTypeName(), fxPropertyType.getImplType());
                        } else {
                            return Stream.empty();
                        }
                    }
            );
            Stream<String> nonFxFieldStream = nonFxFields.stream().map(Field::getType)
                    .filter(t -> primitiveTypes.stream().noneMatch(primitiveType -> primitiveType.getName().equals(t)));
            return Stream.concat(fxFieldStream, nonFxFieldStream).collect(Collectors.toSet());
        }

        public static class Field {
            private final String name;
            private final String type;
            private final String underlyingType;
            private final String implType;
            private final boolean isFxField;

            public Field(String name, String type, boolean isFxField) {
                this.name = name;
                this.type = type;
                this.isFxField = isFxField;
                if (isFxField) {
                    var optional = Optional.ofNullable(FXPropertyType.getEnumByType(type));
                    this.underlyingType = optional.map(FXPropertyType::getUnderlyingType)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid field name: " + name));
                    this.implType = optional.map(FXPropertyType::getImplType)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid field name: " + name));
                } else {
                    this.underlyingType = null;
                    this.implType = null;
                }
            }

            public String getTypeSimpleName() {
                return this.type.substring(this.type.lastIndexOf('.') + 1);
            }


            public String getImplTypeSimpleName() {
                try {
                    return Class.forName(this.implType).getSimpleName();
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

            public String getImplType() {
                return implType;
            }
        }
    }
}