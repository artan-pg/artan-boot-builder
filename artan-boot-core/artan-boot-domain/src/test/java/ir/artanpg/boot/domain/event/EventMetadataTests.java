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

package ir.artanpg.boot.domain.event;

import ir.artanpg.boot.domain.exception.DomainEventException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.entry;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link EventMetadata}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("EventMetadata Unit Tests")
class EventMetadataTests {

    @Test
    @DisplayName("empty() should return shared empty instance")
    void empty_ShouldReturnSharedEmptyInstance_WhenCalled() {
        // given
        // when
        EventMetadata result1 = EventMetadata.empty();
        EventMetadata result2 = EventMetadata.empty();

        // then
        then(result1).isSameAs(result2);
        then(result1.isEmpty()).isTrue();
        then(result1.getCorrelationId()).isNull();
        then(result1.getCausationId()).isNull();
        then(result1.getTenant()).isNull();
        then(result1.getTraceId()).isNull();
        then(result1.getSpanId()).isNull();
        then(result1.getUserId()).isNull();
        then(result1.getProperties()).isEmpty();
    }

    @Test
    @DisplayName("builder() should return a new non-null Builder instance")
    void builder_ShouldReturnNewBuilderInstance_WhenCalled() {
        // given
        // when
        EventMetadata.Builder builder1 = EventMetadata.builder();
        EventMetadata.Builder builder2 = EventMetadata.builder();

        // then
        then(builder1).isNotNull();
        then(builder2).isNotNull();
        then(builder1).isNotSameAs(builder2);
    }

    @Nested
    @DisplayName("Getters")
    class GettersTests {

        @Test
        @DisplayName("getCorrelationId should return set value")
        void getCorrelationId_ShouldReturnValue_WhenSet() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .correlationId("corr-123")
                    .build();

            // when
            String result = metadata.getCorrelationId();

            // then
            then(result).isEqualTo("corr-123");
        }

        @Test
        @DisplayName("getCorrelationId should return null when not set")
        void getCorrelationId_ShouldReturnNull_WhenNotSet() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when
            String result = metadata.getCorrelationId();

