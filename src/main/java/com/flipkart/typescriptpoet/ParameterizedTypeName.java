package com.flipkart.typescriptpoet;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static com.flipkart.typescriptpoet.Util.checkArgument;
import static com.flipkart.typescriptpoet.Util.checkNotNull;

public class ParameterizedTypeName extends TypeName {
    public final ClassName rawType;
    public final List<TypeName> typeArguments;
    private final ParameterizedTypeName enclosingType;
    private MapParameterizedTypeName mapParameterizedTypeName;
    private ListParameterizedTypeName listParameterizedTypeName;

    ParameterizedTypeName(ParameterizedTypeName enclosingType, ClassName rawType,
                          List<TypeName> typeArguments) {
        this(enclosingType, rawType, typeArguments, new ArrayList<AnnotationSpec>());
    }

    private ParameterizedTypeName(ParameterizedTypeName enclosingType, ClassName rawType,
                                  List<TypeName> typeArguments, List<AnnotationSpec> annotations) {
        super(annotations);
        this.rawType = checkNotNull(rawType, "rawType == null");
        this.enclosingType = enclosingType;
        this.typeArguments = Util.immutableList(typeArguments);
        checkArgument(!this.typeArguments.isEmpty() || enclosingType != null,
                "no type arguments: %s", rawType);
        for (TypeName typeArgument : this.typeArguments) {
            checkArgument(!typeArgument.isPrimitive() && typeArgument != VOID,
                    "invalid type parameter: %s", typeArgument);
        }
    }

    /**
     * Returns a parameterized type, applying {@code typeArguments} to {@code rawType}.
     */
    public static ParameterizedTypeName get(ClassName rawType, TypeName... typeArguments) {
        return new ParameterizedTypeName(null, rawType, Arrays.asList(typeArguments));
    }

    /**
     * Returns a parameterized type, applying {@code typeArguments} to {@code rawType}.
     */
    public static ParameterizedTypeName get(Class<?> rawType, Type... typeArguments) {
        return new ParameterizedTypeName(null, ClassName.get(rawType), list(typeArguments));
    }

    /**
     * Returns a parameterized type equivalent to {@code type}.
     */
    public static ParameterizedTypeName get(ParameterizedType type) {
        return get(type, new LinkedHashMap<Type, TypeVariableName>());
    }

    /**
     * Returns a parameterized type equivalent to {@code type}.
     */
    static ParameterizedTypeName get(ParameterizedType type, Map<Type, TypeVariableName> map) {
        ClassName rawType = ClassName.get((Class<?>) type.getRawType());
        ParameterizedType ownerType = (type.getOwnerType() instanceof ParameterizedType)
                && !Modifier.isStatic(((Class<?>) type.getRawType()).getModifiers())
                ? (ParameterizedType) type.getOwnerType() : null;
        List<TypeName> typeArguments = TypeName.list(type.getActualTypeArguments(), map);
        return (ownerType != null)
                ? get(ownerType, map).nestedClass(rawType.simpleName(), typeArguments)
                : new ParameterizedTypeName(null, rawType, typeArguments);
    }

    private MapParameterizedTypeName getMapParameterizedTypeName() {
        if (mapParameterizedTypeName == null) {
            mapParameterizedTypeName = new MapParameterizedTypeName(enclosingType, rawType, typeArguments);
        }
        return mapParameterizedTypeName;
    }

    private ListParameterizedTypeName getListParameterizedTypeName() {
        if (listParameterizedTypeName == null) {
            listParameterizedTypeName = new ListParameterizedTypeName(enclosingType, rawType, typeArguments);
        }
        return listParameterizedTypeName;
    }

    @Override
    public ParameterizedTypeName annotated(List<AnnotationSpec> annotations) {
        return new ParameterizedTypeName(
                enclosingType, rawType, typeArguments, concatAnnotations(annotations));
    }

    @Override
    public TypeName withoutAnnotations() {
        return new ParameterizedTypeName(
                enclosingType, rawType, typeArguments, new ArrayList<AnnotationSpec>());
    }

    @Override
    CodeWriter emit(CodeWriter out) throws IOException {
        if (Util.isMap(rawType)) {
            return getMapParameterizedTypeName().emit(out);
        }

        return getListParameterizedTypeName().emit(out);
    }

    /**
     * Returns a new {@link ParameterizedTypeName} instance for the specified {@code name} as nested
     * inside this class.
     */
    public ParameterizedTypeName nestedClass(String name) {
        checkNotNull(name, "name == null");
        return new ParameterizedTypeName(this, rawType.nestedClass(name), new ArrayList<TypeName>(),
                new ArrayList<AnnotationSpec>());
    }

    /**
     * Returns a new {@link ParameterizedTypeName} instance for the specified {@code name} as nested
     * inside this class, with the specified {@code typeArguments}.
     */
    public ParameterizedTypeName nestedClass(String name, List<TypeName> typeArguments) {
        checkNotNull(name, "name == null");
        return new ParameterizedTypeName(this, rawType.nestedClass(name), typeArguments,
                new ArrayList<AnnotationSpec>());
    }
}