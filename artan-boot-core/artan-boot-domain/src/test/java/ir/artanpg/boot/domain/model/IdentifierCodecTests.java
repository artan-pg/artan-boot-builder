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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link IdentifierCodec}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("IdentifierCodec")
class IdentifierCodecTests {

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Throws UnsupportedOperationException when instantiated via reflection")
        void Constructor_ShouldThrowUnsupportedOperationException_WhenInvokedViaReflection() {
            // given
            Constructor<IdentifierCodec> constructor;
            try {
                constructor = IdentifierCodec.class.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            constructor.setAccessible(true);

            // when & then
            thenThrownBy(constructor::newInstance)
                    .as("the utility-class constructor must reject external instantiation by throwing "
                            + "UnsupportedOperationException")
                    .isInstanceOf(InvocationTargetException.class)
                    .cause()
                    .as("the reflective wrapper must expose the underlying UnsupportedOperationException")
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("toWire(Identifier)")
    class ToWireTests {

        @Test
        @DisplayName("Encodes a String-based identifier into a scalar wire node")
        void ToWire_ShouldEncodeStringIdentifier_WhenIdentifierIsStringBased() {
            // given
            StringId identifier = new StringId("abc");

            // when
            IdentifierCodec.WireFormat wire = IdentifierCodec.toWire(identifier);

            // then
            then(wire.identifierType())
                    .as("the wire must record the concrete identifier class name")
                    .isEqualTo(StringId.class.getName());
            then(wire.valueType())
                    .as("the wire must record the runtime value type name")
                    .isEqualTo(String.class.getName());
            then(wire.value())
                    .as("the value node must be a scalar")
                    .isInstanceOf(IdentifierCodec.ValueNode.Scalar.class);
            IdentifierCodec.ValueNode.Scalar scalar = (IdentifierCodec.ValueNode.Scalar) wire.value();
            then(scalar.type())
                    .as("a String identifier must be normalized to the String scalar type name")
                    .isEqualTo(String.class.getName());
            then(scalar.value())
                    .as("the scalar must carry the raw string value verbatim")
                    .isEqualTo("abc");
        }

        @Test
        @DisplayName("Encodes a Long-based identifier into a scalar wire node")
        void ToWire_ShouldEncodeLongIdentifier_WhenIdentifierIsLongBased() {
            // given
            LongId identifier = new LongId(42L);

            // when
            IdentifierCodec.WireFormat wire = IdentifierCodec.toWire(identifier);

            // then
            IdentifierCodec.ValueNode.Scalar scalar = (IdentifierCodec.ValueNode.Scalar) wire.value();
            then(wire.valueType())
                    .as("value type must report the Long runtime class name")
                    .isEqualTo(Long.class.getName());
            then(scalar.type())
                    .as("a Long identifier must be normalized to the Long scalar type name")
                    .isEqualTo(Long.class.getName());
            then(scalar.value())
                    .as("the scalar must carry the string form of the Long value")
                    .isEqualTo("42");
        }

        @Test
        @DisplayName("Encodes an Integer-based identifier into a scalar wire node")
        void ToWire_ShouldEncodeIntegerIdentifier_WhenIdentifierIsIntegerBased() {
            // given
            IntId identifier = new IntId(7);

            // when
            IdentifierCodec.WireFormat wire = IdentifierCodec.toWire(identifier);

            // then
            IdentifierCodec.ValueNode.Scalar scalar = (IdentifierCodec.ValueNode.Scalar) wire.value();
            then(wire.valueType())
                    .as("value type must report the Integer runtime class name")
                    .isEqualTo(Integer.class.getName());
            then(scalar.type())
                    .as("an Integer identifier must be normalized to the Integer scalar type name")
                    .isEqualTo(Integer.class.getName());
            then(scalar.value())
                    .as("the scalar must carry the string form of the Integer value")
                    .isEqualTo("7");
        }

        @Test
        @DisplayName("Encodes a UUID-based identifier into a scalar wire node")
        void ToWire_ShouldEncodeUuidIdentifier_WhenIdentifierIsUuidBased() {
            // given
            UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UuidId identifier = new UuidId(uuid);

            // when
            IdentifierCodec.WireFormat wire = IdentifierCodec.toWire(identifier);

            // then
            IdentifierCodec.ValueNode.Scalar scalar = (IdentifierCodec.ValueNode.Scalar) wire.value();
            then(wire.valueType())
                    .as("value type must report the UUID runtime class name")
                    .isEqualTo(UUID.class.getName());
            then(scalar.type())
                    .as("a UUID identifier must be normalized to the UUID scalar type name")
                    .isEqualTo(UUID.class.getName());
            then(scalar.value())
                    .as("the scalar must carry the UUID canonical toString form")
                    .isEqualTo(uuid.toString());
        }

        @Test
        @DisplayName("Encodes a composite record value into a composite wire node")
        void ToWire_ShouldEncodeCompositeRecord_WhenIdentifierHoldsRecordValue() {
            // given
            CompositeValue value = new CompositeValue("alice", 3);
            CompositeId identifier = new CompositeId(value);

            // when
            IdentifierCodec.WireFormat wire = IdentifierCodec.toWire(identifier);

            // then
            then(wire.value())
                    .as("a record value must be encoded as a composite node")
                    .isInstanceOf(IdentifierCodec.ValueNode.Composite.class);
            IdentifierCodec.ValueNode.Composite composite = (IdentifierCodec.ValueNode.Composite) wire.value();
            then(composite.type())
                    .as("the composite node must record the concrete record type name")
                    .isEqualTo(CompositeValue.class.getName());
            then(composite.fields())
                    .as("the composite must contain a field for each record component in declaration order")
                    .containsOnlyKeys("name", "count");
            then(((IdentifierCodec.ValueNode.Scalar) composite.fields().get("name")).value())
                    .as("the nested name field must carry the string value")
                    .isEqualTo("alice");
            then(((IdentifierCodec.ValueNode.Scalar) composite.fields().get("count")).value())
                    .as("the nested count field must carry the string form of the integer")
                    .isEqualTo("3");
        }

        @Test
        @DisplayName("Throws DomainException when identifier is null")
        @SuppressWarnings("ConstantConditions")
        void ToWire_ShouldThrowDomainException_WhenIdentifierIsNull() {
            // given
            Identifier<?> identifier = null;

            // when & then
            thenThrownBy(() -> IdentifierCodec.toWire(identifier))
                    .as("a null identifier must be rejected by toWire")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The identifier cannot be null");
        }

        @Test
        @DisplayName("Throws DomainException when identifier value is unsupported")
        void ToWire_ShouldThrowDomainException_WhenValueIsUnsupportedScalar() {
            // given
            DoubleId identifier = new DoubleId(1.5);

            // when & then
            thenThrownBy(() -> IdentifierCodec.toWire(identifier))
                    .as("a Double value is not a supported scalar and must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Unsupported identifier value type");
        }

        @Test
        @DisplayName("Throws DomainException when record component is null")
        void ToWire_ShouldThrowDomainException_WhenRecordComponentIsNull() {
            // given
            CompositeValue value = new CompositeValue("alice", null);
            CompositeId identifier = new CompositeId(value);

            // when & then
            thenThrownBy(() -> IdentifierCodec.toWire(identifier))
                    .as("a null record component must be rejected during encoding")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("cannot be null");
        }

        @Test
        @DisplayName("Throws DomainException when record accessor throws")
        void ToWire_ShouldThrowDomainException_WhenRecordAccessorThrows() {
            // given
            ThrowingAccessorId identifier = new ThrowingAccessorId();

            // when & then
            thenThrownBy(() -> IdentifierCodec.toWire(identifier))
                    .as("a record accessor that throws must surface as a DomainException during encoding")
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("Throws DomainException when identifier value is null")
        void ToWire_ShouldThrowDomainException_WhenIdentifierValueIsNull() {
            // given
            StringId identifier = new StringId(null);

            // when & then
            thenThrownBy(() -> IdentifierCodec.toWire(identifier))
                    .as("a null identifier value must be rejected by requireValid")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The identifier value cannot be null");
        }

        @Test
        @DisplayName("Throws DomainException when identifier value is blank")
        void ToWire_ShouldThrowDomainException_WhenIdentifierValueIsBlank() {
            // given
            StringId identifier = new StringId("   ");

            // when & then
            thenThrownBy(() -> IdentifierCodec.toWire(identifier))
                    .as("a blank string identifier value must be rejected by requireValid")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The identifier value cannot be blank");
        }
    }

    @Nested
    @DisplayName("fromWire(WireFormat, Class)")
    class FromWireTests {

        @Test
        @DisplayName("Reconstitutes a String-based identifier from a valid scalar wire")
        void FromWire_ShouldReconstituteStringId_WhenWireIsValidStringScalar() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    StringId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when
            StringId decoded = IdentifierCodec.fromWire(wire, StringId.class);

            // then
            then(decoded)
                    .as("fromWire must produce a StringId instance")
                    .isNotNull();
            then(decoded.value())
                    .as("the decoded identifier must carry the original string value")
                    .isEqualTo("abc");
        }

        @Test
        @DisplayName("Reconstitutes a Long-based identifier from a valid scalar wire")
        void FromWire_ShouldReconstituteLongId_WhenWireIsValidLongScalar() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    LongId.class.getName(),
                    Long.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Long.class.getName(), "42"));

            // when
            LongId decoded = IdentifierCodec.fromWire(wire, LongId.class);

            // then
            then(decoded.value())
                    .as("the decoded identifier must carry the parsed Long value")
                    .isEqualTo(42L);
        }

