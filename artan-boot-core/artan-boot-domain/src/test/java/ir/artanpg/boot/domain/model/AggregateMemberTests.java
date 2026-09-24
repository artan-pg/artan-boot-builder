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

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link AbstractAggregateMember}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("AbstractAggregateMember")
class AggregateMemberTests {

    public static final String IDENTIFIER_1 = "433f721d-02f8-489f-9248-97b7b5985b48";
    public static final String IDENTIFIER_2 = "fd865c37-2c9b-469c-879f-5f781cab461b";

    private static final String ID_NULL_EXCEPTION = "The identifier cannot be null";

    @Nested
    @DisplayName("Constructor with Identifier")
    class ConstructorWithIdentifier {

        @Test
        @DisplayName("should set id when valid identifier is provided")
        void constructor_ShouldSetId_WhenValidIdentifierIsProvided() {
            // given
            TestIdentifier id = createTestIdentifier();

            // when
            TestAggregateMember member = new TestAggregateMember(id);

            // then
            then(member.getId())
                    .as("Member id should match the provided identifier")
                    .isEqualTo(id);
        }

        @SuppressWarnings({"DataFlowIssue", "ConstantValue"})
        @Test
        @DisplayName("should throw DomainException when id is null")
        void constructor_ShouldThrowDomainException_WhenIdIsNull() {
            // given
            TestIdentifier nullId = null;

            // when & then
            thenThrownBy(() -> new TestAggregateMember(nullId))
                    .as("Should throw DomainException when id is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ID_NULL_EXCEPTION);
        }
    }

    @Nested
    @DisplayName("Constructor with Builder")
    class ConstructorWithBuilder {

        @Test
        @DisplayName("should set id from builder when valid builder is provided")
        void constructor_ShouldSetIdFromBuilder_WhenValidBuilderIsProvided() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregateMember.TestMemberBuilder builder = new TestAggregateMember.TestMemberBuilder()
                    .identifier(id);

            // when
            TestAggregateMember member = new TestAggregateMember(builder);

            // then
            then(member.getId())
                    .as("Member id should match the builder's identifier")
                    .isEqualTo(id);
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainException when builder is null")
        void constructor_ShouldThrowDomainException_WhenBuilderIsNull() {
            // given
            TestAggregateMember.TestMemberBuilder nullBuilder = null;

            // when & then
            thenThrownBy(() -> new TestAggregateMember(nullBuilder))
                    .as("Should throw DomainException when builder is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The builder cannot be null");
        }
    }

    @Nested
    @DisplayName("sameIdentityAs(AggregateMember)")
    class SameIdentityAsWithMember {

        @Test
        @DisplayName("should return true when both members have same identifier")
        void sameIdentityAs_ShouldReturnTrue_WhenBothMembersHaveSameIdentifier() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregateMember member1 = new TestAggregateMember(id);
            TestAggregateMember member2 = new TestAggregateMember(createTestIdentifier());

            // when
            boolean result = member1.sameIdentityAs(member2);

            // then
            then(result)
                    .as("Members with same identifier should have same identity")
                    .isTrue();
        }

        @Test
        @DisplayName("should return false when members have different identifiers")
        void sameIdentityAs_ShouldReturnFalse_WhenMembersHaveDifferentIdentifiers() {
            // given
            TestAggregateMember member1 = new TestAggregateMember(createTestIdentifier());
            TestAggregateMember member2 = new TestAggregateMember(new TestIdentifier(IDENTIFIER_2));

            // when
            boolean result = member1.sameIdentityAs(member2);

            // then
            then(result)
                    .as("Members with different identifiers should not have same identity")
                    .isFalse();
        }

        @Test
        @DisplayName("should return true when comparing member with itself")
        void sameIdentityAs_ShouldReturnTrue_WhenComparingMemberWithItself() {
            // given
            TestAggregateMember member = new TestAggregateMember(createTestIdentifier());

            // when
            boolean result = member.sameIdentityAs(member);

            // then
            then(result)
                    .as("Member should have same identity as itself")
                    .isTrue();
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should return false when other member is null")
        void sameIdentityAs_ShouldReturnFalse_WhenOtherMemberIsNull() {
            // given
            TestAggregateMember member = new TestAggregateMember(createTestIdentifier());
            AggregateMember<TestIdentifier> nullMember = null;

            // when
            boolean result = member.sameIdentityAs(nullMember);

            // then
            then(result)
                    .as("Member should not have same identity as null member")
                    .isFalse();
        }

        @Test
        @DisplayName("should be symmetric when comparing two members")
        void sameIdentityAs_ShouldBeSymmetric_WhenComparingTwoMembers() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregateMember member1 = new TestAggregateMember(id);
            TestAggregateMember member2 = new TestAggregateMember(createTestIdentifier());

            // when
            boolean result1 = member1.sameIdentityAs(member2);
            boolean result2 = member2.sameIdentityAs(member1);

            // then
            then(result1)
                    .as("Symmetry: member1.sameIdentityAs(member2)")
                    .isEqualTo(result2);
        }

