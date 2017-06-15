package me.nqkdev.plugins.maven;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeSpec.Builder;
import io.protostuff.compiler.ParserModule;
import io.protostuff.compiler.model.DynamicMessage;
import io.protostuff.compiler.model.DynamicMessage.Value;
import io.protostuff.compiler.model.Field;
import io.protostuff.compiler.model.Message;
import io.protostuff.compiler.model.Proto;
import io.protostuff.compiler.parser.FileReader;
import io.protostuff.compiler.parser.Importer;
import io.protostuff.compiler.parser.ProtoContext;
import io.protostuff.generator.java.MessageFieldUtil;
import io.protostuff.generator.java.ProtoUtil;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import static me.nqkdev.plugins.maven.ClassUtils.findClassByName;
import static io.protostuff.generator.java.MessageFieldUtil.*;

/**
 * A generator for hamcrest {@link FeatureMatcher} which generates {@link Matcher} for protobuf.
 *
 * @see <a href=https://github.com/yandex-qatools/hamcrest-pojo-matcher-generator>Hamcrest Feature Matcher Generator for
 * POJOs</a>
 */
public class Generator {

    private static final String MATCHER_CLASS_AFFIX = "Matchers";

    private static final Set<String> PROTO_PACKAGES = new HashSet<>();

    private FileReader fileReader;

    private Importer importer;

    public Generator(FileReader fileReader) {
        this.fileReader = fileReader;

        Injector injector = Guice.createInjector(new ParserModule());
        importer = injector.getInstance(Importer.class);
    }

    public void generateMatchers(String protoFile, String output) {
        ProtoContext context = importer.importFile(fileReader, protoFile);
        Proto proto = context.getProto();

        DynamicMessage options = proto.getOptions();
        String packageName = options.get("java_package") != null
                ? options.get("java_package").getString()
                : proto.getCanonicalName();

        collectGeneratedClass(proto);

        TypeSpec matcherClass = createMatcherClass(proto);

        JavaFile javaFile = JavaFile.builder(packageName, matcherClass)
                .build();

        try {
            javaFile.writeTo(new File(output));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void collectGeneratedClass(Proto proto) {
        PROTO_PACKAGES.add(proto.getOptions()
                .getOrDefault(ProtoUtil.OPTION_JAVA_PACKAGE,
                        Value.createString("proto"))
                .getString());

        proto.getPublicImports().forEach(anImport -> collectGeneratedClass(anImport.getProto()));
    }

    private TypeSpec createMatcherClass(Proto proto) {
        Builder builder = TypeSpec.classBuilder(proto.getName().concat(MATCHER_CLASS_AFFIX))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        parseProto(proto, builder);

        return builder.build();
    }

    private static void parseProto(Proto proto, Builder builder) {
        proto.getMessages().forEach(message -> {
            parseMessage(message, builder);
        });
    }

    private static void parseMessage(Message message, Builder builder) {
        message.getMessages().forEach(m -> {
            if (m.isMapEntry())
                return;

            Builder nBuilder = TypeSpec.classBuilder(m.getName().concat(MATCHER_CLASS_AFFIX))
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
            parseMessage(m, nBuilder);

            builder.addType(nBuilder.build());
        });

        message.getFields().forEach(f -> {
            try {
                parseField(f, builder);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void parseField(Field field, Builder builder) throws ClassNotFoundException {
        String getterMethod;
        TypeName fieldType;

        Message parentMessage = (Message) field.getParent();

        if (field.isMap()) {
            // If field is of map type
            getterMethod = getMapGetterName(field);
            Type keyType = findProtoClass(getMapFieldKeyType(field));
            Type valueType = findProtoClass(getMapFieldValueType(field));
            fieldType = ParameterizedTypeName.get(findClassByName("Map"), keyType, valueType);
        } else {
            // Element type
            Type wrappedFieldType = findProtoClass(
                    MessageFieldUtil.getFieldType(field).contains(".")
                            ? field.getTypeName()
                            : MessageFieldUtil.getWrapperFieldType(field));

            if (field.isRepeated()) {
                // If field is of list type
                getterMethod = getRepeatedFieldGetterName(field);
                fieldType = ParameterizedTypeName.get(findClassByName("List"), wrappedFieldType);
            } else {
                getterMethod = getFieldGetterName(field);
                fieldType = TypeName.get(wrappedFieldType);
            }
        }

        TypeName ownerType = TypeName.get(findProtoClass(parentMessage.getName()));
        ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(Matcher.class), ownerType);
        ParameterSpec matcher = ParameterSpec.builder(
                ParameterizedTypeName.get(ClassName.get(Matcher.class),
                        field.isRepeated()
                                ? WildcardTypeName.supertypeOf(fieldType)
                                : fieldType),
                "matcher")
                .build();

        MethodSpec matcherMethod = MethodSpec.methodBuilder("with" + Naming.normalize(field.getName()))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(matcher)
                .returns(returnType)
                .addJavadoc("Matcher for {@link $T#$L_}\n", ownerType, field.getName())
                .addStatement("return $L",
                        TypeSpec.anonymousClassBuilder(
                                "$N, $S, $S",
                                matcher,
                                field.getName(),
                                field.getName()
                        ).addSuperinterface(
                                ParameterizedTypeName.get(
                                        ClassName.get(FeatureMatcher.class),
                                        ownerType,
                                        fieldType
                                )
                        ).addMethod(
                                MethodSpec.methodBuilder("featureValueOf")
                                        .addAnnotation(Override.class)
                                        .addModifiers(Modifier.PUBLIC)
                                        .addParameter(ownerType, "actual")
                                        .returns(fieldType)
                                        .addStatement(
                                                "return $L.$L()",
                                                "actual",
                                                getterMethod
                                        )
                                        .build()
                        ).build()
                )
                .build();
        builder.addMethod(matcherMethod);
    }

    private static Class<?> findProtoClass(String name) throws ClassNotFoundException {
        name = name.substring(name.lastIndexOf(".") + 1);
        return findClassByName(name, PROTO_PACKAGES);
    }

}