            // then
            then(result).isNull();
        }

        @Test
        @DisplayName("getCausationId should return set value")
        void getCausationId_ShouldReturnValue_WhenSet() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .causationId("cause-456")
                    .build();

            // when
            String result = metadata.getCausationId();

            // then
            then(result).isEqualTo("cause-456");
        }

        @Test
        @DisplayName("getCausationId should return null when not set")
        void getCausationId_ShouldReturnNull_WhenNotSet() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when
            String result = metadata.getCausationId();

            // then
            then(result).isNull();
        }

        @Test
        @DisplayName("getTenant should return set value")
        void getTenant_ShouldReturnValue_WhenSet() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .tenant("tenant-A")
                    .build();

            // when
            String result = metadata.getTenant();

            // then
            then(result).isEqualTo("tenant-A");
        }

        @Test
        @DisplayName("getTenant should return null when not set")
        void getTenant_ShouldReturnNull_WhenNotSet() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when
            String result = metadata.getTenant();

            // then
            then(result).isNull();
        }

        @Test
        @DisplayName("getTraceId should return set value")
        void getTraceId_ShouldReturnValue_WhenSet() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .traceId("trace-789")
                    .build();

            // when
            String result = metadata.getTraceId();

            // then
            then(result).isEqualTo("trace-789");
        }

        @Test
        @DisplayName("getTraceId should return null when not set")
        void getTraceId_ShouldReturnNull_WhenNotSet() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when
            String result = metadata.getTraceId();

            // then
            then(result).isNull();
        }

        @Test
        @DisplayName("getSpanId should return set value")
        void getSpanId_ShouldReturnValue_WhenSet() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .spanId("span-101")
                    .build();

            // when
            String result = metadata.getSpanId();

            // then
            then(result).isEqualTo("span-101");
        }

        @Test
        @DisplayName("getSpanId should return null when not set")
        void getSpanId_ShouldReturnNull_WhenNotSet() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when
            String result = metadata.getSpanId();

            // then
            then(result).isNull();
        }

        @Test
        @DisplayName("getUserId should return set value")
        void getUserId_ShouldReturnValue_WhenSet() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .userId("user-999")
                    .build();

            // when
            String result = metadata.getUserId();

            // then
            then(result).isEqualTo("user-999");
        }

        @Test
        @DisplayName("getUserId should return null when not set")
        void getUserId_ShouldReturnNull_WhenNotSet() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when
            String result = metadata.getUserId();

            // then
            then(result).isNull();
        }

        @Test
        @DisplayName("getProperties should return unmodifiable empty map when no properties")
        void getProperties_ShouldReturnEmptyUnmodifiableMap_WhenNoProperties() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when
            Map<String, Object> result = metadata.getProperties();

            // then
            then(result).isEmpty();
            thenThrownBy(() -> result.put("k", "v"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("getProperties should return unmodifiable snapshot of properties")
        void getProperties_ShouldReturnUnmodifiableSnapshot_WhenPropertiesPresent() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .properties("key1", "value1")
                    .properties("key2", 42)
                    .build();

            // when
            Map<String, Object> result = metadata.getProperties();

            // then
            then(result).containsExactly(entry("key1", "value1"), entry("key2", 42));
            thenThrownBy(() -> result.put("new", "val")).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("isEmpty()")
    class IsEmptyTests {

        @Test
        @DisplayName("isEmpty should return true for empty instance")
        void isEmpty_ShouldReturnTrue_WhenEmptyInstance() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when
            boolean result = metadata.isEmpty();

            // then
            then(result).isTrue();
        }

        @Test
        @DisplayName("isEmpty should return false when any standard field is set")
        void isEmpty_ShouldReturnFalse_WhenAnyStandardFieldIsSet() {
            // given
            EventMetadata withCorrelation = EventMetadata.builder().correlationId("c").build();
            EventMetadata withCausation = EventMetadata.builder().causationId("c").build();
            EventMetadata withTenant = EventMetadata.builder().tenant("t").build();
            EventMetadata withTrace = EventMetadata.builder().traceId("t").build();
            EventMetadata withSpan = EventMetadata.builder().spanId("s").build();
            EventMetadata withUser = EventMetadata.builder().userId("u").build();

            // when / then
            then(withCorrelation.isEmpty()).isFalse();
            then(withCausation.isEmpty()).isFalse();
            then(withTenant.isEmpty()).isFalse();
            then(withTrace.isEmpty()).isFalse();
            then(withSpan.isEmpty()).isFalse();
            then(withUser.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("isEmpty should return false when properties are present")
        void isEmpty_ShouldReturnFalse_WhenPropertiesPresent() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .properties("key", "value")
                    .build();

            // when
            boolean result = metadata.isEmpty();

            // then
            then(result).isFalse();
        }
    }

    @Nested
    @DisplayName("has* methods")
    class HasMethodsTests {

        @Test
        @DisplayName("hasCorrelationId should return true when set")
        void hasCorrelationId_ShouldReturnTrue_WhenSet() {
            // given
            EventMetadata metadata = EventMetadata.builder().correlationId("c").build();

            // when / then
            then(metadata.hasCorrelationId()).isTrue();
        }

        @Test
        @DisplayName("hasCorrelationId should return false when not set")
        void hasCorrelationId_ShouldReturnFalse_WhenNotSet() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when / then
            then(metadata.hasCorrelationId()).isFalse();
        }

        @Test
        @DisplayName("hasCausationId should return true when set")
        void hasCausationId_ShouldReturnTrue_WhenSet() {
            // given
            EventMetadata metadata = EventMetadata.builder().causationId("c").build();

            // when / then
            then(metadata.hasCausationId()).isTrue();
        }

        @Test
        @DisplayName("hasCausationId should return false when not set")
        void hasCausationId_ShouldReturnFalse_WhenNotSet() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when / then
            then(metadata.hasCausationId()).isFalse();
        }

        @Test
        @DisplayName("hasTenant should return true when set")
        void hasTenant_ShouldReturnTrue_WhenSet() {
            // given
            EventMetadata metadata = EventMetadata.builder().tenant("t").build();

            // when / then
            then(metadata.hasTenant()).isTrue();
        }

        @Test
        @DisplayName("hasTenant should return false when not set")
        void hasTenant_ShouldReturnFalse_WhenNotSet() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when / then
            then(metadata.hasTenant()).isFalse();
        }

        @Test
        @DisplayName("hasTraceId should return true when set")
        void hasTraceId_ShouldReturnTrue_WhenSet() {
            // given
            EventMetadata metadata = EventMetadata.builder().traceId("t").build();

            // when / then
            then(metadata.hasTraceId()).isTrue();
        }

        @Test
        @DisplayName("hasTraceId should return false when not set")
        void hasTraceId_ShouldReturnFalse_WhenNotSet() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when / then
            then(metadata.hasTraceId()).isFalse();
        }

        @Test
        @DisplayName("hasSpanId should return true when set")
        void hasSpanId_ShouldReturnTrue_WhenSet() {
            // given
            EventMetadata metadata = EventMetadata.builder().spanId("s").build();

            // when / then
            then(metadata.hasSpanId()).isTrue();
        }

        @Test
        @DisplayName("hasSpanId should return false when not set")
        void hasSpanId_ShouldReturnFalse_WhenNotSet() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when / then
            then(metadata.hasSpanId()).isFalse();
        }

        @Test
        @DisplayName("hasUserId should return true when set")
        void hasUserId_ShouldReturnTrue_WhenSet() {
            // given
            EventMetadata metadata = EventMetadata.builder().userId("u").build();

            // when / then
            then(metadata.hasUserId()).isTrue();
        }

        @Test
        @DisplayName("hasUserId should return false when not set")
        void hasUserId_ShouldReturnFalse_WhenNotSet() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when / then
            then(metadata.hasUserId()).isFalse();
        }
    }

    @Nested
    @DisplayName("getProperty & hasProperty")
    class PropertyAccessTests {

        @Test
        @DisplayName("getProperty should return value when key exists")
        void getProperty_ShouldReturnValue_WhenKeyExists() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .properties("name", "Alice")
                    .build();

            // when
            Object result = metadata.getProperty("name");

            // then
            then(result).isEqualTo("Alice");
        }

        @Test
        @DisplayName("getProperty should return null when key does not exist")
        void getProperty_ShouldReturnNull_WhenKeyDoesNotExist() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .properties("name", "Alice")
                    .build();

            // when
            Object result = metadata.getProperty("missing");

            // then
            then(result).isNull();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("getProperty should return null when key is null")
        void getProperty_ShouldReturnNull_WhenKeyIsNull() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .properties("name", "Alice")
                    .build();

            // when
            Object result = metadata.getProperty(null);

            // then
            then(result).isNull();
        }

        @Test
        @DisplayName("hasProperty should return true when key exists")
        void hasProperty_ShouldReturnTrue_WhenKeyExists() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .properties("name", "Alice")
                    .build();

            // when / then
            then(metadata.hasProperty("name")).isTrue();
        }

        @Test
        @DisplayName("hasProperty should return false when key does not exist")
        void hasProperty_ShouldReturnFalse_WhenKeyDoesNotExist() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .properties("name", "Alice")
                    .build();

            // when / then
            then(metadata.hasProperty("missing")).isFalse();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("hasProperty should return false when key is null")
        void hasProperty_ShouldReturnFalse_WhenKeyIsNull() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .properties("name", "Alice")
                    .build();

            // when / then
            then(metadata.hasProperty(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("toBuilder()")
    class ToBuilderTests {

        @Test
        @DisplayName("toBuilder should produce builder with same values")
        void toBuilder_ShouldProduceEquivalentMetadata_WhenBuilt() {
            // given
            EventMetadata original = EventMetadata.builder()
                    .correlationId("corr")
                    .causationId("cause")
                    .tenant("tenant")
                    .traceId("trace")
                    .spanId("span")
                    .userId("user")
                    .properties("k1", "v1")
                    .properties("k2", 100)
                    .build();

            // when
            EventMetadata copy = original.toBuilder().build();

            // then
            then(copy).isEqualTo(original);
            then(copy).isNotSameAs(original);
            then(copy.getCorrelationId()).isEqualTo("corr");
            then(copy.getCausationId()).isEqualTo("cause");
            then(copy.getTenant()).isEqualTo("tenant");
            then(copy.getTraceId()).isEqualTo("trace");
            then(copy.getSpanId()).isEqualTo("span");
            then(copy.getUserId()).isEqualTo("user");
            then(copy.getProperties()).containsExactly(entry("k1", "v1"), entry("k2", 100)
            );
        }

        @Test
        @DisplayName("toBuilder should produce empty metadata when source is empty")
        void toBuilder_ShouldProduceEmptyMetadata_WhenSourceIsEmpty() {
            // given
            EventMetadata original = EventMetadata.empty();

            // when
            EventMetadata copy = original.toBuilder().build();

            // then
            then(copy).isSameAs(EventMetadata.empty());
            then(copy.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("toBuilder should allow modification without affecting original")
        void toBuilder_ShouldAllowModification_WithoutAffectingOriginal() {
            // given
            EventMetadata original = EventMetadata.builder()
                    .correlationId("original")
                    .properties("key", "value")
                    .build();

            // when
            EventMetadata modified = original.toBuilder()
                    .correlationId("modified")
                    .properties("key", "new-value")
                    .build();

            // then
            then(original.getCorrelationId()).isEqualTo("original");
            then(original.getProperty("key")).isEqualTo("value");
            then(modified.getCorrelationId()).isEqualTo("modified");
            then(modified.getProperty("key")).isEqualTo("new-value");
        }
    }

    @Nested
    @DisplayName("equals, hashCode, toString")
    class ObjectMethodsTests {

        @SuppressWarnings("EqualsWithItself")
        @Test
        @DisplayName("equals should return true for same instance")
        void equals_ShouldReturnTrue_WhenSameInstance() {
            // given
            EventMetadata metadata = EventMetadata.builder().correlationId("c").build();

            // when / then
            then(metadata.equals(metadata)).isTrue();
        }

        @Test
        @DisplayName("equals should return true for equal instances")
        void equals_ShouldReturnTrue_WhenEqualInstances() {
            // given
            EventMetadata m1 = EventMetadata.builder()
                    .correlationId("c")
                    .causationId("cause")
                    .tenant("t")
                    .traceId("tr")
                    .spanId("s")
                    .userId("u")
                    .properties("k", "v")
                    .build();
            EventMetadata m2 = EventMetadata.builder()
                    .correlationId("c")
                    .causationId("cause")
                    .tenant("t")
                    .traceId("tr")
                    .spanId("s")
                    .userId("u")
                    .properties("k", "v")
                    .build();

            // when / then
            then(m1).isEqualTo(m2);
            then(m1.hashCode()).isEqualTo(m2.hashCode());
        }

        @Test
        @DisplayName("equals should return false when any field differs")
        void equals_ShouldReturnFalse_WhenAnyFieldDiffers() {
            // given
            EventMetadata base = EventMetadata.builder()
                    .correlationId("c")
                    .causationId("cause")
                    .tenant("t")
                    .traceId("tr")
                    .spanId("s")
                    .userId("u")
                    .properties("k", "v")
                    .build();

            // when / then
            then(base).isNotEqualTo(EventMetadata.builder().correlationId("other").build());
            then(base).isNotEqualTo(EventMetadata.builder().correlationId("c").causationId("other").build());

            then(base).isNotEqualTo(
                    EventMetadata.builder()
                            .correlationId("c").causationId("cause").tenant("other").build());

            then(base).isNotEqualTo(
                    EventMetadata.builder()
                            .correlationId("c").causationId("cause").tenant("t").traceId("other").build());

            then(base).isNotEqualTo(
                    EventMetadata.builder()
                            .correlationId("c").causationId("cause").tenant("t").traceId("tr").spanId("other").build());

            then(base).isNotEqualTo(
                    EventMetadata.builder()
                            .correlationId("c")
                            .causationId("cause")
                            .tenant("t")
                            .traceId("tr")
                            .spanId("s")
                            .userId("other")
                            .build());

            then(base).isNotEqualTo(
                    EventMetadata.builder()
                            .correlationId("c")
                            .causationId("cause")
                            .tenant("t")
                            .traceId("tr")
                            .spanId("s")
                            .userId("u")
                            .properties("k", "other")
                            .build());

            then(base).isNotEqualTo(EventMetadata.empty());
            then(base).isNotEqualTo(null);
            then(base).isNotEqualTo("not-an-EventMetadata");
        }

        @Test
        @DisplayName("equals should return true for two empty instances")
        void equals_ShouldReturnTrue_WhenBothEmpty() {
            // given
            EventMetadata e1 = EventMetadata.empty();
            EventMetadata e2 = EventMetadata.builder().build();

            // when / then
            then(e1).isEqualTo(e2);
            then(e1.hashCode()).isEqualTo(e2.hashCode());
        }

        @Test
        @DisplayName("hashCode should be consistent with equals")
        void hashCode_ShouldBeConsistentWithEquals_WhenCalled() {
            // given
            EventMetadata m1 = EventMetadata.builder().correlationId("c").build();
            EventMetadata m2 = EventMetadata.builder().correlationId("c").build();

            // when / then
            then(m1.hashCode()).isEqualTo(m2.hashCode());
        }

        @Test
        @DisplayName("toString should contain all fields")
        void toString_ShouldContainAllFields_WhenCalled() {
            // given
            EventMetadata metadata = EventMetadata.builder()
                    .correlationId("corr")
                    .causationId("cause")
                    .tenant("tenant")
                    .traceId("trace")
                    .spanId("span")
                    .userId("user")
                    .properties("k", "v")
                    .build();

            // when
            String result = metadata.toString();

            // then
            then(result).contains("EventMetadata[");
            then(result).contains("correlationId='corr'");
            then(result).contains("causationId='cause'");
            then(result).contains("tenant='tenant'");
            then(result).contains("traceId='trace'");
            then(result).contains("spanId='span'");
            then(result).contains("userId='user'");
            then(result).contains("properties=");
        }

        @Test
        @DisplayName("toString should handle empty instance")
        void toString_ShouldHandleEmptyInstance_WhenCalled() {
            // given
            EventMetadata metadata = EventMetadata.empty();

            // when
            String result = metadata.toString();

            // then
            then(result).contains("EventMetadata[");
            then(result).contains("correlationId='null'");
            then(result).contains("properties=");
        }

        @Test
        @DisplayName("toString should use safeString when property toString throws")
        void toString_ShouldUseSafeString_WhenPropertyToStringThrows() {
            // given
            Object badObject = new Object() {
                @Override
                public String toString() {
                    throw new RuntimeException("boom");
                }
            };
            EventMetadata metadata = EventMetadata.builder()
                    .properties("bad", badObject)
                    .build();

            // when
            String result = metadata.toString();

            // then
            then(result).contains("EventMetadata[");
            then(result).contains("properties=");
            // safeString fallback: ClassName@identityHash
            then(result).contains("@");
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("build should return empty shared instance when nothing is set")
        void build_ShouldReturnEmptySharedInstance_WhenNothingIsSet() {
            // given
            EventMetadata.Builder builder = EventMetadata.builder();

            // when
            EventMetadata result = builder.build();

            // then
            then(result).isSameAs(EventMetadata.empty());
            then(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("build should create new instance when any field is set")
        void build_ShouldCreateNewInstance_WhenAnyFieldIsSet() {
            // given
            EventMetadata.Builder builder = EventMetadata.builder()
                    .correlationId("c");

            // when
            EventMetadata result = builder.build();

            // then
            then(result).isNotSameAs(EventMetadata.empty());
            then(result.getCorrelationId()).isEqualTo("c");
        }

        @Test
        @DisplayName("correlationId should set the value")
        void correlationId_ShouldSetValue_WhenCalled() {
            // given / when
            EventMetadata metadata = EventMetadata.builder()
                    .correlationId("corr-id")
                    .build();

            // then
            then(metadata.getCorrelationId()).isEqualTo("corr-id");
        }

        @Test
        @DisplayName("causationId should set the value")
        void causationId_ShouldSetValue_WhenCalled() {
            // given / when
            EventMetadata metadata = EventMetadata.builder()
                    .causationId("cause-id")
                    .build();

            // then
            then(metadata.getCausationId()).isEqualTo("cause-id");
        }

        @Test
        @DisplayName("tenant should set the value")
        void tenant_ShouldSetValue_WhenCalled() {
            // given / when
            EventMetadata metadata = EventMetadata.builder()
                    .tenant("my-tenant")
                    .build();

            // then
            then(metadata.getTenant()).isEqualTo("my-tenant");
        }

        @Test
        @DisplayName("traceId should set the value")
        void traceId_ShouldSetValue_WhenCalled() {
            // given / when
            EventMetadata metadata = EventMetadata.builder()
                    .traceId("trace-id")
                    .build();

            // then
            then(metadata.getTraceId()).isEqualTo("trace-id");
        }

        @Test
        @DisplayName("spanId should set the value")
        void spanId_ShouldSetValue_WhenCalled() {
            // given / when
            EventMetadata metadata = EventMetadata.builder()
                    .spanId("span-id")
                    .build();

            // then
            then(metadata.getSpanId()).isEqualTo("span-id");
        }

        @Test
        @DisplayName("userId should set the value")
        void userId_ShouldSetValue_WhenCalled() {
            // given / when
            EventMetadata metadata = EventMetadata.builder()
                    .userId("user-id")
                    .build();

            // then
            then(metadata.getUserId()).isEqualTo("user-id");
        }

        // ----- properties(String, Object) -----

        @Test
        @DisplayName("properties(key, value) should add property")
        void properties_ShouldAddProperty_WhenKeyAndValueProvided() {
            // given / when
            EventMetadata metadata = EventMetadata.builder()
                    .properties("name", "Bob")
                    .build();

            // then
            then(metadata.getProperty("name")).isEqualTo("Bob");
            then(metadata.hasProperty("name")).isTrue();
        }

        @Test
        @DisplayName("properties(key, value) should replace existing property")
        void properties_ShouldReplaceProperty_WhenKeyAlreadyExists() {
            // given / when
            EventMetadata metadata = EventMetadata.builder()
                    .properties("name", "Bob")
                    .properties("name", "Alice")
                    .build();

            // then
            then(metadata.getProperty("name")).isEqualTo("Alice");
        }

        @Test
        @DisplayName("properties(key, null) should remove existing property")
        void properties_ShouldRemoveProperty_WhenValueIsNull() {
            // given / when
            EventMetadata metadata = EventMetadata.builder()
                    .properties("name", "Bob")
                    .properties("name", null)
                    .build();

            // then
            then(metadata.hasProperty("name")).isFalse();
            then(metadata.getProperties()).isEmpty();
        }

        @Test
        @DisplayName("properties(key, value) should throw DomainEventException when key is null")
        void properties_ShouldThrowDomainEventException_WhenKeyIsNull() {
            // given
            EventMetadata.Builder builder = EventMetadata.builder();

            // when / then
            thenThrownBy(() -> builder.properties(null, "value"))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The property key cannot be null or blank");
        }

        @Test
        @DisplayName("properties(key, value) should throw DomainEventException when key is empty")
        void properties_ShouldThrowDomainEventException_WhenKeyIsEmpty() {
            // given
            EventMetadata.Builder builder = EventMetadata.builder();

            // when / then
            thenThrownBy(() -> builder.properties("", "value"))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The property key cannot be null or blank");
        }

        @Test
        @DisplayName("properties(key, value) should throw DomainEventException when key is blank")
        void properties_ShouldThrowDomainEventException_WhenKeyIsBlank() {
            // given
            EventMetadata.Builder builder = EventMetadata.builder();

            // when / then
            thenThrownBy(() -> builder.properties("   ", "value"))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The property key cannot be null or blank");
        }

        // ----- properties(Map) -----

        @Test
        @DisplayName("properties(map) should replace all properties")
        void properties_ShouldReplaceAllProperties_WhenMapProvided() {
            // given / when
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("a", 1);
            props.put("b", "two");

            EventMetadata metadata = EventMetadata.builder()
                    .properties("old", "value")
                    .properties(props)
                    .build();

            // then
            then(metadata.getProperties()).containsExactly(entry("a", 1), entry("b", "two"));
            then(metadata.hasProperty("old")).isFalse();
        }

        @Test
        @DisplayName("properties(map) should ignore null values in map")
        void properties_ShouldIgnoreNullValues_WhenMapContainsNulls() {
            // given
            Map<String, Object> props = new HashMap<>();
            props.put("valid", "ok");
            props.put("nullValue", null);

            // when
            EventMetadata metadata = EventMetadata.builder()
                    .properties(props)
                    .build();

            // then
            then(metadata.getProperties()).containsExactly(entry("valid", "ok"));
            then(metadata.hasProperty("nullValue")).isFalse();
        }

        @Test
        @DisplayName("properties(map) should clear properties when null map is provided")
        void properties_ShouldClearProperties_WhenMapIsNull() {
            // given / when
            EventMetadata metadata = EventMetadata.builder()
                    .properties("existing", "value")
                    .properties(null)
                    .build();

            // then
            then(metadata.getProperties()).isEmpty();
            then(metadata.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("properties(map) should throw DomainEventException when map contains null key")
        void properties_ShouldThrowDomainEventException_WhenMapContainsNullKey() {
            // given
            Map<String, Object> props = new HashMap<>();
            props.put(null, "value");

            EventMetadata.Builder builder = EventMetadata.builder();

            // when / then
            thenThrownBy(() -> builder.properties(props))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The property key cannot be null or blank");
        }

        @Test
        @DisplayName("properties(map) should throw DomainEventException when map contains empty key")
        void properties_ShouldThrowDomainEventException_WhenMapContainsEmptyKey() {
            // given
            Map<String, Object> props = Map.of("", "value");

            EventMetadata.Builder builder = EventMetadata.builder();

            // when / then
            thenThrownBy(() -> builder.properties(props))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The property key cannot be null or blank");
        }

        @Test
        @DisplayName("properties(map) should throw DomainEventException when map contains blank key")
        void properties_ShouldThrowDomainEventException_WhenMapContainsBlankKey() {
            // given
            Map<String, Object> props = Map.of("   ", "value");

            EventMetadata.Builder builder = EventMetadata.builder();

            // when / then
            thenThrownBy(() -> builder.properties(props))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The property key cannot be null or blank");
        }

        @Test
        @DisplayName("properties(map) should accept empty map")
        void properties_ShouldAcceptEmptyMap_WhenProvided() {
            // given / when
            EventMetadata metadata = EventMetadata.builder()
                    .properties("old", "value")
                    .properties(Collections.emptyMap())
                    .build();

            // then
            then(metadata.getProperties()).isEmpty();
            then(metadata.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Builder methods should support fluent chaining")
        void builderMethods_ShouldSupportFluentChaining_WhenCalled() {
            // given / when
            EventMetadata metadata = EventMetadata.builder()
                    .correlationId("c")
                    .causationId("cause")
                    .tenant("t")
                    .traceId("tr")
                    .spanId("s")
                    .userId("u")
                    .properties("k1", "v1")
                    .properties("k2", 2)
                    .build();

            // then
            then(metadata.getCorrelationId()).isEqualTo("c");
            then(metadata.getCausationId()).isEqualTo("cause");
            then(metadata.getTenant()).isEqualTo("t");
            then(metadata.getTraceId()).isEqualTo("tr");
            then(metadata.getSpanId()).isEqualTo("s");
            then(metadata.getUserId()).isEqualTo("u");
            then(metadata.getProperties()).containsExactly(entry("k1", "v1"), entry("k2", 2)
            );
        }

        @Test
        @DisplayName("snapshotProperties should return emptyMap when source is empty")
        void snapshotProperties_ShouldReturnEmptyMap_WhenSourceIsEmpty() {
            // given / when
            // exercised via build() with no properties
            EventMetadata metadata = EventMetadata.builder()
                    .correlationId("only-standard")
                    .build();

            // then
            then(metadata.getProperties()).isSameAs(Collections.emptyMap());
        }

        @Test
        @DisplayName("snapshotProperties should preserve insertion order")
        void snapshotProperties_ShouldPreserveInsertionOrder_WhenMultipleProperties() {
            // given / when
            EventMetadata metadata = EventMetadata.builder()
                    .properties("first", 1)
                    .properties("second", 2)
                    .properties("third", 3)
                    .build();

            // then
            then(metadata.getProperties().keySet())
                    .containsExactly("first", "second", "third");
        }
    }
}