        @Test
        @DisplayName("should return true when comparing with member of different class but same id")
        void sameIdentityAs_ShouldReturnTrue_WhenComparingWithMemberOfDifferentClassButSameId() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregateMember member1 = new TestAggregateMember(id);
            AnotherTestMember member2 = new AnotherTestMember(id);

            // when
            boolean result = member1.sameIdentityAs(member2);

            // then
            then(result)
                    .as("Members of different classes should not have same identity even with same id")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("sameIdentityAs(Identifier)")
    class SameIdentityAsWithIdentifier {

        @Test
        @DisplayName("should return true when identifier equals member's identifier")
        void sameIdentityAs_ShouldReturnTrue_WhenIdentifierEqualsMemberIdentifier() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregateMember member = new TestAggregateMember(id);
            TestIdentifier sameId = createTestIdentifier();

            // when
            boolean result = member.sameIdentityAs(sameId);

            // then
            then(result)
                    .as("Member should have same identity as equal identifier")
                    .isTrue();
        }

        @Test
        @DisplayName("should return false when identifier differs from member's identifier")
        void sameIdentityAs_ShouldReturnFalse_WhenIdentifierDiffersFromMemberIdentifier() {
            // given
            TestAggregateMember member = new TestAggregateMember(createTestIdentifier());
            TestIdentifier differentId = new TestIdentifier(IDENTIFIER_2);

            // when
            boolean result = member.sameIdentityAs(differentId);

            // then
            then(result)
                    .as("Member should not have same identity as different identifier")
                    .isFalse();
        }

        @Test
        @DisplayName("should return true when comparing with member's own identifier instance")
        void sameIdentityAs_ShouldReturnTrue_WhenComparingWithOwnIdentifierInstance() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregateMember member = new TestAggregateMember(id);

            // when
            boolean result = member.sameIdentityAs(id);

            // then
            then(result)
                    .as("Member should have same identity as its own identifier")
                    .isTrue();
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should return false when provided identifier is null")
        void sameIdentityAs_ShouldReturnFalse_WhenProvidedIdentifierIsNull() {
            // given
            TestAggregateMember member = new TestAggregateMember(createTestIdentifier());
            TestIdentifier nullId = null;

            // when
            boolean result = member.sameIdentityAs(nullId);

            // then
            then(result)
                    .as("Member should not have same identity as null identifier")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @SuppressWarnings({"EqualsWithItself", "ConstantValue"})
        @Test
        @DisplayName("should return true when comparing member with itself")
        void equals_ShouldReturnTrue_WhenComparingMemberWithItself() {
            // given
            TestAggregateMember member = createMember();

            // when
            boolean result = member.equals(member);

            // then
            then(result)
                    .as("Member should be equal to itself")
                    .isTrue();
        }

        @Test
        @DisplayName("should return true when comparing members with same id and same class")
        void equals_ShouldReturnTrue_WhenComparingMembersWithSameIdAndSameClass() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregateMember member1 = new TestAggregateMember(id);
            TestAggregateMember member2 = new TestAggregateMember(createTestIdentifier());

            // when
            boolean result = member1.equals(member2);

            // then
            then(result)
                    .as("Members with same id and same class should be equal")
                    .isTrue();
        }

        @Test
        @DisplayName("should return false when comparing members with different ids")
        void equals_ShouldReturnFalse_WhenComparingMembersWithDifferentIds() {
            // given
            TestAggregateMember member1 = new TestAggregateMember(createTestIdentifier());
            TestAggregateMember member2 = new TestAggregateMember(new TestIdentifier(IDENTIFIER_2));

            // when
            boolean result = member1.equals(member2);

            // then
            then(result)
                    .as("Members with different ids should not be equal")
                    .isFalse();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should return false when comparing with null")
        void equals_ShouldReturnFalse_WhenComparingWithNull() {
            // given
            TestAggregateMember member = createMember();

            // when
            boolean result = member.equals(null);

            // then
            then(result)
                    .as("Member should not be equal to null")
                    .isFalse();
        }

        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        @Test
        @DisplayName("should return false when comparing with different object type")
        void equals_ShouldReturnFalse_WhenComparingWithDifferentObjectType() {
            // given
            TestAggregateMember member = createMember();

            // when
            boolean result = member.equals("not a member");

            // then
            then(result)
                    .as("Member should not be equal to different object type")
                    .isFalse();
        }

        @Test
        @DisplayName("should return same hash code for equal members")
        void hashCode_ShouldReturnSameHashCode_ForEqualMembers() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregateMember member1 = new TestAggregateMember(id);
            TestAggregateMember member2 = new TestAggregateMember(createTestIdentifier());

            // when & then
            then(member1.hashCode())
                    .as("Equal members should have same hash code")
                    .isEqualTo(member2.hashCode());
        }

