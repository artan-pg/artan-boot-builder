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
import ir.artanpg.boot.domain.model.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link EventReconstitutionSupport}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("EventReconstitutionSupport")
class EventReconstitutionSupportTests {

    private static final EventType EVENT_TYPE = EventType.of("SampleEvent");
    private static final String EVENT_ID = "event-1";
    private static final Instant OCCURRED_AT = Instant.parse("2024-01-01T00:00:00Z");

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Throws UnsupportedOperationException when instantiated via reflection")
        void Constructor_ShouldThrowUnsupportedOperationException_WhenInvokedViaReflection() {
            // given
            Constructor<EventReconstitutionSupport> constructor;
            try {
                constructor = EventReconstitutionSupport.class.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            constructor.setAccessible(true);

            // when & then
            thenThrownBy(constructor::newInstance)
                    .as("the utility-class constructor must reject external instantiation")
                    .isInstanceOf(InvocationTargetException.class)
                    .cause()
                    .as("the reflective wrapper must expose the underlying UnsupportedOperationException")
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("reconstitute(Class, String, Instant, Identifier, Object, EventType, EventMetadata)")
    class ReconstituteTests {

        @SuppressWarnings("ConstantConditions")
        @Test
        @DisplayName("Throws when event class is null")
        void Reconstitute_ShouldThrowDomainEventException_WhenEventClassIsNull() {
            // given
            Class<SampleEvent> eventClass = null;
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    eventClass, EVENT_ID, OCCURRED_AT, aggregateId, "payload", EVENT_TYPE, null))
                    .as("a null event class must be rejected before any other validation")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The event class cannot be null");
        }

        @SuppressWarnings("ConstantConditions")
        @Test
        @DisplayName("Throws when event id is null")
        void Reconstitute_ShouldThrowDomainEventException_WhenEventIdIsNull() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    SampleEvent.class, null, OCCURRED_AT, aggregateId, "payload", EVENT_TYPE, null))
                    .as("a null event id must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The eventId cannot be null or blank");
        }

        @Test
        @DisplayName("Throws when event id is empty")
        void Reconstitute_ShouldThrowDomainEventException_WhenEventIdIsEmpty() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    SampleEvent.class, "", OCCURRED_AT, aggregateId, "payload", EVENT_TYPE, null))
                    .as("an empty event id must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The eventId cannot be null or blank");
        }

        @Test
        @DisplayName("Throws when event id is blank")
        void Reconstitute_ShouldThrowDomainEventException_WhenEventIdIsBlank() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    SampleEvent.class, "   ", OCCURRED_AT, aggregateId, "payload", EVENT_TYPE, null))
                    .as("a blank event id must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The eventId cannot be null or blank");
        }

        @SuppressWarnings("ConstantConditions")
        @Test
        @DisplayName("Throws when occurred at is null")
        void Reconstitute_ShouldThrowDomainEventException_WhenOccurredAtIsNull() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    SampleEvent.class, EVENT_ID, null, aggregateId, "payload", EVENT_TYPE, null))
                    .as("a null occurredAt must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The occurredAt cannot be null");
        }

        @SuppressWarnings("ConstantConditions")
        @Test
        @DisplayName("Throws when aggregate id is null")
        void Reconstitute_ShouldThrowDomainEventException_WhenAggregateIdIsNull() {
            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    SampleEvent.class, EVENT_ID, OCCURRED_AT, null, "payload", EVENT_TYPE, null))
                    .as("a null aggregate id must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The aggregateId cannot be null");
        }

        @SuppressWarnings("ConstantConditions")
        @Test
        @DisplayName("Throws when payload is null")
        void Reconstitute_ShouldThrowDomainEventException_WhenPayloadIsNull() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    SampleEvent.class, EVENT_ID, OCCURRED_AT, aggregateId, null, EVENT_TYPE, null))
                    .as("a null payload must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The payload cannot be null");
        }

        @SuppressWarnings("ConstantConditions")
        @Test
        @DisplayName("Throws when event type is null")
        void Reconstitute_ShouldThrowDomainEventException_WhenEventTypeIsNull() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    SampleEvent.class, EVENT_ID, OCCURRED_AT, aggregateId, "payload", null, null))
                    .as("a null event type must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The eventType cannot be null");
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("Throws when event class does not extend AbstractDomainEvent")
        void Reconstitute_ShouldThrowDomainEventException_WhenEventClassDoesNotExtendAbstractDomainEvent() {
            // given
            Class<DomainEvent<?, ?>> nonEventClass =
                    (Class<DomainEvent<?, ?>>) (Class<?>) NonEventClass.class;
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    nonEventClass, EVENT_ID, OCCURRED_AT, aggregateId, "payload", EVENT_TYPE, null))
                    .as("a class that does not extend AbstractDomainEvent must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("The event class must extend AbstractDomainEvent");
        }

        @Test
        @DisplayName("Returns reconstituted event when all arguments are valid")
        void Reconstitute_ShouldReturnEvent_WhenAllArgumentsAreValid() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");
            EventMetadata metadata = EventMetadata.builder().correlationId("corr-1").build();

            // when
            SampleEvent event = EventReconstitutionSupport.reconstitute(
                    SampleEvent.class, EVENT_ID, OCCURRED_AT, aggregateId, "payload", EVENT_TYPE, metadata);

            // then
            then(event.getEventId())
                    .as("the reconstituted event must preserve the supplied event id")
                    .isEqualTo(EVENT_ID);
            then(event.getOccurredAt())
                    .as("the reconstituted event must preserve the supplied occurrence time")
                    .isEqualTo(OCCURRED_AT);
            then(event.getAggregateId())
                    .as("the reconstituted event must preserve the exact aggregate identifier")
                    .isSameAs(aggregateId);
            then(event.getPayload())
                    .as("the reconstituted event must preserve the supplied payload")
                    .isEqualTo("payload");
            then(event.getEventType())
                    .as("the reconstituted event must preserve the supplied event type")
                    .isEqualTo(EVENT_TYPE);
            then(event.getMetadata())
                    .as("the reconstituted event must preserve the exact metadata instance")
                    .isSameAs(metadata);
        }

        @Test
        @DisplayName("Falls back to empty metadata when metadata is null")
        void Reconstitute_ShouldFallBackToEmptyMetadata_WhenMetadataIsNull() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when
            SampleEvent event = EventReconstitutionSupport.reconstitute(
                    SampleEvent.class, EVENT_ID, OCCURRED_AT, aggregateId, "payload", EVENT_TYPE, null);

            // then
            then(event.getMetadata())
                    .as("a null metadata argument must be normalized to the shared empty metadata")
                    .isEqualTo(EventMetadata.empty());
        }

        @Test
        @DisplayName("Throws when no reconstitution constructor is present")
        void Reconstitute_ShouldThrowDomainEventException_WhenNoReconstitutionConstructorIsPresent() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    NoReconstitutionConstructorEvent.class, EVENT_ID, OCCURRED_AT, aggregateId,
                    "payload", EVENT_TYPE, null))
                    .as("an event class without any reconstitution constructor must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("No reconstitution constructor found");
        }

        @Test
        @DisplayName("Skips constructors that do not match the reconstitution signature")
        void Reconstitute_ShouldSkipConstructors_WhenSignaturesDoNotMatchReconstitutionSignature() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    WrongSignaturesEvent.class, EVENT_ID, OCCURRED_AT, aggregateId,
                    "payload", EVENT_TYPE, null))
                    .as("every constructor with a mismatched signature must be skipped, ending with no match")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("No reconstitution constructor found");
        }

        @Test
        @DisplayName("Throws when constructor throws DomainEventException")
        void Reconstitute_ShouldRethrowDomainEventException_WhenConstructorThrowsDomainEventException() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    DomainThrowingEvent.class, EVENT_ID, OCCURRED_AT, aggregateId,
                    "payload", EVENT_TYPE, null))
                    .as("a DomainEventException thrown by the constructor must be rethrown as-is")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("constructor rejected");
        }

        @Test
        @DisplayName("Wraps non-DomainEventException from constructor")
        void Reconstitute_ShouldWrapException_WhenConstructorThrowsOtherException() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    RuntimeThrowingEvent.class, EVENT_ID, OCCURRED_AT, aggregateId,
                    "payload", EVENT_TYPE, null))
                    .as("a non-DomainEventException from the constructor must be wrapped in a DomainEventException")
                    .isInstanceOf(DomainEventException.class);
        }

        @Test
        @DisplayName("Throws DomainEventException when event class is abstract")
        void Reconstitute_ShouldThrowDomainEventException_WhenEventClassIsAbstract() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    AbstractSampleEvent.class, EVENT_ID, OCCURRED_AT, aggregateId,
                    "payload", EVENT_TYPE, null))
                    .as("an abstract event class cannot be instantiated and must be reported as a DomainEventException")
                    .isInstanceOf(DomainEventException.class);
        }

        @Test
        @DisplayName("Uses Object-payload fallback constructor when aggregate type is incompatible")
        void Reconstitute_ShouldUseFallbackConstructor_WhenAggregateIsIncompatibleAndPayloadIsObject() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    FallbackEvent.class, EVENT_ID, OCCURRED_AT, aggregateId,
                    "payload", EVENT_TYPE, null))
                    .as("the Object-payload fallback is picked but instantiation fails on the aggregate type mismatch")
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws when no constructor passes the String/Instant first-param check")
        void Reconstitute_ShouldThrowDomainEventException_WhenFirstParamIsNotString() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    FirstParamWrongEvent.class, EVENT_ID, OCCURRED_AT, aggregateId,
                    "payload", EVENT_TYPE, null))
                    .as("a constructor whose first parameter is not String must be skipped, ending with no match")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("No reconstitution constructor found");
        }

        @Test
        @DisplayName("Throws when aggregate matches but payload type is incompatible")
        void Reconstitute_ShouldThrowDomainEventException_WhenPayloadTypeIsIncompatible() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    PayloadIncompatibleEvent.class, EVENT_ID, OCCURRED_AT, aggregateId,
                    "payload", EVENT_TYPE, null))
                    .as("when aggregate matches but payload type is incompatible, the candidate must be skipped")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("No reconstitution constructor found");
        }

        @Test
        @DisplayName("Skips additional Object-payload fallbacks once one has already been recorded")
        void Reconstitute_ShouldSkipFallback_WhenFallbackAlreadyRecorded() {
            // given
            Identifier<String> aggregateId = new SampleIdentifier("agg-1");

            // when & then
            thenThrownBy(() -> EventReconstitutionSupport.reconstitute(
                    TwoFallbacksEvent.class, EVENT_ID, OCCURRED_AT, aggregateId, "payload", EVENT_TYPE, null))
                    .as("the second Object-payload fallback must not overwrite the first; "
                            + "invoking the recorded fallback fails on the aggregate type mismatch")
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // Helpers and test doubles below all nested test classes

    record SampleIdentifier(String value) implements Identifier<String> {
    }

    record SpecificIdentifier(String value) implements Identifier<String> {
    }

    static final class SampleEvent extends AbstractDomainEvent<SampleIdentifier, String> {
        SampleEvent(String eventId,
                    Instant occurredAt,
                    SampleIdentifier aggregateId,
                    String payload,
                    EventType eventType,
                    EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    abstract static class AbstractSampleEvent extends AbstractDomainEvent<SampleIdentifier, String> {
        AbstractSampleEvent(String eventId,
                            Instant occurredAt,
                            SampleIdentifier aggregateId,
                            String payload,
                            EventType eventType,
                            EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    static final class DomainThrowingEvent extends AbstractDomainEvent<SampleIdentifier, String> {
        DomainThrowingEvent(String eventId,
                            Instant occurredAt,
                            SampleIdentifier aggregateId,
                            String payload,
                            EventType eventType,
                            EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
            throw new DomainEventException("constructor rejected");
        }
    }

    static final class RuntimeThrowingEvent extends AbstractDomainEvent<SampleIdentifier, String> {
        RuntimeThrowingEvent(String eventId,
                             Instant occurredAt,
                             SampleIdentifier aggregateId,
                             String payload,
                             EventType eventType,
                             EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
            throw new IllegalStateException("boom");
        }
    }

    static final class NoReconstitutionConstructorEvent extends AbstractDomainEvent<SampleIdentifier, String> {
        NoReconstitutionConstructorEvent(SampleIdentifier aggregateId, String payload) {
            super(aggregateId, payload, EventType.of("NoRecon"));
        }
    }

    static final class WrongSignaturesEvent extends AbstractDomainEvent<SampleIdentifier, String> {

        WrongSignaturesEvent(SampleIdentifier aggregateId, String payload) {
            super(aggregateId, payload, EventType.of("WrongSig"));
        }

        WrongSignaturesEvent(String eventId,
                             String notInstant,
                             SampleIdentifier aggregateId,
                             String payload,
                             EventType eventType,
                             EventMetadata metadata) {
            super(eventId, Instant.now(), aggregateId, payload, eventType, metadata);
        }

        WrongSignaturesEvent(String eventId,
                             Instant occurredAt,
                             String notIdentifier,
                             String payload,
                             EventType eventType,
                             EventMetadata metadata) {
            super(eventId, occurredAt, new SampleIdentifier(notIdentifier), payload, eventType, metadata);
        }

        WrongSignaturesEvent(String eventId,
                             Instant occurredAt,
                             SampleIdentifier aggregateId,
                             String payload,
                             String notEventType,
                             EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, EventType.of("Fake"), metadata);
        }

        WrongSignaturesEvent(String eventId,
                             Instant occurredAt,
                             SampleIdentifier aggregateId,
                             String payload,
                             EventType eventType,
                             String notMetadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, EventMetadata.empty());
        }
    }

    static final class FallbackEvent extends AbstractDomainEvent<SpecificIdentifier, Object> {
        FallbackEvent(String eventId,
                      Instant occurredAt,
                      SpecificIdentifier aggregateId,
                      Object payload,
                      EventType eventType,
                      EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    static final class NonEventClass {
    }

    record AnotherIdentifier(String value) implements Identifier<String> {
    }

    static final class FirstParamWrongEvent extends AbstractDomainEvent<SampleIdentifier, String> {

        FirstParamWrongEvent(SampleIdentifier aggregateId, String payload) {
            super(aggregateId, payload, EventType.of("FirstWrong"));
        }

        FirstParamWrongEvent(Integer notEventId,
                             Instant occurredAt,
                             SampleIdentifier aggregateId,
                             String payload,
                             EventType eventType,
                             EventMetadata metadata) {
            super("id", occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    static final class PayloadIncompatibleEvent extends AbstractDomainEvent<SampleIdentifier, String> {

        PayloadIncompatibleEvent(SampleIdentifier aggregateId, String payload) {
            super(aggregateId, payload, EventType.of("PayloadIncompatible"));
        }

        PayloadIncompatibleEvent(String eventId,
                                 Instant occurredAt,
                                 SampleIdentifier aggregateId,
                                 Integer notTheRightPayload,
                                 EventType eventType,
                                 EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, String.valueOf(notTheRightPayload), eventType, metadata);
        }
    }

    static final class TwoFallbacksEvent extends AbstractDomainEvent<SampleIdentifier, Object> {

        TwoFallbacksEvent(SampleIdentifier aggregateId, Object payload) {
            super(aggregateId, payload, EventType.of("TwoFallbacks"));
        }

        TwoFallbacksEvent(String eventId,
                          Instant occurredAt,
                          SpecificIdentifier aggregateId,
                          Object payload,
                          EventType eventType,
                          EventMetadata metadata) {
            super(eventId, occurredAt, new SampleIdentifier(aggregateId.value()), payload, eventType, metadata);
        }

        TwoFallbacksEvent(String eventId,
                          Instant occurredAt,
                          AnotherIdentifier aggregateId,
                          Object payload,
                          EventType eventType,
                          EventMetadata metadata) {
            super(eventId, occurredAt, new SampleIdentifier(aggregateId.value()), payload, eventType, metadata);
        }
    }
}