        @Test
        @DisplayName("Reconstitutes an Integer-based identifier from a valid scalar wire")
        void FromWire_ShouldReconstituteIntId_WhenWireIsValidIntegerScalar() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    IntId.class.getName(),
                    Integer.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Integer.class.getName(), "7"));

            // when
            IntId decoded = IdentifierCodec.fromWire(wire, IntId.class);

            // then
            then(decoded.value())
                    .as("the decoded identifier must carry the parsed Integer value")
                    .isEqualTo(7);
        }

        @Test
        @DisplayName("Reconstitutes a UUID-based identifier from a valid scalar wire")
        void FromWire_ShouldReconstituteUuidId_WhenWireIsValidUuidScalar() {
            // given
            UUID uuid = UUID.fromString("11111111-2222-3333-4444-555555555555");
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    UuidId.class.getName(),
                    UUID.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(UUID.class.getName(), uuid.toString()));

            // when
            UuidId decoded = IdentifierCodec.fromWire(wire, UuidId.class);

            // then
            then(decoded.value())
                    .as("the decoded identifier must carry the parsed UUID")
                    .isEqualTo(uuid);
        }

        @Test
        @DisplayName("Reconstitutes a composite-record identifier from a valid composite wire")
        void FromWire_ShouldReconstituteCompositeId_WhenWireIsValidComposite() {
            // given
            Map<String, IdentifierCodec.ValueNode> fields = new LinkedHashMap<>();
            fields.put("name", new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "alice"));
            fields.put("count", new IdentifierCodec.ValueNode.Scalar(Integer.class.getName(), "3"));
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    CompositeId.class.getName(),
                    CompositeValue.class.getName(),
                    new IdentifierCodec.ValueNode.Composite(CompositeValue.class.getName(), fields));

            // when
            CompositeId decoded = IdentifierCodec.fromWire(wire, CompositeId.class);

            // then
            then(decoded.value())
                    .as("the decoded composite must preserve every field value")
                    .isEqualTo(new CompositeValue("alice", 3));
        }

        @Test
        @DisplayName("Coerces a Long wire value into an Integer identifier")
        void FromWire_ShouldCoerceLongToInteger_WhenWireDeclaresLongButIdentifierExpectsInteger() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    IntId.class.getName(),
                    Integer.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Long.class.getName(), "42"));

            // when
            IntId decoded = IdentifierCodec.fromWire(wire, IntId.class);

            // then
            then(decoded.value())
                    .as("the Long wire value must be narrowed to Integer during coercion")
                    .isEqualTo(42);
        }

        @Test
        @DisplayName("Coerces an Integer wire value into a Long identifier")
        void FromWire_ShouldCoerceIntegerToLong_WhenWireDeclaresIntegerButIdentifierExpectsLong() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    LongId.class.getName(),
                    Long.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Integer.class.getName(), "42"));

            // when
            LongId decoded = IdentifierCodec.fromWire(wire, LongId.class);

            // then
            then(decoded.value())
                    .as("the Integer wire value must be widened to Long during coercion")
                    .isEqualTo(42L);
        }

        @Test
        @DisplayName("Coerces a boxed Long into a primitive long constructor parameter")
        void FromWire_ShouldCoerceBoxedLongToPrimitiveLong_WhenIdentifierConstructorExpectsLong() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    PrimitiveLongId.class.getName(),
                    Long.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Long.class.getName(), "42"));

            // when
            PrimitiveLongId decoded = IdentifierCodec.fromWire(wire, PrimitiveLongId.class);

            // then
            then(decoded.value())
                    .as("the decoded identifier must unwrap the boxed Long into the primitive long constructor")
                    .isEqualTo(42L);
        }

        @Test
        @DisplayName("Coerces a boxed Integer into a primitive int constructor parameter")
        void FromWire_ShouldCoerceBoxedIntegerToPrimitiveInt_WhenIdentifierConstructorExpectsInt() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    PrimitiveIntId.class.getName(),
                    Integer.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Integer.class.getName(), "7"));

            // when
            PrimitiveIntId decoded = IdentifierCodec.fromWire(wire, PrimitiveIntId.class);

            // then
            then(decoded.value())
                    .as("the decoded identifier must unwrap the boxed Integer into the primitive int constructor")
                    .isEqualTo(7);
        }

        @Test
        @DisplayName("Coerces a boxed Integer into a primitive long constructor through the Number branch")
        void FromWire_ShouldCoerceBoxedIntegerToPrimitiveLong_WhenIdentifierConstructorExpectsLong() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    PrimitiveLongId.class.getName(),
                    Integer.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Long.class.getName(), "42"));

            // when
            PrimitiveLongId decoded = IdentifierCodec.fromWire(wire, PrimitiveLongId.class);

            // then
            then(decoded.value())
                    .as("an Integer value must be widened to a primitive long through the Number coercion branch")
                    .isEqualTo(42L);
        }

        @Test
        @DisplayName("Coerces a boxed Long into a primitive int constructor through the Number branch")
        void FromWire_ShouldCoerceBoxedLongToPrimitiveInt_WhenIdentifierConstructorExpectsInt() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    PrimitiveIntId.class.getName(),
                    Long.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Integer.class.getName(), "7"));

            // when
            PrimitiveIntId decoded = IdentifierCodec.fromWire(wire, PrimitiveIntId.class);

            // then
            then(decoded.value())
                    .as("a Long value must be narrowed to a primitive int through the Number coercion branch")
                    .isEqualTo(7);
        }

        @Test
        @DisplayName("Coerces a non-String scalar into a String value type")
        void FromWire_ShouldCoerceNonStringScalarToString_WhenValueTypeIsString() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    StringId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Long.class.getName(), "42"));

            // when
            StringId decoded = IdentifierCodec.fromWire(wire, StringId.class);

            // then
            then(decoded.value())
                    .as("a Long scalar must be stringified into a String value type")
                    .isEqualTo("42");
        }

        @Test
        @DisplayName("Coerces a String scalar into a UUID value type")
        void FromWire_ShouldCoerceStringToUuid_WhenValueTypeIsUuid() {
            // given
            UUID uuid = UUID.fromString("11111111-2222-3333-4444-555555555555");
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    UuidId.class.getName(),
                    UUID.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), uuid.toString()));

            // when
            UuidId decoded = IdentifierCodec.fromWire(wire, UuidId.class);

            // then
            then(decoded.value())
                    .as("a String scalar must be parsed into a UUID value type")
                    .isEqualTo(uuid);
        }

        @Test
        @DisplayName("Reconstitutes via single-argument constructor")
        void FromWire_ShouldReconstituteViaSingleArgConstructor_WhenNoRecordOrFactoryAvailable() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    ConstructorOnlyId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when
            ConstructorOnlyId decoded = IdentifierCodec.fromWire(wire, ConstructorOnlyId.class);

            // then
            then(decoded.value())
                    .as("the identifier must be instantiated through its single-argument constructor")
                    .isEqualTo("abc");
        }

        @Test
        @DisplayName("Reconstitutes via static factory named 'of'")
        void FromWire_ShouldReconstituteViaStaticFactoryOf_WhenNoRecordOrConstructorAvailable() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    FactoryOfId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when
            FactoryOfId decoded = IdentifierCodec.fromWire(wire, FactoryOfId.class);

            // then
            then(decoded.value())
                    .as("the identifier must be created through the static factory 'of'")
                    .isEqualTo("abc");
        }

        @Test
        @DisplayName("Reconstitutes via static factory named 'valueOf'")
        void FromWire_ShouldReconstituteViaStaticFactoryValueOf_WhenNoRecordOrConstructorAvailable() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    FactoryValueOfId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when
            FactoryValueOfId decoded = IdentifierCodec.fromWire(wire, FactoryValueOfId.class);

            // then
            then(decoded.value())
                    .as("the identifier must be created through the static factory 'valueOf'")
                    .isEqualTo("abc");
        }

        @Test
        @DisplayName("Reconstitutes via static factory named 'from'")
        void FromWire_ShouldReconstituteViaStaticFactoryFrom_WhenNoRecordOrConstructorAvailable() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    FactoryFromId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when
            FactoryFromId decoded = IdentifierCodec.fromWire(wire, FactoryFromId.class);

            // then
            then(decoded.value())
                    .as("the identifier must be created through the static factory 'from'")
                    .isEqualTo("abc");
        }

        @Test
        @DisplayName("Reconstitutes identifier via the static factory when single-arg constructors are inaccessible")
        void FromWire_ShouldUseStaticFactory_WhenNoAccessibleSingleArgConstructor() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    PureFactoryId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when
            PureFactoryId decoded = IdentifierCodec.fromWire(wire, PureFactoryId.class);

            // then
            then(decoded.value())
                    .as("the factory must be invoked when no accessible single-arg constructor exists")
                    .isEqualTo("abc");
        }

        @Test
        @DisplayName("Throws DomainException when wire is null")
        @SuppressWarnings("ConstantConditions")
        void FromWire_ShouldThrowDomainException_WhenWireIsNull() {
            // given
            IdentifierCodec.WireFormat wire = null;

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, StringId.class))
                    .as("a null wire format must be rejected by fromWire")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The wire format cannot be null");
        }

        @Test
        @DisplayName("Throws DomainException when expected type is null")
        @SuppressWarnings("ConstantConditions")
        void FromWire_ShouldThrowDomainException_WhenExpectedTypeIsNull() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    StringId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));
            Class<StringId> expectedType = null;

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, expectedType))
                    .as("a null expected type must be rejected by fromWire")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The expected type cannot be null");
        }

        @Test
        @DisplayName("Throws DomainException when identifier class is unknown on classpath")
        void FromWire_ShouldThrowDomainException_WhenIdentifierClassUnknown() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    "com.nonexistent.MissingIdentifier",
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, StringId.class))
                    .as("an unloadable identifier class must surface as a DomainException")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Unknown type on the classpath");
        }

        @Test
        @DisplayName("Throws DomainException when wire identifier type is not assignable to expected type")
        void FromWire_ShouldThrowDomainException_WhenIdentifierTypeMismatch() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    LongId.class.getName(),
                    Long.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Long.class.getName(), "42"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, StringId.class))
                    .as("a wire declaring a different identifier class than expected must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Identifier type mismatch");
        }

        @Test
        @DisplayName("Throws DomainException when wire scalar type is unsupported")
        void FromWire_ShouldThrowDomainException_WhenScalarTypeIsUnsupported() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    StringId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Double.class.getName(), "1.5"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, StringId.class))
                    .as("a wire declaring an unsupported scalar type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Unsupported scalar type");
        }

        @Test
        @DisplayName("Throws DomainException when wire declares a malformed UUID")
        void FromWire_ShouldThrowDomainException_WhenUuidMalformed() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    UuidId.class.getName(),
                    UUID.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(UUID.class.getName(), "not-a-uuid"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, UuidId.class))
                    .as("a malformed UUID scalar must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Invalid UUID identifier value");
        }

        @Test
        @DisplayName("Throws DomainException when wire declares a malformed Long")
        void FromWire_ShouldThrowDomainException_WhenLongMalformed() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    LongId.class.getName(),
                    Long.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Long.class.getName(), "not-a-long"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, LongId.class))
                    .as("a malformed Long scalar must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Invalid Long identifier value");
        }

        @Test
        @DisplayName("Throws DomainException when wire declares a malformed Integer")
        void FromWire_ShouldThrowDomainException_WhenIntegerMalformed() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    IntId.class.getName(),
                    Integer.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Integer.class.getName(), "not-an-int"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, IntId.class))
                    .as("a malformed Integer scalar must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Invalid Integer identifier value");
        }

        @Test
        @DisplayName("Throws DomainException when the parsed scalar cannot be coerced to the declared value type")
        void FromWire_ShouldThrowDomainException_WhenScalarCannotBeCoerced() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    StringId.class.getName(),
                    java.util.List.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, StringId.class))
                    .as("a wire whose valueType cannot accept the parsed scalar must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot coerce scalar value of type");
        }

        @Test
        @DisplayName("Throws DomainException when UUID value type receives a non-String scalar")
        void FromWire_ShouldThrowDomainException_WhenUuidValueTypeReceivesNonStringScalar() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    UuidId.class.getName(),
                    UUID.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(Long.class.getName(), "42"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, UuidId.class))
                    .as("a UUID value type must reject scalar values that are not String instances")
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("Throws DomainException when Long value type receives a non-Number value")
        void FromWire_ShouldThrowDomainException_WhenLongValueTypeReceivesNonNumberValue() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    LongId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, LongId.class))
                    .as("a Long identifier must reject decoded values that are not Number instances")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Throws DomainException when Integer value type receives a non-Number value")
        void FromWire_ShouldThrowDomainException_WhenIntegerValueTypeReceivesNonNumberValue() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    IntId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, IntId.class))
                    .as("an Integer identifier must reject decoded values that are not Number instances")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Throws DomainException when a scalar value cannot be coerced to a non-scalar target type")
        void FromWire_ShouldThrowDomainException_WhenScalarCannotBeCoercedToNonScalarTarget() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    DoubleId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, DoubleId.class))
                    .as("a String value that cannot be coerced to the declared DoubleBased identifier must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Throws DomainException when both value and target types are non-scalar")
        void FromWire_ShouldThrowDomainException_WhenBothValueAndTargetTypesAreNonScalar() {
            // given
            IdentifierCodec.WireFormat wire = buildWireFormat(SecondValueIdentifier.class, "name", "abc", "1");

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, SecondValueIdentifier.class))
                    .as("when neither the value nor the target type is a supported scalar, coercion must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Throws DomainException when composite wire type is not a record")
        void FromWire_ShouldThrowDomainException_WhenCompositeTypeIsNotRecord() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    StringId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Composite(String.class.getName(), Map.of()));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, StringId.class))
                    .as("a composite wire whose type is not a record must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Composite wire value must refer to a record type");
        }

        @Test
        @DisplayName("Throws DomainException when composite type is not assignable to expected type")
        void FromWire_ShouldThrowDomainException_WhenCompositeTypeMismatch() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    CompositeId.class.getName(),
                    CompositeValue.class.getName(),
                    new IdentifierCodec.ValueNode.Composite(OtherRecord.class.getName(), Map.of()));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, CompositeId.class))
                    .as("a composite whose type is not assignable to the expected type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Composite type mismatch");
        }

        @Test
        @DisplayName("Throws DomainException when composite field count does not match record components")
        void FromWire_ShouldThrowDomainException_WhenCompositeFieldCountMismatches() {
            // given
            Map<String, IdentifierCodec.ValueNode> fields = new LinkedHashMap<>();
            fields.put("name", new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "alice"));
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    CompositeId.class.getName(),
                    CompositeValue.class.getName(),
                    new IdentifierCodec.ValueNode.Composite(CompositeValue.class.getName(), fields));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, CompositeId.class))
                    .as("a composite with a mismatched field count must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Composite field count mismatch");
        }

        @Test
        @DisplayName("Throws DomainException when composite is missing a required field")
        void FromWire_ShouldThrowDomainException_WhenCompositeMissingField() {
            // given
            IdentifierCodec.WireFormat wire = buildWireFormat(CompositeId.class, "wrongName", "alice", "3");

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, CompositeId.class))
                    .as("a composite missing a required record component must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Missing composite field");
        }

        @Test
        @DisplayName("Throws DomainException when composite record canonical constructor throws")
        void FromWire_ShouldThrowDomainException_WhenCompositeConstructorThrows() {
            // given
            Map<String, IdentifierCodec.ValueNode> fields = new LinkedHashMap<>();
            fields.put("name", new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    ThrowingCompositeId.class.getName(),
                    ThrowingCompositeValue.class.getName(),
                    new IdentifierCodec.ValueNode.Composite(ThrowingCompositeValue.class.getName(), fields));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, ThrowingCompositeId.class))
                    .as("a composite record whose canonical constructor throws must surface as a DomainException")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Failed to instantiate composite value type "
                            + ThrowingCompositeValue.class.getName());
        }

        @Test
        @DisplayName("Throws DomainException when identifier factory returns null instead of an Identifier")
        void FromWire_ShouldThrowDomainException_WhenFactoryReturnsNullInsteadOfIdentifier() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    NullFactoryId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, NullFactoryId.class))
                    .as("a factory that returns null must be treated as unusable and fall back to a final failure")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Throws DomainException when a static single-arg Identifier method has a non-factory name")
        void FromWire_ShouldThrowDomainException_WhenStaticMethodNameIsNotRecognizedFactory() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    NonFactoryStaticId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, NonFactoryStaticId.class))
                    .as("a static single-arg Identifier method not named of/valueOf/from must not be used as a factory")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Throws DomainException when the only candidate method is non-static")
        void FromWire_ShouldThrowDomainException_WhenOnlyCandidateMethodIsNonStatic() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    NoStaticFactoryId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, NoStaticFactoryId.class))
                    .as("an identifier with no static factory method must fall through to the final failure")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Throws DomainException when a static factory method has a wrong parameter count")
        void FromWire_ShouldThrowDomainException_WhenStaticFactoryHasWrongParameterCount() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    WrongParamCountFactoryId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, WrongParamCountFactoryId.class))
                    .as("a static factory with zero parameters must be ignored, leading to final failure")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Throws DomainException when a static single-arg method returns a non-Identifier type")
        void FromWire_ShouldThrowDomainException_WhenStaticSingleArgMethodReturnsNonIdentifier() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    NonIdentifierReturnFactoryId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, NonIdentifierReturnFactoryId.class))
                    .as("a static method returning a non-Identifier type must not be treated as a factory")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Throws DomainException when static factory parameter cannot be coerced")
        void FromWire_ShouldThrowDomainException_WhenStaticFactoryParameterCannotBeCoerced() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    IncorrigibleParamFactoryId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, IncorrigibleParamFactoryId.class))
                    .as("a static factory whose parameter type rejects the decoded value must be skipped")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Throws DomainException when static factory throws during invocation")
        void FromWire_ShouldThrowDomainException_WhenStaticFactoryThrowsDuringInvocation() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    ThrowingFactoryId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, ThrowingFactoryId.class))
                    .as("a static factory that throws during invocation must be caught and treated as unusable")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Throws DomainException when static factory returns an identifier that fails requireValid")
        void FromWire_ShouldThrowDomainException_WhenFactoryReturnsInvalidIdentifier() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    InvalidValueFactoryId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, InvalidValueFactoryId.class))
                    .as("a factory returning an identifier whose value fails requireValid must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Returns null from record strategy when the record has more than one component")
        void FromWire_ShouldReturnNullFromRecordStrategy_WhenRecordHasMoreThanOneComponent() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    TwoComponentId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, TwoComponentId.class))
                    .as("a two-component record must bypass the record-canonical strategy and end in failure")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Falls back from record strategy when the canonical constructor throws")
        void FromWire_ShouldFallbackFromRecordStrategy_WhenCanonicalConstructorThrows() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    ThrowingCanonicalConstructorId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "bad"));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, ThrowingCanonicalConstructorId.class))
                    .as("a record whose canonical constructor throws must fall back to other strategies and fail")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }

        @Test
        @DisplayName("Falls back from record strategy when the resulting value fails requireValid")
        void FromWire_ShouldFallbackFromRecordStrategy_WhenRecordValueFailsRequireValid() {
            // given
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    StringId.class.getName(),
                    String.class.getName(),
                    new IdentifierCodec.ValueNode.Scalar(String.class.getName(), ""));

            // when & then
            thenThrownBy(() -> IdentifierCodec.fromWire(wire, StringId.class))
                    .as("a record whose value fails requireValid must fall back and end in failure")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot instantiate Identifier type");
        }
    }

    @Nested
    @DisplayName("WireFormat")
    class WireFormatTests {

        @Test
        @DisplayName("Creates a wire format with all fields preserved")
        void WireFormat_ShouldCreateInstance_WhenValidArgumentsProvided() {
            // given
            IdentifierCodec.ValueNode value = new IdentifierCodec.ValueNode.Scalar(
                    String.class.getName(), "abc");

            // when
            IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                    StringId.class.getName(), String.class.getName(), value);

            // then
            then(wire.identifierType())
                    .as("the identifier type must be preserved exactly")
                    .isEqualTo(StringId.class.getName());
            then(wire.valueType())
                    .as("the value type must be preserved exactly")
                    .isEqualTo(String.class.getName());
            then(wire.value())
                    .as("the value node must be preserved as the same instance")
                    .isSameAs(value);
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws when identifierType is null")
        void WireFormat_ShouldThrowDomainException_WhenIdentifierTypeIsNull() {
            // given
            IdentifierCodec.ValueNode value = new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc");

            // when & then
            thenThrownBy(() -> new IdentifierCodec.WireFormat(null, String.class.getName(), value))
                    .as("a null identifier type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The identifierType cannot be null or blank");
        }

        @Test
        @DisplayName("Throws when identifierType is empty")
        void WireFormat_ShouldThrowDomainException_WhenIdentifierTypeIsEmpty() {
            // given
            IdentifierCodec.ValueNode value = new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc");

            // when & then
            thenThrownBy(() -> new IdentifierCodec.WireFormat("", String.class.getName(), value))
                    .as("an empty identifier type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The identifierType cannot be null or blank");
        }

        @Test
        @DisplayName("Throws when identifierType is blank")
        void WireFormat_ShouldThrowDomainException_WhenIdentifierTypeIsBlank() {
            // given
            IdentifierCodec.ValueNode value = new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc");

            // when & then
            thenThrownBy(() -> new IdentifierCodec.WireFormat("   ", String.class.getName(), value))
                    .as("a blank identifier type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The identifierType cannot be null or blank");
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws when valueType is null")
        void WireFormat_ShouldThrowDomainException_WhenValueTypeIsNull() {
            // given
            IdentifierCodec.ValueNode value = new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc");

            // when & then
            thenThrownBy(() -> new IdentifierCodec.WireFormat(StringId.class.getName(), null, value))
                    .as("a null value type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The valueType cannot be null or blank");
        }

        @Test
        @DisplayName("Throws when valueType is empty")
        void WireFormat_ShouldThrowDomainException_WhenValueTypeIsEmpty() {
            // given
            IdentifierCodec.ValueNode value = new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc");

            // when & then
            thenThrownBy(() -> new IdentifierCodec.WireFormat(StringId.class.getName(), "", value))
                    .as("an empty value type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The valueType cannot be null or blank");
        }

        @Test
        @DisplayName("Throws when valueType is blank")
        void WireFormat_ShouldThrowDomainException_WhenValueTypeIsBlank() {
            // given
            IdentifierCodec.ValueNode value = new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "abc");

            // when & then
            thenThrownBy(() -> new IdentifierCodec.WireFormat(StringId.class.getName(), "   ", value))
                    .as("a blank value type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The valueType cannot be null or blank");
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws when value is null")
        void WireFormat_ShouldThrowDomainException_WhenValueIsNull() {
            // when & then
            thenThrownBy(() -> new IdentifierCodec.WireFormat(
                    StringId.class.getName(), String.class.getName(), null))
                    .as("a null value node must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The value cannot be null");
        }
    }

    @Nested
    @DisplayName("ValueNode.Scalar")
    class ScalarTests {

        @Test
        @DisplayName("Creates a scalar node with type and value preserved")
        void Scalar_ShouldCreateInstance_WhenValidArgumentsProvided() {
            // when
            IdentifierCodec.ValueNode.Scalar scalar = new IdentifierCodec.ValueNode.Scalar(
                    String.class.getName(), "abc");

            // then
            then(scalar.type())
                    .as("the scalar type must be preserved")
                    .isEqualTo(String.class.getName());
            then(scalar.value())
                    .as("the scalar value must be preserved")
                    .isEqualTo("abc");
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws when scalar type is null")
        void Scalar_ShouldThrowDomainException_WhenTypeIsNull() {
            // when & then
            thenThrownBy(() -> new IdentifierCodec.ValueNode.Scalar(null, "abc"))
                    .as("a null scalar type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The scalar type cannot be null or blank");
        }

        @Test
        @DisplayName("Throws when scalar type is empty")
        void Scalar_ShouldThrowDomainException_WhenTypeIsEmpty() {
            // when & then
            thenThrownBy(() -> new IdentifierCodec.ValueNode.Scalar("", "abc"))
                    .as("an empty scalar type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The scalar type cannot be null or blank");
        }

        @Test
        @DisplayName("Throws when scalar type is blank")
        void Scalar_ShouldThrowDomainException_WhenTypeIsBlank() {
            // when & then
            thenThrownBy(() -> new IdentifierCodec.ValueNode.Scalar("   ", "abc"))
                    .as("a blank scalar type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The scalar type cannot be null or blank");
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws when scalar value is null")
        void Scalar_ShouldThrowDomainException_WhenValueIsNull() {
            // when & then
            thenThrownBy(() -> new IdentifierCodec.ValueNode.Scalar(String.class.getName(), null))
                    .as("a null scalar value must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The scalar value cannot be null");
        }
    }

    @Nested
    @DisplayName("ValueNode.Composite")
    class CompositeTests {

        @Test
        @DisplayName("Creates a composite node with fields preserved")
        void Composite_ShouldCreateInstance_WhenValidArgumentsProvided() {
            // given
            Map<String, IdentifierCodec.ValueNode> fields = new LinkedHashMap<>();
            fields.put("name", new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "alice"));

            // when
            IdentifierCodec.ValueNode.Composite composite = new IdentifierCodec.ValueNode.Composite(
                    CompositeValue.class.getName(), fields);

            // then
            then(composite.type())
                    .as("the composite type must be preserved")
                    .isEqualTo(CompositeValue.class.getName());
            then(composite.fields())
                    .as("the composite fields must contain the provided entries")
                    .containsOnlyKeys("name");
        }

        @Test
        @DisplayName("Protects the fields map against external modification")
        void Composite_ShouldMakeFieldsImmutable_WhenCreated() {
            // given
            Map<String, IdentifierCodec.ValueNode> source = new LinkedHashMap<>();
            source.put("name", new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "alice"));

            // when
            IdentifierCodec.ValueNode.Composite composite = new IdentifierCodec.ValueNode.Composite(
                    CompositeValue.class.getName(), source);

            // then
            thenThrownBy(() -> composite.fields().put(
                    "x", new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "x")))
                    .as("the composite fields must be unmodifiable to protect immutability")
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Detaches the fields map from the source")
        void Composite_ShouldDetachFieldsFromSource_WhenCreated() {
            // given
            Map<String, IdentifierCodec.ValueNode> source = new LinkedHashMap<>();
            source.put("name", new IdentifierCodec.ValueNode.Scalar(String.class.getName(), "alice"));

            // when
            IdentifierCodec.ValueNode.Composite composite = new IdentifierCodec.ValueNode.Composite(
                    CompositeValue.class.getName(), source);
            source.put("count", new IdentifierCodec.ValueNode.Scalar(Integer.class.getName(), "3"));

            // then
            then(composite.fields())
                    .as("later mutations of the source map must not leak into the composite node")
                    .containsOnlyKeys("name");
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws when composite type is null")
        void Composite_ShouldThrowDomainException_WhenTypeIsNull() {
            // when & then
            thenThrownBy(() -> new IdentifierCodec.ValueNode.Composite(null, Map.of()))
                    .as("a null composite type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The composite type cannot be null or blank");
        }

        @Test
        @DisplayName("Throws when composite type is empty")
        void Composite_ShouldThrowDomainException_WhenTypeIsEmpty() {
            // when & then
            thenThrownBy(() -> new IdentifierCodec.ValueNode.Composite("", Map.of()))
                    .as("an empty composite type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The composite type cannot be null or blank");
        }

        @Test
        @DisplayName("Throws when composite type is blank")
        void Composite_ShouldThrowDomainException_WhenTypeIsBlank() {
            // when & then
            thenThrownBy(() -> new IdentifierCodec.ValueNode.Composite("   ", Map.of()))
                    .as("a blank composite type must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The composite type cannot be null or blank");
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws when composite fields is null")
        void Composite_ShouldThrowDomainException_WhenFieldsIsNull() {
            // when & then
            thenThrownBy(() -> new IdentifierCodec.ValueNode.Composite(CompositeValue.class.getName(), null))
                    .as("a null composite fields map must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The composite fields cannot be null");
        }
    }

    // Helpers and test doubles below all nested test classes

    private static IdentifierCodec.@NonNull WireFormat buildWireFormat(
            Class<?> identifierClass, String firstFieldName, String firstFieldValue, String countValue) {

        Map<String, IdentifierCodec.ValueNode> fields = new LinkedHashMap<>();
        fields.put(firstFieldName, new IdentifierCodec.ValueNode.Scalar(String.class.getName(), firstFieldValue));
        fields.put("count", new IdentifierCodec.ValueNode.Scalar(Integer.class.getName(), countValue));

        return new IdentifierCodec.WireFormat(
                identifierClass.getName(),
                CompositeValue.class.getName(),
                new IdentifierCodec.ValueNode.Composite(CompositeValue.class.getName(), fields));
    }

    record StringId(String value) implements Identifier<String> {
    }

    record LongId(Long value) implements Identifier<Long> {
    }

    record IntId(Integer value) implements Identifier<Integer> {
    }

    record UuidId(UUID value) implements Identifier<UUID> {
    }

    record CompositeValue(String name, Integer count) implements Serializable {
    }

    record CompositeId(CompositeValue value) implements Identifier<CompositeValue> {
    }

    record OtherRecord(String value) implements Serializable {
    }

    record DoubleId(Double value) implements Identifier<Double> {

        @Serial
        private static final long serialVersionUID = -5403938107805858732L;
    }

    record ConstructorOnlyId(String value) implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = 659713735756609830L;
    }

    record FactoryOfId(String value) implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = -1995782811726300221L;

        static FactoryOfId of(String value) {
            return new FactoryOfId(value);
        }
    }

    record FactoryValueOfId(String value) implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = 8671030806738042646L;

        static FactoryValueOfId valueOf(String value) {
            return new FactoryValueOfId(value);
        }
    }

    record FactoryFromId(String value) implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = 618079932497028562L;

        static FactoryFromId from(String value) {
            return new FactoryFromId(value);
        }
    }

    static final class ThrowingAccessorId implements Identifier<ThrowingValue> {

        @Serial
        private static final long serialVersionUID = 6755632635780519577L;

        @Override
        public ThrowingValue value() {
            return new ThrowingValue("x");
        }
    }

    record ThrowingValue(String value) implements Serializable {

        @Override
        public String value() {
            throw new IllegalStateException("accessor boom");
        }
    }

    static final class PrimitiveLongId implements Identifier<Long> {

        @Serial
        private static final long serialVersionUID = 5528851974925232535L;

        private final long value;

        PrimitiveLongId(long value) {
            this.value = value;
        }

        @Override
        public Long value() {
            return value;
        }
    }

    static final class PrimitiveIntId implements Identifier<Integer> {

        @Serial
        private static final long serialVersionUID = 3786083774724324331L;

        private final int value;

        PrimitiveIntId(int value) {
            this.value = value;
        }

        @Override
        public Integer value() {
            return value;
        }
    }

    record SecondValueIdentifier(OtherRecord value) implements Identifier<OtherRecord> {

        @Serial
        private static final long serialVersionUID = 918348953067761020L;
    }

    static final class NullFactoryId implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = -250210915992738256L;

        private NullFactoryId() {
        }

        static NullFactoryId of(String value) {
            return null;
        }

        @Override
        public String value() {
            return "static";
        }
    }

    static final class NonFactoryStaticId implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = -5014792435096915747L;

        private NonFactoryStaticId() {
        }

        static NonFactoryStaticId create(String value) {
            return new NonFactoryStaticId();
        }

        @Override
        public String value() {
            return "static";
        }
    }

    static final class NoStaticFactoryId implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = 8417521735781992283L;

        private NoStaticFactoryId() {
        }

        @Override
        public String value() {
            return "static";
        }
    }

    static final class WrongParamCountFactoryId implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = -6040028746552325068L;

        private WrongParamCountFactoryId() {
        }

        static WrongParamCountFactoryId of() {
            return new WrongParamCountFactoryId();
        }

        @Override
        public String value() {
            return "static";
        }
    }

    static final class NonIdentifierReturnFactoryId implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = 2269474477800738456L;

        private NonIdentifierReturnFactoryId() {
        }

        static String of(String value) {
            return value;
        }

        @Override
        public String value() {
            return "static";
        }
    }

    static final class IncorrigibleParamFactoryId implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = -2128078694791887444L;

        private IncorrigibleParamFactoryId() {
        }

        static IncorrigibleParamFactoryId of(java.util.List<String> value) {
            return new IncorrigibleParamFactoryId();
        }

        @Override
        public String value() {
            return "static";
        }
    }

    static final class ThrowingFactoryId implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = -3152831261193123814L;

        private ThrowingFactoryId() {
        }

        static ThrowingFactoryId of(String value) {
            throw new IllegalStateException("factory boom");
        }

        @Override
        public String value() {
            return "static";
        }
    }

    static final class InvalidValueFactoryId implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = -7489136244998131577L;

        private InvalidValueFactoryId() {
        }

        static InvalidValueFactoryId of(String value) {
            return new InvalidValueFactoryId();
        }

        @Override
        public String value() {
            return null;
        }
    }

    static final class PureFactoryId implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = -8718164071809515066L;

        private final String value;

        private PureFactoryId() {
            this.value = "placeholder";
        }

        private PureFactoryId(String value) {
            this.value = value;
        }

        static PureFactoryId of(String value) {
            return new PureFactoryId(value);
        }

        @Override
        public String value() {
            return value;
        }
    }

    record TwoComponentId(String value, Integer count) implements Identifier<String> {
    }

    record ThrowingCanonicalConstructorId(String value) implements Identifier<String> {

        ThrowingCanonicalConstructorId {
            if ("bad".equals(value)) {
                throw new IllegalArgumentException("canonical constructor boom");
            }
        }
    }

    record ThrowingCompositeValue(String name) implements Serializable {

        ThrowingCompositeValue {
            throw new IllegalStateException("composite boom");
        }
    }

    record ThrowingCompositeId(ThrowingCompositeValue value) implements Identifier<ThrowingCompositeValue> {

        @Serial
        private static final long serialVersionUID = 8438426993083574914L;
    }
}