        @Test
        @DisplayName("should return different hash code for members with different ids")
        void hashCode_ShouldReturnDifferentHashCode_ForMembersWithDifferentIds() {
            // given
            TestAggregateMember member1 = new TestAggregateMember(createTestIdentifier());
            TestAggregateMember member2 = new TestAggregateMember(new TestIdentifier(IDENTIFIER_2));

            // when & then
            then(member1.hashCode())
                    .as("Members with different ids should have different hash codes")
                    .isNotEqualTo(member2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should return string representation with class name and id")
        void toString_ShouldReturnStringRepresentation_WhenCalled() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregateMember member = new TestAggregateMember(id);

            // when
            String result = member.toString();

            // then
            then(result)
                    .as("toString should contain class name and id")
                    .contains("AbstractAggregateMember")
                    .contains("id=433f721d-02f8-489f-9248-97b7b5985b48");
        }
    }

    @Nested
    @DisplayName("AbstractBuilder")
    class AbstractBuilderTests {

        @Test
        @DisplayName("should set identifier when provided")
        void builder_ShouldSetIdentifier_WhenProvided() {
            // given
            TestIdentifier id = createTestIdentifier();

            // when
            TestAggregateMember.TestMemberBuilder builder = new TestAggregateMember.TestMemberBuilder()
                    .identifier(id);
            TestAggregateMember member = builder.build();

            // then
            then(member.getId())
                    .as("Builder should set identifier correctly")
                    .isEqualTo(id);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when identifier is null")
        void builder_ShouldThrowDomainException_WhenIdentifierIsNull() {
            // given
            TestIdentifier nullId = null;

            // when & then
            thenThrownBy(() -> new TestAggregateMember.TestMemberBuilder().identifier(nullId))
                    .as("Should throw DomainException when identifier is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ID_NULL_EXCEPTION);
        }

        @Test
        @DisplayName("should support method chaining")
        void builder_ShouldSupportMethodChaining_WhenCalled() {
            // given
            TestIdentifier id = createTestIdentifier();

            // when
            TestAggregateMember.TestMemberBuilder builder = new TestAggregateMember.TestMemberBuilder()
                    .identifier(id);

            // then
            then(builder)
                    .as("Builder should support method chaining")
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("Inheritance")
    class Inheritance {

        @Test
        @DisplayName("should implement AggregateMember interface")
        void inheritance_ShouldImplementAggregateMemberInterface_WhenCreated() {
            // given
            TestAggregateMember member = createMember();

            // when & then
            then(member)
                    .as("AbstractAggregateMember should implement AggregateMember")
                    .isInstanceOf(AggregateMember.class);
        }

        @Test
        @DisplayName("should be Serializable")
        void inheritance_ShouldBeSerializable_WhenCreated() {
            // given
            TestAggregateMember member = createMember();

            // when & then
            then(member)
                    .as("AbstractAggregateMember should be Serializable")
                    .isInstanceOf(java.io.Serializable.class);
        }
    }

    // ========================================================================================
    // Helper Methods
    // ========================================================================================

    private TestIdentifier createTestIdentifier() {
        return new TestIdentifier(IDENTIFIER_1);
    }

    private TestAggregateMember createMember() {
        return new TestAggregateMember(createTestIdentifier());
    }

    // ========================================================================================
    // Test Doubles
    // ========================================================================================

    private record TestIdentifier(String value) implements Identifier<String> {

        @Override
        public @NonNull String value() {
            return this.value;
        }
    }

    private static class TestAggregateMember extends AbstractAggregateMember<TestIdentifier> {

        @Serial
        private static final long serialVersionUID = -839494936337590502L;

        TestAggregateMember(TestIdentifier id) {
            super(id);
        }

        TestAggregateMember(TestMemberBuilder builder) {
            super(builder);
        }

        static class TestMemberBuilder extends AbstractBuilder<TestIdentifier, TestAggregateMember, TestMemberBuilder> {

            @Override
            protected TestMemberBuilder self() {
                return this;
            }

            @Override
            public TestAggregateMember build() {
                return new TestAggregateMember(this);
            }
        }
    }

    private static class AnotherTestMember extends AbstractAggregateMember<TestIdentifier> {

        @Serial
        private static final long serialVersionUID = -1237215332304846317L;

        AnotherTestMember(TestIdentifier id) {
            super(id);
        }
    }
}
