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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;

/**
 * Infrastructure-facing helper for restoring a previously persisted
 * {@link AbstractDomainEvent}.
 *
 * <p><strong>Intended audience:</strong> driven adapters (serializers,
 * deserializers, event stores). Application services must not use this type to
 * raise new domain facts; generative constructors on the event remain the only
 * supported path for that.
 *
 * <p>Concrete event classes are expected to expose a reconstitution constructor
 * compatible with {@link AbstractDomainEvent}'s reconstitution constructor:
 * {@code (String eventId, Instant occurredAt, I aggregateId, P payload,
 * EventType eventType, EventMetadata metadata)}.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public final class EventReconstitutionSupport {

    private static final int EVENT_ID_INDEX = 0;
    private static final int OCCURRED_AT_INDEX = 1;
    private static final int AGGREGATE_ID_INDEX = 2;
    private static final int PAYLOAD_INDEX = 3;
    private static final int EVENT_TYPE_INDEX = 4;
    private static final int METADATA_INDEX = 5;
    private static final int RECONSTITUTION_PARAM_COUNT = 6;

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws UnsupportedOperationException if called via reflection (implicitly)
     */
    private EventReconstitutionSupport() {
        throw new UnsupportedOperationException();
    }

    /**
     * Restores an event instance of the given concrete type.
     *
     * @param eventClass  concrete event class; must extend {@link AbstractDomainEvent}
     * @param eventId     original event id
     * @param occurredAt  original occurrence time
     * @param aggregateId aggregate identifier
     * @param payload     event payload
     * @param eventType   semantic event type
     * @param metadata    metadata snapshot; {@code null} is treated as empty
     * @param <E>         concrete event type
     * @return reconstituted event instance
     * @throws DomainEventException if arguments are invalid or no suitable constructor exists
     */
    @NonNull
    public static <E extends DomainEvent<?, ?>> E reconstitute(
            @NonNull Class<E> eventClass,
            @NonNull String eventId,
            @NonNull Instant occurredAt,
            @NonNull Identifier<?> aggregateId,
            @NonNull Object payload,
            @NonNull EventType eventType,
            @Nullable EventMetadata metadata) {

        validateReconstitutionArguments(eventClass, eventId, occurredAt, aggregateId, payload, eventType);

        EventMetadata effectiveMetadata = (metadata != null) ? metadata : EventMetadata.empty();
        Constructor<E> constructor = resolveReconstitutionConstructor(eventClass, aggregateId, payload);

        return instantiate(constructor, eventId, occurredAt, aggregateId, payload, eventType, effectiveMetadata);
    }

    /**
     * Validates the mandatory inputs of a reconstitution request.
     *
     * @param eventClass  concrete event class
     * @param eventId     original event id
     * @param occurredAt  original occurrence time
     * @param aggregateId aggregate identifier
     * @param payload     event payload
     * @param eventType   semantic event type
     * @param <E>         concrete event type
     * @throws DomainEventException if any argument is invalid
     */
    private static <E extends DomainEvent<?, ?>> void validateReconstitutionArguments(
            Class<E> eventClass,
            String eventId,
            Instant occurredAt,
            Identifier<?> aggregateId,
            Object payload,
            EventType eventType) {

        requireNotNull(eventClass, "The event class cannot be null");
        requireNonBlank(eventId);
        requireNotNull(occurredAt, "The occurredAt cannot be null");
        requireNotNull(aggregateId, "The aggregateId cannot be null");
        requireNotNull(payload, "The payload cannot be null");
        requireNotNull(eventType, "The eventType cannot be null");

        if (!AbstractDomainEvent.class.isAssignableFrom(eventClass)) {
            throw new DomainEventException("The event class must extend AbstractDomainEvent: " + eventClass.getName());
        }
    }

    /**
     * Instantiates the event through its reconstitution constructor, translating
     * reflective failures into {@link DomainEventException}.
     *
     * @param constructor the resolved constructor
     * @param eventId     original event id
     * @param occurredAt  original occurrence time
     * @param aggregateId aggregate identifier
     * @param payload     event payload
     * @param eventType   semantic event type
     * @param metadata    effective metadata snapshot
     * @param <E>         concrete event type
     * @return the reconstituted event
     * @throws DomainEventException on reflective failure
     */
    @NonNull
    private static <E extends DomainEvent<?, ?>> E instantiate(
            Constructor<E> constructor,
            String eventId,
            Instant occurredAt,
            Identifier<?> aggregateId,
            Object payload,
            EventType eventType,
            EventMetadata metadata) {

        try {
            return constructor.newInstance(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        } catch (InvocationTargetException ex) {
            throw translateInvocationFailure(ex);
        } catch (ReflectiveOperationException ex) {
            throw new DomainEventException(ex.getMessage(), ex);
        }
    }

    /**
     * Translates the cause of an {@link InvocationTargetException} into a
     * {@link DomainEventException}.
     *
     * @param ex the invocation failure
     * @return the exception to throw
     */
    @NonNull
    private static DomainEventException translateInvocationFailure(InvocationTargetException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof DomainEventException domainEventException) return domainEventException;
        return new DomainEventException(cause.getMessage(), cause);
    }

    /**
     * Resolves the reconstitution constructor declared by the given event class.
     *
     * @param eventClass  concrete event class
     * @param aggregateId aggregate identifier instance
     * @param payload     payload instance
     * @param <E>         concrete event type
     * @return the matched constructor
     * @throws DomainEventException if no suitable constructor exists
     */
    @SuppressWarnings("unchecked")
    @NonNull
    private static <E extends DomainEvent<?, ?>> Constructor<E> resolveReconstitutionConstructor(
            Class<E> eventClass, Identifier<?> aggregateId, Object payload) {

        Constructor<?> fallback = null;

        for (Constructor<?> candidate : eventClass.getDeclaredConstructors()) {
            Class<?>[] params = candidate.getParameterTypes();

            if (!hasReconstitutionSignature(params)) continue;
            if (matchesDirectly(params, aggregateId, payload)) return (Constructor<E>) candidate;
            if (fallback == null && params[PAYLOAD_INDEX] == Object.class) fallback = candidate;
        }

        if (fallback != null) return (Constructor<E>) fallback;

        throw new DomainEventException(
                "No reconstitution constructor found on " + eventClass.getName()
                        + ". Expected signature: (String, Instant, Identifier, payload, EventType, EventMetadata)");
    }

    /**
     * Checks whether the given parameter list matches the canonical
     * reconstitution constructor signature.
     *
     * <p>The check is delegated to two focused predicates so that each
     * boolean expression stays within the configured complexity budget.
     *
     * @param params the candidate parameter types
     * @return {@code true} when the signature matches, {@code false} otherwise
     */
    private static boolean hasReconstitutionSignature(Class<?>[] params) {
        return hasExpectedLengthAndPrefix(params) && hasExpectedIdentifierAndTail(params);
    }

    /**
     * Checks the parameter count together with the leading
     * {@code (String, Instant, ...)} portion of the signature.
     *
     * @param params the candidate parameter types
     * @return {@code true} when the length and the first two parameters match, {@code false} otherwise
     */
    private static boolean hasExpectedLengthAndPrefix(Class<?>[] params) {
        return params.length == RECONSTITUTION_PARAM_COUNT
                && params[EVENT_ID_INDEX] == String.class
                && params[OCCURRED_AT_INDEX] == Instant.class;
    }

    /**
     * Checks the trailing {@code (Identifier, ..., EventType, EventMetadata)}
     * portion of the signature.
     *
     * @param params the candidate parameter types
     * @return {@code true} when the identifier, event-type and metadata parameters match, {@code false} otherwise
     */
    private static boolean hasExpectedIdentifierAndTail(Class<?>[] params) {
        return Identifier.class.isAssignableFrom(params[AGGREGATE_ID_INDEX])
                && params[EVENT_TYPE_INDEX] == EventType.class
                && params[METADATA_INDEX] == EventMetadata.class;
    }

    /**
     * Checks whether the candidate constructor's aggregate-id and payload
     * parameters can directly accept the supplied instances.
     *
     * @param params      the candidate parameter types
     * @param aggregateId the aggregate identifier instance
     * @param payload     the payload instance
     * @return {@code true} when both parameters are compatible, {@code false} otherwise
     */
    private static boolean matchesDirectly(Class<?>[] params, Identifier<?> aggregateId, Object payload) {
        return params[AGGREGATE_ID_INDEX].isInstance(aggregateId) && params[PAYLOAD_INDEX].isInstance(payload);
    }

    /**
     * Asserts that the given reference is non-null.
     *
     * @param value   the value to check
     * @param message the exception message
     */
    private static void requireNotNull(Object value, String message) {
        if (value == null) throw new DomainEventException(message);
    }

    /**
     * Asserts that the given string is non-null and not blank.
     *
     * @param value the value to check
     */
    private static void requireNonBlank(String value) {
        if (value == null || value.isBlank()) throw new DomainEventException("The eventId cannot be null or blank");
    }
}
