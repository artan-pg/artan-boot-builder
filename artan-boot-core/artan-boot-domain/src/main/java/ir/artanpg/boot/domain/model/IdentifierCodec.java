/*
 * Copyright (c) 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ir.artanpg.boot.domain.model;

import ir.artanpg.boot.domain.exception.DomainException;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Encodes and decodes {@link Identifier} instances to a portable wire form.
 *
 * <p>Supported scalar value types are limited to {@link String}, {@link Long},
 * {@link Integer} and {@link UUID}. Composite values are supported only as
 * Java records whose components recursively obey the same rule.
 *
 * <p>Intended for infrastructure serializers (JSON, outbox, event store).
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public final class IdentifierCodec {

    private static final String WIRE_EXCEPTION_MSG = "The wire format cannot be null";

    /**
     * Set of scalar value types supported by the codec.
     *
     * <p>Contains both boxed and primitive forms of {@code Long} and
     * {@code Integer}, plus {@code String} and {@code UUID}.
     */
    private static final Set<Class<?>> SUPPORTED_SCALARS = Set.of(
            String.class,
            Long.class,
            Integer.class,
            UUID.class
    );

    private static final String[] FACTORY_NAMES = {"of", "valueOf", "from"};

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws UnsupportedOperationException if called via reflection (implicitly)
     */
    private IdentifierCodec() {
        throw new UnsupportedOperationException();
    }

    /**
     * Encodes the given identifier into a portable wire format.
     *
     * @param identifier the identifier to encode
     * @return wire representation
     * @throws DomainException if the identifier or its value is invalid / unsupported
     */
    @NonNull
    public static WireFormat toWire(@NonNull Identifier<?> identifier) {
        if (identifier == null) throw new DomainException("The identifier cannot be null");
        Object raw = identifier.requireValid();
        return new WireFormat(identifier.getClass().getName(), raw.getClass().getName(), encodeValue(raw));
    }

    /**
     * Decodes a wire format into an identifier of the expected type.
     *
     * @param wire         portable representation
     * @param expectedType expected concrete identifier class
     * @param <I>          identifier type
     * @return reconstituted identifier
     * @throws DomainException if decoding fails or types are incompatible
     */
    @NonNull
    public static <I extends Identifier<?>> I fromWire(@NonNull WireFormat wire, @NonNull Class<I> expectedType) {
        if (wire == null) throw new DomainException(WIRE_EXCEPTION_MSG);
        if (expectedType == null) throw new DomainException("The expected type cannot be null");

        Class<?> identifierClass = loadClass(wire.identifierType());
        if (!expectedType.isAssignableFrom(identifierClass)) {
            throw new DomainException(
                    String.format(
                            "Identifier type mismatch: wire declares %s but expected %s",
                            wire.identifierType(), expectedType.getName()));
        }

        Object value = decodeValue(wire.value(), wire.valueType());
        Identifier<?> identifier = instantiateIdentifier(identifierClass, value);
        return expectedType.cast(identifier);
    }

    // -------------------------------------------------------------------------
    // Value encode / decode
    // -------------------------------------------------------------------------

    /**
     * Encodes a value into a {@link ValueNode} tree.
     *
     * <p>Scalar values are encoded as {@link ValueNode.Scalar};
     * record values are recursively encoded as
     * {@link ValueNode.Composite}. Any other type is rejected.
     *
     * @param value the value to encode
     * @return the encoded node
     * @throws DomainException if the value type is not supported
     */
    private static ValueNode encodeValue(Object value) {
        Class<?> type = value.getClass();

        if (isSupportedScalar(type)) return new ValueNode.Scalar(normalizeScalarTypeName(type), stringifyScalar(value));
        if (type.isRecord()) return encodeRecord(value);

        throw new DomainException(
                String.format(
                        "Unsupported identifier value type: %s. Allowed scalars: String, Long, Integer, UUID; " +
                                "composites must be records of those scalars",
                        type.getName()));
    }

    /**
     * Encodes a record value as a {@link ValueNode.Composite}.
     *
     * <p>Each record component is read reflectively and recursively
     * encoded. Null component values are rejected.
     *
     * @param recordValue the record instance to encode
     * @return the encoded composite node
     * @throws DomainException if a component is {@code null}, cannot be read, or contains an unsupported value type
     */
    private static ValueNode.Composite encodeRecord(Object recordValue) {
        Class<?> recordType = recordValue.getClass();
        RecordComponent[] components = recordType.getRecordComponents();
        Map<String, ValueNode> fields = new LinkedHashMap<>();

        for (RecordComponent component : components) {
            try {
                Object componentValue = component.getAccessor().invoke(recordValue);
                if (componentValue == null) {
                    throw new DomainException(
                            String.format(
                                    "Record component %s of %s cannot be null",
                                    component.getName(), recordType.getName()));
                }
                fields.put(component.getName(), encodeValue(componentValue));
            } catch (ReflectiveOperationException ex) {
                throw new DomainException(ex.getMessage(), ex);
            }
        }

        return new ValueNode.Composite(recordType.getName(), fields);
    }

    /**
     * Decodes a value node into a Java object, guided by the wire
     * type name.
     *
     * @param node             the value node to decode
     * @param expectedTypeName the fully qualified name of the expected Java type
     * @return the decoded value
     * @throws DomainException if the node is of an unknown kind, the type name is unknown, or decoding fails
     */
    private static Object decodeValue(ValueNode node, String expectedTypeName) {
        Class<?> expectedType = loadClass(expectedTypeName);
        return switch (node) {
            case ValueNode.Scalar scalar -> decodeScalar(scalar, expectedType);
            case ValueNode.Composite composite -> decodeComposite(composite, expectedType);
        };
    }

    /**
     * Decodes a scalar node into the requested Java type.
     *
     * <p>The parsed value is coerced to the expected type if it is
     * compatible. Unsupported scalar types are rejected by
     * {@link #parseScalar(Class, String)}.
     *
     * @param scalar       the scalar node to decode
     * @param expectedType the expected Java type
     * @return the decoded scalar value
     * @throws DomainException if the scalar type is unsupported, the raw value is malformed, or coercion fails
     */
    private static Object decodeScalar(ValueNode.Scalar scalar, Class<?> expectedType) {
        Class<?> scalarType = loadClass(scalar.type());
        Object parsed = parseScalar(scalarType, scalar.value());
        return coerceScalar(parsed, expectedType);
    }

    /**
     * Decodes a composite node into a record instance.
     *
     * <p>The wire composite type must be a record assignable to the
     * expected type. The component count and names must match the
     * record's canonical components. Each component value is decoded
     * recursively before invoking the canonical constructor.</p>
     *
     * @param composite    the composite node to decode
     * @param expectedType the expected Java type
     * @return the decoded record instance
     * @throws DomainException if the type is not a record, does not match the expected type,
     *                         has a component mismatch, or cannot be instantiated
     */
    private static Object decodeComposite(ValueNode.Composite composite, Class<?> expectedType) {
        Class<?> compositeType = loadClass(composite.type());
        if (!compositeType.isRecord()) {
            throw new DomainException("Composite wire value must refer to a record type: " + composite.type());
        }
        if (!expectedType.isAssignableFrom(compositeType)) {
            throw new DomainException(
                    String.format(
                            "Composite type mismatch: wire declares %s but expected %s",
                            composite.type(), expectedType.getName()));
        }

        RecordComponent[] components = compositeType.getRecordComponents();
        if (components.length != composite.fields().size()) {
            throw new DomainException(
                    String.format(
                            "Composite field count mismatch for %s: expected %d but was %d",
                            composite.type(), components.length, composite.fields().size()));
        }

        Object[] args = new Object[components.length];
        Class<?>[] paramTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            ValueNode fieldNode = composite.fields().get(component.getName());
            if (fieldNode == null) {
                throw new DomainException(
                        String.format(
                                "Missing composite field %s for %s",
                                component.getName(), composite.type()));
            }
            paramTypes[i] = component.getType();
            args[i] = decodeValue(fieldNode, component.getType().getName());
        }

        try {
            Constructor<?> canonical = compositeType.getDeclaredConstructor(paramTypes);
            return canonical.newInstance(args);
        } catch (ReflectiveOperationException ex) {
            throw new DomainException("Failed to instantiate composite value type " + composite.type(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Identifier instantiation
    // -------------------------------------------------------------------------

    /**
     * Instantiates an {@link Identifier} from a decoded value.
     *
     * <p>Tries, in order: the record canonical constructor (when
     * the identifier is a record with a single component), any
     * single-argument constructor, and finally a static factory
     * method named {@code of}, {@code valueOf} or {@code from}.
     *
     * @param identifierClass the identifier class to instantiate
     * @param value           the decoded value
     * @return the instantiated identifier
     * @throws DomainException if none of the strategies succeed
     */
    private static Identifier<?> instantiateIdentifier(Class<?> identifierClass, Object value) {
        if (identifierClass.isRecord()) {
            Identifier<?> recordIdentifier = tryRecordIdentifier(identifierClass, value);
            if (recordIdentifier != null) return recordIdentifier;
        }

        Identifier<?> fromConstructor = trySingleArgConstructor(identifierClass, value);
        if (fromConstructor != null) return fromConstructor;

        Identifier<?> fromFactory = tryStaticFactory(identifierClass, value);
        if (fromFactory != null) return fromFactory;

        throw new DomainException(
                String.format(
                        "Cannot instantiate Identifier type %s from value type %s",
                        identifierClass.getName(), value.getClass().getName()));
    }

    /**
     * Attempts to instantiate a record identifier through its canonical
     * constructor.
     *
     * @param identifierClass the identifier class, must be a record with exactly one component
     * @param value           the decoded value
     * @return the instantiated identifier, or {@code null} when the strategy does not apply
     */
    private static Identifier<?> tryRecordIdentifier(Class<?> identifierClass, Object value) {
        RecordComponent[] components = identifierClass.getRecordComponents();
        if (components.length != 1) return null;

        try {
            Constructor<?> canonical = identifierClass.getDeclaredConstructor(components[0].getType());
            Object coerced = coerceTo(value, components[0].getType());
            return castIdentifier(canonical.newInstance(coerced));
        } catch (ReflectiveOperationException | DomainException _) {
            return null;
        }
    }

    /**
     * Attempts to instantiate an identifier through any single-argument
     * constructor.
     *
     * <p>Constructors are tried in reflection order. A constructor
     * is considered a match when its sole parameter type can accept
     * the decoded value after coercion.
     *
     * @param identifierClass the identifier class
     * @param value           the decoded value
     * @return the instantiated identifier, or {@code null} when no suitable constructor is found
     */
    private static Identifier<?> trySingleArgConstructor(Class<?> identifierClass, Object value) {
        for (Constructor<?> constructor : identifierClass.getDeclaredConstructors()) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length != 1) continue;

            try {
                Object coerced = coerceTo(value, params[0]);
                return castIdentifier(constructor.newInstance(coerced));
            } catch (ReflectiveOperationException | DomainException _) {
                // try next constructor
            }
        }
        return null;
    }

    /**
     * Attempts to instantiate an identifier through a static factory
     * method.
     *
     * <p>Eligible methods are identified by
     * {@link #isEligibleFactoryMethod(Method)}. The first matching
     * method whose sole parameter accepts the decoded value is
     * invoked; failures are silently ignored and the next candidate
     * is tried.
     *
     * @param identifierClass the identifier class
     * @param value           the decoded value
     * @return the instantiated identifier, or {@code null} when no suitable factory method is found
     */
    private static Identifier<?> tryStaticFactory(Class<?> identifierClass, Object value) {
        for (Method method : identifierClass.getDeclaredMethods()) {
            if (!isEligibleFactoryMethod(method)) continue;
            try {
                Object coerced = coerceTo(value, method.getParameterTypes()[0]);
                return castIdentifier(method.invoke(null, coerced));
            } catch (ReflectiveOperationException | DomainException _) {
                // try next factory
            }
        }
        return null;
    }

    /**
     * Checks whether the given method can serve as an identifier factory.
     *
     * <p>A method is eligible when it is static, takes exactly one
     * parameter, returns a type assignable to {@link Identifier},
     * and its name matches one of {@link #FACTORY_NAMES}.
     *
     * @param method the method to check
     * @return {@code true} if the method is eligible, {@code false} otherwise
     */
    private static boolean isEligibleFactoryMethod(Method method) {
        if (!Modifier.isStatic(method.getModifiers())) return false;
        if (method.getParameterCount() != 1) return false;
        if (!Identifier.class.isAssignableFrom(method.getReturnType())) return false;
        return matchesFactoryName(method.getName());
    }

    /**
     * Checks whether the given method name matches a recognized factory name.
     *
     * @param methodName the method name to check
     * @return {@code true} if the name is one of {@link #FACTORY_NAMES}, {@code false} otherwise
     */
    private static boolean matchesFactoryName(String methodName) {
        for (String factoryName : FACTORY_NAMES) {
            if (factoryName.equals(methodName)) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Scalar helpers
    // -------------------------------------------------------------------------

    /**
     * Checks whether the given class is one of the supported scalar types.
     *
     * @param type the class to check
     * @return {@code true} if the type is supported, {@code false} otherwise
     */
    private static boolean isSupportedScalar(Class<?> type) {
        return SUPPORTED_SCALARS.contains(type);
    }

    /**
     * Normalizes a scalar class to its canonical boxed type name.
     *
     * @param type the scalar class to normalize
     * @return the canonical type name
     */
    private static String normalizeScalarTypeName(Class<?> type) {
        if (type == Long.class) return Long.class.getName();
        if (type == Integer.class) return Integer.class.getName();
        if (type == UUID.class) return UUID.class.getName();

        return String.class.getName();
    }

    /**
     * Converts a scalar value to its canonical wire string form.
     *
     * @param value the scalar value
     * @return the canonical string form
     */
    private static String stringifyScalar(Object value) {
        if (value instanceof UUID uuid) return uuid.toString();
        return String.valueOf(value);
    }

    /**
     * Parses a raw string into an instance of the given scalar type.
     *
     * <p>Supported types are {@link String}, {@link UUID}, {@link Long}
     * and {@link Integer}. Any other type is rejected with a
     * {@link DomainException}.
     *
     * @param scalarType the target scalar type
     * @param raw        the raw string to parse
     * @return the parsed value
     * @throws DomainException if the type is unsupported or the string is malformed
     */
    private static Object parseScalar(Class<?> scalarType, String raw) {
        try {
            return switch (scalarType.getName()) {
                case "java.lang.String" -> raw;
                case "java.util.UUID" -> UUID.fromString(raw);
                case "java.lang.Long" -> Long.valueOf(raw);
                case "java.lang.Integer" -> Integer.valueOf(raw);
                default -> throw new DomainException(
                        String.format(
                                "Unsupported scalar type: %s. Allowed: String, Long, Integer, UUID",
                                scalarType.getName()));
            };
        } catch (IllegalArgumentException ex) {
            String messageException = String.format("Invalid %s identifier value: %s", scalarType.getSimpleName(), raw);
            throw new DomainException(messageException, ex);
        }
    }

    /**
     * Coerces a scalar value to the expected target type.
     *
     * <p>Direct instances are returned unchanged. Numeric widening
     * and narrowing between {@link Number} subtypes is supported,
     * as are conversions to {@link String} and {@link UUID}.
     *
     * @param value        the value to coerce
     * @param expectedType the target type
     * @return the coerced value
     * @throws DomainException if coercion is not possible
     */
    private static Object coerceScalar(Object value, Class<?> expectedType) {
        if (expectedType.isInstance(value)) return value;
        if (expectedType == String.class) return String.valueOf(value);
        if (expectedType == UUID.class && value instanceof String string) return UUID.fromString(string);

        if (value instanceof Number number) {
            if (expectedType == Long.class || expectedType == long.class) return number.longValue();
            if (expectedType == Integer.class || expectedType == int.class) return number.intValue();
        }

        throw new DomainException(
                String.format(
                        "Cannot coerce scalar value of type %s to %s",
                        value.getClass().getName(), expectedType.getName()));
    }

    /**
     * Coerces a value to the target type when both sides are scalars
     * or instances of each other.
     *
     * <p>When the target type already accepts the value, the value is
     * returned unchanged. Otherwise, the coercion is delegated to
     * {@link #coerceScalar(Object, Class)} if both types are
     * supported scalars.
     *
     * @param value      the value to coerce
     * @param targetType the target type
     * @return the coerced value
     * @throws DomainException if the value cannot be coerced
     */
    private static Object coerceTo(Object value, Class<?> targetType) {
        if (targetType.isInstance(value)) return value;
        if (isSupportedScalar(targetType) || isSupportedScalar(value.getClass())) {
            return coerceScalar(value, targetType);
        }

        throw new DomainException(
                String.format(
                        "Cannot coerce value of type %s to %s",
                        value.getClass().getName(), targetType.getName()));
    }

    /**
     * Validates that the given instance is an {@link Identifier} and
     * that it reports itself as valid.
     *
     * @param instance the instance to validate
     * @return the same instance, cast to {@link Identifier}
     * @throws DomainException if the instance is not an {@link Identifier} or fails its validation
     */
    private static Identifier<?> castIdentifier(Object instance) {
        if (!(instance instanceof Identifier<?> identifier)) {
            throw new DomainException("Constructed instance does not implement Identifier");
        }
        identifier.requireValid();
        return identifier;
    }

    /**
     * Loads a class by its fully qualified name.
     *
     * @param className the fully qualified class name
     * @return the loaded class
     * @throws DomainException if the class is not available on the classpath
     */
    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new DomainException("Unknown type on the classpath: " + className, ex);
        }
    }

    /**
     * Portable snapshot of an {@link Identifier}.
     *
     * @param identifierType fully qualified name of the Identifier implementation
     * @param valueType      fully qualified name of Identifier#value()'s runtime type
     * @param value          scalar or composite value tree
     */
    public record WireFormat(@NonNull String identifierType, @NonNull String valueType, @NonNull ValueNode value) {

        /**
         * Validates the record components.
         *
         * @param identifierType fully qualified name of the Identifier implementation
         * @param valueType      fully qualified name of Identifier#value()'s runtime type
         * @param value          scalar or composite value tree
         * @throws DomainException if any component is {@code null}, identifierType is {@code blank},
         *                         or valueType is {@code blank}
         */
        public WireFormat {
            if (identifierType == null || identifierType.isBlank()) {
                throw new DomainException("The identifierType cannot be null or blank");
            }
            if (valueType == null || valueType.isBlank()) {
                throw new DomainException("The valueType cannot be null or blank");
            }
            if (value == null) {
                throw new DomainException("The value cannot be null");
            }
        }
    }

    /**
     * Wire representation of an identifier value.
     */
    public sealed interface ValueNode permits ValueNode.Scalar, ValueNode.Composite {

        /**
         * Scalar leaf value.
         *
         * <p>Represents the wire form of a single scalar identifier value.
         * The {@code type} field holds the fully qualified name of one of
         * the codec's supported scalar types, and {@code value} holds its
         * canonical string form.
         *
         * @param type  fully qualified scalar type name
         * @param value canonical string form of the scalar
         */
        record Scalar(@NonNull String type, @NonNull String value) implements ValueNode {

            /**
             * Validates the record components.
             *
             * @param type  fully qualified scalar type name
             * @param value canonical string form of the scalar
             * @throws DomainException if type is {@code null} or {@code blank}, or value is {@code null}
             */
            public Scalar {
                if (type == null || type.isBlank()) {
                    throw new DomainException("The scalar type cannot be null or blank");
                }
                if (value == null) {
                    throw new DomainException("The scalar value cannot be null");
                }
            }
        }

        /**
         * Composite record value.
         *
         * <p>Represents the wire form of a record identifier value. The
         * {@code type} field holds the fully qualified name of the record
         * class, and {@code fields} holds an ordered map of component name
         * to nested {@link ValueNode}.
         *
         * <p>The {@code fields} map is defensively copied into an
         * unmodifiable, insertion-ordered map upon construction, so
         * later mutations of the source map do not affect this
         * instance.
         *
         * @param type   fully qualified record type name
         * @param fields ordered map of component name to nested value
         */
        record Composite(@NonNull String type, @NonNull Map<String, ValueNode> fields) implements ValueNode {

            /**
             * Validates the record components and defensively copies the
             * fields map.
             *
             * @param type   fully qualified record type name
             * @param fields ordered map of component name to nested value
             * @throws DomainException if type is {@code null} or {@code blank}, or fields is {@code null}
             */
            public Composite {
                if (type == null || type.isBlank()) {
                    throw new DomainException("The composite type cannot be null or blank");
                }
                if (fields == null) {
                    throw new DomainException("The composite fields cannot be null");
                }
                fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
            }
        }
    }
}
