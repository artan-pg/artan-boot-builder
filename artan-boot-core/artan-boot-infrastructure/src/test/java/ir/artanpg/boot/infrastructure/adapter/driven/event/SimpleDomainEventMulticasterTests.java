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

package ir.artanpg.boot.infrastructure.adapter.driven.event;

import ir.artanpg.boot.application.port.driven.event.DomainEventInterceptionContext;
import ir.artanpg.boot.application.port.driven.event.DomainEventInterceptor;
import ir.artanpg.boot.application.port.driving.event.DomainEventListener;
import ir.artanpg.boot.application.port.driving.event.DomainEventSmartListener;
import ir.artanpg.boot.application.port.driving.event.ListenerExceptionHandler;
import ir.artanpg.boot.domain.event.AbstractDomainEvent;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.event.retry.RetryPolicy;
import ir.artanpg.boot.domain.exception.DomainEventException;
import ir.artanpg.boot.domain.exception.InterceptorVetoException;
import ir.artanpg.boot.domain.model.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Unit tests for {@link SimpleDomainEventMulticaster}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("SimpleDomainEventMulticaster")
class SimpleDomainEventMulticasterTests {

    private static final String ERROR_LISTENER_NULL = "The listener cannot be null";
    private static final String ERROR_INTERCEPTOR_NULL = "The interceptor cannot be null";
    private static final String ERROR_EVENT_NULL = "The event cannot be null";
    private static final String ERROR_RETRY_POLICY_NULL = "The defaultRetryPolicy cannot be null";
    private static final String DEFAULT_VETO_MESSAGE = "Publication vetoed by interceptor";

    private static final EventType SAMPLE_TYPE = EventType.of("SampleEvent");
    private static final EventType OTHER_TYPE = EventType.of("OtherEvent");

    private SimpleDomainEventMulticaster multicaster;

    @BeforeEach
    void setUp() {
        multicaster = new SimpleDomainEventMulticaster();
    }

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Creates multicaster with defaults")
        void constructor_ShouldCreateInstance_WhenNoArgsProvided() {
            // given / when
            SimpleDomainEventMulticaster instance = new SimpleDomainEventMulticaster();

            // then
            then(instance)
                    .as("constructor should create a usable multicaster instance")
                    .isNotNull();
            then(instance.getListeners())
                    .as("listener registry should start empty")
                    .isEmpty();
            then(instance.getInterceptors())
                    .as("interceptor registry should start empty")
                    .isEmpty();
        }

        @Test
        @DisplayName("Creates multicaster with executor only")
        void constructor_ShouldCreateInstance_WhenExecutorProvided() {
            // given
            Executor executor = Runnable::run;

            // when
            SimpleDomainEventMulticaster instance = new SimpleDomainEventMulticaster(executor);

            // then
            then(instance)
                    .as("constructor should create a multicaster when only an executor is provided")
                    .isNotNull();
        }

        @Test
        @DisplayName("Creates multicaster with executor and retry policy")
        void constructor_ShouldCreateInstance_WhenExecutorAndRetryPolicyProvided() {
            // given
            Executor executor = Runnable::run;
            RetryPolicy policy = RetryPolicy.fixed(1, Duration.ofMillis(1));

            // when
            SimpleDomainEventMulticaster instance = new SimpleDomainEventMulticaster(executor, policy);

            // then
            then(instance)
                    .as("constructor should create a multicaster when executor and retry policy are provided")
                    .isNotNull();
        }

        @Test
        @DisplayName("Throws DomainEventException when retry policy is null")
        @SuppressWarnings("ConstantConditions")
        void constructor_ShouldThrowDomainEventException_WhenRetryPolicyIsNull() {
            // given
            RetryPolicy policy = null;

            // when & then
            thenThrownBy(() -> new SimpleDomainEventMulticaster(null, policy))
                    .as("constructor must reject a null default RetryPolicy")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_RETRY_POLICY_NULL);
        }
    }

    @Nested
    @DisplayName("addListener")
    class AddListenerTests {

        @Test
        @DisplayName("Registers listener and returns true")
        void addListener_ShouldReturnTrue_WhenListenerIsNew() {
            // given
            RecordingListener listener = new RecordingListener("l1");

            // when
            boolean added = multicaster.addListener(listener);

            // then
            then(added)
                    .as("addListener should return true when the listener id is not yet registered")
                    .isTrue();
            then(multicaster.getListeners())
                    .as("listener registry should contain exactly the newly registered listener")
                    .containsExactly(listener);
        }

        @Test
        @DisplayName("Rejects duplicate listener id and returns false")
        void addListener_ShouldReturnFalse_WhenListenerIdAlreadyRegistered() {
            // given
            RecordingListener first = new RecordingListener("same-id");
            RecordingListener second = new RecordingListener("same-id");
            multicaster.addListener(first);

            // when
            boolean added = multicaster.addListener(second);

            // then
            then(added)
                    .as("addListener should return false when another listener with the same id is already registered")
                    .isFalse();
            then(multicaster.getListeners())
                    .as("listener registry must keep only the first listener for a duplicate id")
                    .containsExactly(first);
        }

        @Test
        @DisplayName("Falls back to class name when listener id is null")
        void addListener_ShouldUseClassNameAsId_WhenListenerIdIsNull() {
            // given
            DomainEventListener<DomainEvent<?, ?>> listener = new DomainEventListener<>() {
                @Override
                public void process(DomainEvent<?, ?> event) {
                    // no-op
                }

                @Override
                public String getListenerId() {
                    return null;
                }

                @Override
                public boolean supportsAsyncExecution() {
                    return false;
                }
            };

            // when
            boolean added = multicaster.addListener(listener);

            // then
            then(added)
                    .as("addListener should register a listener when id is null by falling back to class name")
                    .isTrue();
            then(multicaster.removeListener(listener.getClass().getName()))
                    .as("removeListener should find the listener registered under its class-name id")
                    .isTrue();
        }

        @Test
        @DisplayName("Falls back to class name when listener id is empty")
        void addListener_ShouldUseClassNameAsId_WhenListenerIdIsEmpty() {
            // given
            DomainEventListener<DomainEvent<?, ?>> listener = new DomainEventListener<>() {
                @Override
                public void process(DomainEvent<?, ?> event) {
                    // no-op
                }

                @Override
                public boolean supportsAsyncExecution() {
                    return false;
                }
            };

            // when
            boolean added = multicaster.addListener(listener);

            // then
            then(added)
                    .as("addListener should register a listener when id is empty by falling back to class name")
                    .isTrue();
            then(multicaster.removeListener(listener.getClass().getName()))
                    .as("removeListener should find the listener registered under its class-name id")
                    .isTrue();
        }

        @Test
        @DisplayName("Falls back to class name when listener id is blank")
        void addListener_ShouldUseClassNameAsId_WhenListenerIdIsBlank() {
            // given
            DomainEventListener<DomainEvent<?, ?>> listener = new DomainEventListener<>() {
                @Override
                public void process(DomainEvent<?, ?> event) {
                    // no-op
                }

                @Override
                public String getListenerId() {
                    return "   ";
                }

                @Override
                public boolean supportsAsyncExecution() {
                    return false;
                }
            };

            // when
            boolean added = multicaster.addListener(listener);

            // then
            then(added)
                    .as("addListener should register a listener when id is blank by falling back to class name")
                    .isTrue();
            then(multicaster.getListeners())
                    .as("listener registry should contain the listener registered with a blank id")
                    .containsExactly(listener);
        }

        @Test
        @DisplayName("Throws DomainEventException when listener is null")
        @SuppressWarnings("ConstantConditions")
        void addListener_ShouldThrowDomainEventException_WhenListenerIsNull() {
            // given
            DomainEventListener<?> listener = null;

            // when & then
            thenThrownBy(() -> multicaster.addListener(listener))
                    .as("addListener must reject a null listener argument")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_LISTENER_NULL);
        }
    }

    @Nested
    @DisplayName("removeListener")
    class RemoveListenerTests {

        @Test
        @DisplayName("Removes listener instance and returns true")
        void removeListener_ShouldReturnTrue_WhenListenerWasRegistered() {
            // given
            RecordingListener listener = new RecordingListener("l1");
            multicaster.addListener(listener);

            // when
            boolean removed = multicaster.removeListener(listener);

            // then
            then(removed)
                    .as("removeListener should return true when the listener instance was registered")
                    .isTrue();
            then(multicaster.getListeners())
                    .as("listener registry should be empty after removing the only registered listener")
                    .isEmpty();
        }

        @Test
        @DisplayName("Returns false when listener instance is unknown")
        void removeListener_ShouldReturnFalse_WhenListenerWasNotRegistered() {
            // given
            RecordingListener listener = new RecordingListener("missing");

            // when
            boolean removed = multicaster.removeListener(listener);

            // then
            then(removed)
                    .as("removeListener should return false when the listener instance was never registered")
                    .isFalse();
        }

        @Test
        @DisplayName("Returns false when listener instance is null")
        @SuppressWarnings("ConstantConditions")
        void removeListener_ShouldReturnFalse_WhenListenerIsNull() {
            // given
            DomainEventListener<?> listener = null;

            // when
            boolean removed = multicaster.removeListener(listener);

            // then
            then(removed)
                    .as("removeListener should return false when the listener argument is null")
                    .isFalse();
        }

        @Test
        @DisplayName("Removes listener by id and returns true")
        void removeListener_ShouldReturnTrue_WhenListenerIdExists() {
            // given
            RecordingListener listener = new RecordingListener("by-id");
            multicaster.addListener(listener);

            // when
            boolean removed = multicaster.removeListener("by-id");

            // then
            then(removed)
                    .as("removeListener should return true when a listener with the given id exists")
                    .isTrue();
            then(multicaster.getListeners())
                    .as("listener registry should be empty after removing the listener by id")
                    .isEmpty();
        }

        @Test
        @DisplayName("Returns false when listener id is unknown")
        void removeListener_ShouldReturnFalse_WhenListenerIdDoesNotExist() {
            // given
            // empty registry

            // when
            boolean removed = multicaster.removeListener("unknown");

            // then
            then(removed)
                    .as("removeListener should return false when no listener has the given id")
                    .isFalse();
        }

        @Test
        @DisplayName("Returns false when listener id is null")
        @SuppressWarnings("ConstantConditions")
        void removeListener_ShouldReturnFalse_WhenListenerIdIsNull() {
            // given
            String listenerId = null;

            // when
            boolean removed = multicaster.removeListener(listenerId);

            // then
            then(removed)
                    .as("removeListener should return false when listener id is null")
                    .isFalse();
        }

        @Test
        @DisplayName("Returns false when listener id is empty")
        void removeListener_ShouldReturnFalse_WhenListenerIdIsEmpty() {
            // given
            String listenerId = "";

            // when
            boolean removed = multicaster.removeListener(listenerId);

            // then
            then(removed)
                    .as("removeListener should return false when listener id is an empty string")
                    .isFalse();
        }

        @Test
        @DisplayName("Returns false when listener id is blank")
        void removeListener_ShouldReturnFalse_WhenListenerIdIsBlank() {
            // given
            String listenerId = "   ";

            // when
            boolean removed = multicaster.removeListener(listenerId);

            // then
            then(removed)
                    .as("removeListener should return false when listener id is blank")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("removeAllListeners")
    class RemoveAllListenersTests {

        @Test
        @DisplayName("Clears all registered listeners")
        void removeAllListeners_ShouldClearRegistry_WhenListenersExist() {
            // given
            multicaster.addListener(new RecordingListener("a"));
            multicaster.addListener(new RecordingListener("b"));

            // when
            multicaster.removeAllListeners();

            // then
            then(multicaster.getListeners())
                    .as("removeAllListeners should clear every registered listener from the registry")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("addInterceptor")
    class AddInterceptorTests {

        @Test
        @DisplayName("Registers interceptor and returns true")
        void addInterceptor_ShouldReturnTrue_WhenInterceptorIsNew() {
            // given
            RecordingInterceptor interceptor = new RecordingInterceptor();

            // when
            boolean added = multicaster.addInterceptor(interceptor);

            // then
            then(added)
                    .as("addInterceptor should return true when the interceptor instance is not yet registered")
                    .isTrue();
            then(multicaster.getInterceptors())
                    .as("interceptor registry should contain exactly the newly registered interceptor")
                    .containsExactly(interceptor);
        }

        @Test
        @DisplayName("Rejects same interceptor instance and returns false")
        void addInterceptor_ShouldReturnFalse_WhenInterceptorAlreadyRegistered() {
            // given
            RecordingInterceptor interceptor = new RecordingInterceptor();
            multicaster.addInterceptor(interceptor);

            // when
            boolean added = multicaster.addInterceptor(interceptor);

            // then
            then(added)
                    .as("addInterceptor should return false when the same interceptor instance is registered again")
                    .isFalse();
            then(multicaster.getInterceptors())
                    .as("interceptor registry must not duplicate the same interceptor instance")
                    .containsExactly(interceptor);
        }

        @Test
        @DisplayName("Throws DomainEventException when interceptor is null")
        @SuppressWarnings("ConstantConditions")
        void addInterceptor_ShouldThrowDomainEventException_WhenInterceptorIsNull() {
            // given
            DomainEventInterceptor<DomainEvent<?, ?>> interceptor = null;

            // when & then
            thenThrownBy(() -> multicaster.addInterceptor(interceptor))
                    .as("addInterceptor must reject a null interceptor argument")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_INTERCEPTOR_NULL);
        }
    }

    @Nested
    @DisplayName("removeInterceptor")
    class RemoveInterceptorTests {

        @Test
        @DisplayName("Removes interceptor and returns true")
        void removeInterceptor_ShouldReturnTrue_WhenInterceptorWasRegistered() {
            // given
            RecordingInterceptor interceptor = new RecordingInterceptor();
            multicaster.addInterceptor(interceptor);

            // when
            boolean removed = multicaster.removeInterceptor(interceptor);

            // then
            then(removed)
                    .as("removeInterceptor should return true when the interceptor was registered")
                    .isTrue();
            then(multicaster.getInterceptors())
                    .as("interceptor registry should be empty after removing the only interceptor")
                    .isEmpty();
        }

        @Test
        @DisplayName("Returns false when interceptor was not registered")
        void removeInterceptor_ShouldReturnFalse_WhenInterceptorWasNotRegistered() {
            // given
            RecordingInterceptor interceptor = new RecordingInterceptor();

            // when
            boolean removed = multicaster.removeInterceptor(interceptor);

            // then
            then(removed)
                    .as("removeInterceptor should return false when the interceptor was never registered")
                    .isFalse();
        }

        @Test
        @DisplayName("Returns false when interceptor is null")
        @SuppressWarnings("ConstantConditions")
        void removeInterceptor_ShouldReturnFalse_WhenInterceptorIsNull() {
            // given
            DomainEventInterceptor<DomainEvent<?, ?>> interceptor = null;

            // when
            boolean removed = multicaster.removeInterceptor(interceptor);

            // then
            then(removed)
                    .as("removeInterceptor should return false when the interceptor argument is null")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("multicastEvent")
    class MulticastEventTests {

        @Test
        @DisplayName("Invokes matching listeners in ascending order")
        void multicastEvent_ShouldInvokeListenersInOrder_WhenMultipleListenersRegistered() {
            // given
            List<String> order = new ArrayList<>();
            RecordingListener lowPriority = new RecordingListener("low", 20, order);
            RecordingListener highPriority = new RecordingListener("high", 1, order);
            multicaster.addListener(lowPriority);
            multicaster.addListener(highPriority);
            SampleEvent event = sampleEvent();

            // when
            multicaster.multicastEvent(event);

            // then
            then(order)
                    .as("multicastEvent should invoke listeners in ascending getOrder() sequence")
                    .containsExactly("high", "low");
            then(highPriority.processedEvents())
                    .as("higher-priority listener should receive the published event once")
                    .containsExactly(event);
            then(lowPriority.processedEvents())
                    .as("lower-priority listener should receive the published event once")
                    .containsExactly(event);
        }

        @Test
        @DisplayName("Completes without error when no listeners are registered")
        void multicastEvent_ShouldCompleteSuccessfully_WhenNoListenersRegistered() {
            // given
            SampleEvent event = sampleEvent();

            // when
            multicaster.multicastEvent(event);

            // then
            then(multicaster.getListeners())
                    .as("multicastEvent should leave an empty listener registry unchanged")
                    .isEmpty();
        }

        @Test
        @DisplayName("Throws DomainEventException when event is null")
        @SuppressWarnings("ConstantConditions")
        void multicastEvent_ShouldThrowDomainEventException_WhenEventIsNull() {
            // given
            DomainEvent<?, ?> event = null;

            // when & then
            thenThrownBy(() -> multicaster.multicastEvent(event))
                    .as("multicastEvent must reject a null domain event")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_EVENT_NULL);
        }

        @Test
        @DisplayName("Invokes only listeners whose declared type matches the event")
        void multicastEvent_ShouldInvokeOnlyAssignableListener_WhenDeclaredTypeIsConcrete() {
            // given
            SampleEventOnlyListener sampleOnly = new SampleEventOnlyListener();
            multicaster.addListener(sampleOnly);

            // when
            multicaster.multicastEvent(sampleEvent());
            multicaster.multicastEvent(new OtherSampleEvent(new SampleIdentifier("agg-2"), "x"));

            // then
            then(sampleOnly.processedCount())
                    .as("multicastEvent should deliver only events assignable to the listener declared type")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Accepts every event when listener type argument cannot be resolved")
        void multicastEvent_ShouldAcceptAllEvents_WhenListenerTypeArgumentIsUnresolved() {
            // given
            RawStyleListener listener = new RawStyleListener();
            multicaster.addListener(listener);

            // when
            multicaster.multicastEvent(sampleEvent());
            multicaster.multicastEvent(new OtherSampleEvent(new SampleIdentifier("agg-2"), "x"));

            // then
            then(listener.processedCount())
                    .as("multicastEvent should treat an unresolved listener type argument as matching every event")
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("Resolves declared type from a generic superclass")
        void multicastEvent_ShouldResolveTypeFromGenericSuperclass_WhenInterfacesOmitType() {
            // given
            SuperclassTypedListener listener = new SuperclassTypedListener();
            multicaster.addListener(listener);

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(listener.processedCount())
                    .as("multicastEvent should resolve the listener event type from its generic superclass")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Ignores non-listener parameterized interfaces while resolving type")
        void multicastEvent_ShouldIgnoreNonListenerParameterizedInterfaces_WhenResolvingType() {
            // given
            ListenerWithComparableInterface listener = new ListenerWithComparableInterface();
            multicaster.addListener(listener);

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(listener.processedCount())
                    .as("multicastEvent should ignore parameterized interfaces that are not DomainEventListener")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Accepts every event when listener type argument is a type variable")
        void multicastEvent_ShouldAcceptAllEvents_WhenListenerTypeArgumentIsTypeVariable() {
            // given
            TypeVariableListener<SampleEvent> listener = new TypeVariableListener<>();
            multicaster.addListener(listener);

            // when
            multicaster.multicastEvent(sampleEvent());
            multicaster.multicastEvent(new OtherSampleEvent(new SampleIdentifier("agg-2"), "x"));

            // then
            then(listener.processedCount())
                    .as("multicastEvent should accept every event when the listener type argument is a TypeVariable")
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("Resolves nested parameterized listener type arguments")
        void multicastEvent_ShouldResolveNestedParameterizedType_WhenListenerDeclaresNestedGeneric() {
            // given
            NestedGenericListener listener = new NestedGenericListener();
            multicaster.addListener(listener);

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(listener.processedCount())
                    .as("multicastEvent should unwrap nested parameterized listener type arguments")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Accepts event when declared listener type is DomainEvent")
        void multicastEvent_ShouldAcceptEvent_WhenDeclaredTypeIsDomainEvent() {
            // given
            DomainEventTypedListener listener = new DomainEventTypedListener();
            multicaster.addListener(listener);

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(listener.processedCount())
                    .as("multicastEvent should accept any event when the declared type is DomainEvent itself")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Skips smart listener when event type is unsupported")
        void multicastEvent_ShouldSkipSmartListener_WhenEventTypeNotSupported() {
            // given
            SmartRecordingListener smart = new SmartRecordingListener("smart", OTHER_TYPE, null);
            multicaster.addListener(smart);

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(smart.processedEvents())
                    .as("multicastEvent should skip a smart listener when supportsEventType returns false")
                    .isEmpty();
        }

        @Test
        @DisplayName("Skips smart listener when content filter rejects the event")
        void multicastEvent_ShouldSkipSmartListener_WhenFilterRejectsEvent() {
            // given
            SmartRecordingListener smart = new SmartRecordingListener("smart-filter", SAMPLE_TYPE, _ -> false);
            multicaster.addListener(smart);

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(smart.processedEvents())
                    .as("multicastEvent should skip a smart listener when its content filter rejects the event")
                    .isEmpty();
        }

        @Test
        @DisplayName("Invokes smart listener when type and filter match")
        void multicastEvent_ShouldInvokeSmartListener_WhenTypeAndFilterMatch() {
            // given
            SmartRecordingListener smart = new SmartRecordingListener("smart-ok", SAMPLE_TYPE, _ -> true);
            multicaster.addListener(smart);
            SampleEvent event = sampleEvent();

            // when
            multicaster.multicastEvent(event);

            // then
            then(smart.processedEvents())
                    .as("multicastEvent should invoke a smart listener when type and filter both match")
                    .containsExactly(event);
        }

        @Test
        @DisplayName("Invokes smart listener when filter is null")
        void multicastEvent_ShouldInvokeSmartListener_WhenFilterIsNull() {
            // given
            SmartRecordingListener smart = new SmartRecordingListener("smart-null-filter", SAMPLE_TYPE, null);
            multicaster.addListener(smart);
            SampleEvent event = sampleEvent();

            // when
            multicaster.multicastEvent(event);

            // then
            then(smart.processedEvents())
                    .as("multicastEvent should treat a null smart-listener filter as unrestricted")
                    .containsExactly(event);
        }

        @Test
        @DisplayName("Runs synchronously when listener disables async execution")
        void multicastEvent_ShouldRunSynchronously_WhenListenerDisablesAsync() {
            // given
            multicaster = new SimpleDomainEventMulticaster(Runnable::run);
            RecordingListener listener = new RecordingListener("sync-only");
            multicaster.addListener(listener);
            SampleEvent event = sampleEvent();

            // when
            multicaster.multicastEvent(event);

            // then
            then(listener.processedEvents())
                    .as("multicastEvent should process the event on the calling thread when async is disabled")
                    .containsExactly(event);
        }

        @Test
        @DisplayName("Runs synchronously when executor is null despite async support")
        void multicastEvent_ShouldRunSynchronously_WhenExecutorIsNullDespiteAsyncSupport() {
            // given
            multicaster = new SimpleDomainEventMulticaster(null);
            RecordingListener listener = new RecordingListener("async-forced-sync") {
                @Override
                public boolean supportsAsyncExecution() {
                    return true;
                }
            };
            multicaster.addListener(listener);
            SampleEvent event = sampleEvent();

            // when
            multicaster.multicastEvent(event);

            // then
            then(listener.processedEvents())
                    .as("multicastEvent should fall back to synchronous dispatch when no executor is configured")
                    .containsExactly(event);
        }

        @Test
        @DisplayName("Runs asynchronously when listener supports async and executor is present")
        void multicastEvent_ShouldRunAsynchronously_WhenAsyncSupportedAndExecutorPresent() {
            // given
            multicaster = new SimpleDomainEventMulticaster(Runnable::run);
            RecordingListener listener = new RecordingListener("async-ok") {
                @Override
                public boolean supportsAsyncExecution() {
                    return true;
                }
            };
            multicaster.addListener(listener);
            SampleEvent event = sampleEvent();

            // when
            multicaster.multicastEvent(event);

            // then
            await().atMost(Duration.ofSeconds(1)).untilAsserted(() ->
                    then(listener.processedEvents())
                            .as("multicastEvent should eventually deliver the event on the configured executor")
                            .containsExactly(event));
        }

        @Test
        @DisplayName("Throws InterceptorVetoException when beforePublish vetoes")
        void multicastEvent_ShouldThrowInterceptorVetoException_WhenBeforePublishVetoes() {
            // given
            RecordingListener listener = new RecordingListener("l1");
            multicaster.addListener(listener);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void beforePublish(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                    context.veto("blocked");
                }
            });

            // when & then
            thenThrownBy(() -> multicaster.multicastEvent(sampleEvent()))
                    .as("multicastEvent should abort publication when beforePublish vetoes the context")
                    .isInstanceOf(InterceptorVetoException.class)
                    .hasMessage("blocked");
            then(listener.processedEvents())
                    .as("multicastEvent must not invoke listeners after a publish-phase veto")
                    .isEmpty();
        }

        @Test
        @DisplayName("Throws InterceptorVetoException when beforePublish throws InterceptorVetoException")
        void multicastEvent_ShouldThrowInterceptorVetoException_WhenBeforePublishThrowsInterceptorVetoException() {
            // given
            RecordingListener listener = new RecordingListener("l1");
            multicaster.addListener(listener);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void beforePublish(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                    throw new InterceptorVetoException("publish-veto-ex");
                }
            });

            // when & then
            thenThrownBy(() -> multicaster.multicastEvent(sampleEvent()))
                    .as("multicastEvent should abort when beforePublish throws InterceptorVetoException")
                    .isInstanceOf(InterceptorVetoException.class)
                    .hasMessage("publish-veto-ex");
            then(listener.processedEvents())
                    .as("multicastEvent must not deliver the event after a publish veto exception")
                    .isEmpty();
        }

        @Test
        @DisplayName("Throws InterceptorVetoException when beforePublish throws a runtime exception")
        void multicastEvent_ShouldThrowInterceptorVetoException_WhenBeforePublishThrowsRuntimeException() {
            // given
            RecordingListener listener = new RecordingListener("l1");
            multicaster.addListener(listener);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void beforePublish(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                    throw new IllegalStateException("publish-hook-failed");
                }
            });

            // when & then
            thenThrownBy(() -> multicaster.multicastEvent(sampleEvent()))
                    .as("multicastEvent should convert beforePublish runtime failures into a publication veto")
                    .isInstanceOf(InterceptorVetoException.class)
                    .hasMessage("publish-hook-failed");
            then(listener.processedEvents())
                    .as("multicastEvent must not invoke listeners after beforePublish fails")
                    .isEmpty();
        }

        @Test
        @DisplayName("Uses default veto message when veto reason is null")
        void multicastEvent_ShouldUseDefaultVetoMessage_WhenVetoReasonIsNull() {
            // given
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void beforePublish(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                    context.veto();
                }
            });

            // when & then
            thenThrownBy(() -> multicaster.multicastEvent(sampleEvent()))
                    .as("multicastEvent should fall back to the default veto message when no reason is provided")
                    .isInstanceOf(InterceptorVetoException.class)
                    .hasMessage(DEFAULT_VETO_MESSAGE);
        }

        @Test
        @DisplayName("Invokes beforePublish interceptors in ascending order")
        void multicastEvent_ShouldInvokeBeforePublishInAscendingOrder_WhenMultipleInterceptorsRegistered() {
            // given
            List<String> order = new ArrayList<>();
            multicaster.addInterceptor(orderedBeforePublishInterceptor("first", 10, order));
            multicaster.addInterceptor(orderedBeforePublishInterceptor("second", 1, order));
            multicaster.addListener(new RecordingListener("l1"));

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(order)
                    .as("multicastEvent should invoke beforePublish hooks in ascending getOrder() sequence")
                    .containsExactly("second", "first");
        }

        @Test
        @DisplayName("Invokes afterPublish interceptors in reverse order")
        void multicastEvent_ShouldInvokeAfterPublishInReverseOrder_WhenMultipleInterceptorsRegistered() {
            // given
            List<String> order = new ArrayList<>();
            multicaster.addInterceptor(orderedAfterPublishInterceptor("first", 1, order));
            multicaster.addInterceptor(orderedAfterPublishInterceptor("second", 2, order));
            multicaster.addListener(new RecordingListener("l1"));

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(order)
                    .as("multicastEvent should invoke afterPublish hooks in reverse order for onion-style unwinding")
                    .containsExactly("second", "first");
        }

        @Test
        @DisplayName("Runs afterPublish even when publication is vetoed")
        void multicastEvent_ShouldInvokeAfterPublish_WhenPublicationIsVetoed() {
            // given
            AtomicInteger afterPublishCalls = new AtomicInteger();
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void beforePublish(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                    context.veto("stop");
                }

                @Override
                public void afterPublish(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                    afterPublishCalls.incrementAndGet();
                }
            });

            // when & then
            thenThrownBy(() -> multicaster.multicastEvent(sampleEvent()))
                    .as("multicastEvent should still surface InterceptorVetoException when publication is vetoed")
                    .isInstanceOf(InterceptorVetoException.class)
                    .hasMessage("stop");
            then(afterPublishCalls.get())
                    .as("multicastEvent should run afterPublish in finally even when publication was vetoed")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Swallows failures thrown from afterPublish hooks")
        void multicastEvent_ShouldSwallowException_WhenAfterPublishThrows() {
            // given
            RecordingListener listener = new RecordingListener("l1");
            multicaster.addListener(listener);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void afterPublish(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                    throw new IllegalStateException("after-publish-failed");
                }
            });

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(listener.processedEvents())
                    .as("multicastEvent should not prevent listener delivery when afterPublish throws")
                    .hasSize(1);
        }

        @Test
        @DisplayName("Skips interceptors that do not apply to the event")
        void multicastEvent_ShouldSkipInterceptor_WhenAppliesToReturnsFalse() {
            // given
            AtomicInteger beforePublishCount = new AtomicInteger();
            multicaster.addListener(new RecordingListener("l1"));
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public boolean appliesTo(DomainEvent<?, ?> event) {
                    return false;
                }

                @Override
                public void beforePublish(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                    beforePublishCount.incrementAndGet();
                }
            });

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(beforePublishCount.get())
                    .as("multicastEvent should not invoke interceptor hooks when appliesTo returns false")
                    .isZero();
        }

        @Test
        @DisplayName("Skips only the vetoed listener during handle phase")
        void multicastEvent_ShouldSkipListener_WhenBeforeHandleVetoes() {
            // given
            RecordingListener allowed = new RecordingListener("allowed");
            RecordingListener blocked = new RecordingListener("blocked");
            multicaster.addListener(allowed);
            multicaster.addListener(blocked);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void beforeHandle(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                    if ("blocked".equals(context.getListenerId())) {
                        context.veto("skip-one");
                    }
                }
            });
            SampleEvent event = sampleEvent();

            // when
            multicaster.multicastEvent(event);

            // then
            then(allowed.processedEvents())
                    .as("multicastEvent should still process listeners that were not vetoed")
                    .containsExactly(event);
            then(blocked.processedEvents())
                    .as("multicastEvent should skip a listener vetoed in beforeHandle")
                    .isEmpty();
        }

        @Test
        @DisplayName("Skips listener when beforeHandle throws InterceptorVetoException")
        void multicastEvent_ShouldSkipListener_WhenBeforeHandleThrowsInterceptorVetoException() {
            // given
            RecordingListener listener = new RecordingListener("l1");
            multicaster.addListener(listener);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void beforeHandle(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                    throw new InterceptorVetoException("handle-veto-ex");
                }
            });

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(listener.processedEvents())
                    .as("multicastEvent should skip the listener when beforeHandle throws InterceptorVetoException")
                    .isEmpty();
        }

        @Test
        @DisplayName("Skips listener when beforeHandle throws a runtime exception")
        void multicastEvent_ShouldSkipListener_WhenBeforeHandleThrowsRuntimeException() {
            // given
            RecordingListener listener = new RecordingListener("l1");
            multicaster.addListener(listener);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void beforeHandle(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                    throw new IllegalStateException("handle-runtime");
                }
            });

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(listener.processedEvents())
                    .as("multicastEvent should skip the listener when beforeHandle throws a runtime exception")
                    .isEmpty();
        }

        @Test
        @DisplayName("Invokes afterHandle when process completes successfully")
        void multicastEvent_ShouldInvokeAfterHandle_WhenProcessSucceeds() {
            // given
            AtomicInteger afterHandleCount = new AtomicInteger();
            RecordingListener listener = new RecordingListener("ok");
            multicaster.addListener(listener);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void afterHandle(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                    afterHandleCount.incrementAndGet();
                }
            });

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(afterHandleCount.get())
                    .as("multicastEvent should call afterHandle once after a successful process invocation")
                    .isEqualTo(1);
            then(listener.processedEvents())
                    .as("multicastEvent should deliver the event to the listener on success")
                    .hasSize(1);
        }

        @Test
        @DisplayName("Swallows failures thrown from afterHandle hooks")
        void multicastEvent_ShouldSwallowException_WhenAfterHandleThrows() {
            // given
            RecordingListener listener = new RecordingListener("l1");
            multicaster.addListener(listener);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void afterHandle(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                    throw new IllegalStateException("after-handle-failed");
                }
            });

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(listener.processedEvents())
                    .as("multicastEvent should keep a successful delivery even when afterHandle throws")
                    .hasSize(1);
        }

        @Test
        @DisplayName("Continues other listeners when one listener fails")
        void multicastEvent_ShouldContinueOtherListeners_WhenOneListenerThrows() {
            // given
            FailingListener failing = new FailingListener("failing", new IllegalStateException("boom"));
            RecordingListener surviving = new RecordingListener("surviving");
            multicaster.addListener(failing);
            multicaster.addListener(surviving);
            SampleEvent event = sampleEvent();

            // when
            multicaster.multicastEvent(event);

            // then
            then(failing.handledExceptions())
                    .as("multicastEvent should forward the failing listener exception to its exceptionHandler once")
                    .hasSize(1);
            then(surviving.processedEvents())
                    .as("multicastEvent must not stop remaining listeners when one listener fails")
                    .containsExactly(event);
        }

        @Test
        @DisplayName("Swallows exception thrown by listener exceptionHandler")
        void multicastEvent_ShouldSwallowException_WhenExceptionHandlerThrows() {
            // given
            DomainEventListener<DomainEvent<?, ?>> listener = new DomainEventListener<>() {
                @Override
                public void process(DomainEvent<?, ?> event) {
                    throw new IllegalStateException("process-failed");
                }

                @Override
                public String getListenerId() {
                    return "handler-throws";
                }

                @Override
                public boolean supportsAsyncExecution() {
                    return false;
                }

                @Override
                public ListenerExceptionHandler exceptionHandler() {
                    return (_, _) -> {
                        throw new IllegalArgumentException("handler-failed");
                    };
                }
            };
            RecordingListener other = new RecordingListener("other");
            multicaster.addListener(listener);
            multicaster.addListener(other);

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(other.processedEvents())
                    .as("multicastEvent should keep dispatching remaining listeners when exceptionHandler itself fails")
                    .hasSize(1);
        }

        @Test
        @DisplayName("Swallows failures thrown from onError hooks")
        void multicastEvent_ShouldSwallowException_WhenOnErrorThrows() {
            // given
            DomainEventListener<DomainEvent<?, ?>> failing = new DomainEventListener<>() {
                @Override
                public void process(DomainEvent<?, ?> event) {
                    throw new IllegalStateException("boom");
                }

                @Override
                public String getListenerId() {
                    return "failing";
                }

                @Override
                public boolean supportsAsyncExecution() {
                    return false;
                }
            };
            RecordingListener surviving = new RecordingListener("surviving");
            multicaster.addListener(failing);
            multicaster.addListener(surviving);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void onError(DomainEventInterceptionContext<DomainEvent<?, ?>> context, Throwable throwable) {
                    throw new IllegalStateException("on-error-failed");
                }
            });

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(surviving.processedEvents())
                    .as("multicastEvent should continue remaining listeners when onError itself throws")
                    .hasSize(1);
        }

        @Test
        @DisplayName("Skips onError when interceptor does not apply")
        void multicastEvent_ShouldSkipOnError_WhenAppliesToReturnsFalse() {
            // given
            AtomicInteger onErrorCount = new AtomicInteger();
            DomainEventListener<DomainEvent<?, ?>> failing = new DomainEventListener<>() {
                @Override
                public void process(DomainEvent<?, ?> event) {
                    throw new IllegalStateException("boom");
                }

                @Override
                public String getListenerId() {
                    return "failing";
                }

                @Override
                public boolean supportsAsyncExecution() {
                    return false;
                }
            };
            multicaster.addListener(failing);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public boolean appliesTo(DomainEvent<?, ?> event) {
                    return false;
                }

                @Override
                public void onError(DomainEventInterceptionContext<DomainEvent<?, ?>> context, Throwable throwable) {
                    onErrorCount.incrementAndGet();
                }
            });

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(onErrorCount.get())
                    .as("multicastEvent should skip onError hooks when appliesTo returns false")
                    .isZero();
        }

        @Test
        @DisplayName("Retries listener when interceptor requests retry")
        void multicastEvent_ShouldRetryListener_WhenInterceptorRequestsRetry() {
            // given
            AtomicInteger attempts = new AtomicInteger();
            DomainEventListener<DomainEvent<?, ?>> listener = new DomainEventListener<>() {
                @Override
                public void process(DomainEvent<?, ?> event) {
                    if (attempts.incrementAndGet() == 1) throw new IllegalStateException("transient");
                }

                @Override
                public String getListenerId() {
                    return "retry-listener";
                }

                @Override
                public boolean supportsAsyncExecution() {
                    return false;
                }
            };
            multicaster = new SimpleDomainEventMulticaster(null, RetryPolicy.fixed(1, Duration.ofMillis(1)));
            multicaster.addListener(listener);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void onError(DomainEventInterceptionContext<DomainEvent<?, ?>> context, Throwable throwable) {
                    context.requestRetry();
                }
            });

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(attempts.get())
                    .as("multicastEvent should run the initial attempt plus one granted retry")
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("Does not retry when retry policy denies the request")
        void multicastEvent_ShouldNotRetry_WhenRetryPolicyDeniesRequest() {
            // given
            AtomicInteger attempts = new AtomicInteger();
            DomainEventListener<DomainEvent<?, ?>> listener = new DomainEventListener<>() {
                @Override
                public void process(DomainEvent<?, ?> event) {
                    attempts.incrementAndGet();
                    throw new IllegalStateException("always-fail");
                }

                @Override
                public String getListenerId() {
                    return "no-retry";
                }

                @Override
                public boolean supportsAsyncExecution() {
                    return false;
                }
            };
            multicaster = new SimpleDomainEventMulticaster(null, RetryPolicy.none());
            multicaster.addListener(listener);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void onError(DomainEventInterceptionContext<DomainEvent<?, ?>> context, Throwable throwable) {
                    context.requestRetry();
                }
            });

            // when
            multicaster.multicastEvent(sampleEvent());

            // then
            then(attempts.get())
                    .as("multicastEvent should not retry when RetryPolicy.none denies the requested retry")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Applies backoff delay when retry policy grants a positive interval")
        void multicastEvent_ShouldApplyBackoffDelay_WhenRetryPolicyGrantsPositiveInterval() {
            // given
            AtomicInteger attempts = new AtomicInteger();
            DomainEventListener<DomainEvent<?, ?>> listener = new DomainEventListener<>() {
                @Override
                public void process(DomainEvent<?, ?> event) {
                    if (attempts.incrementAndGet() == 1) throw new IllegalStateException("transient");
                }

                @Override
                public String getListenerId() {
                    return "retry-sleep";
                }

                @Override
                public boolean supportsAsyncExecution() {
                    return false;
                }
            };
            multicaster = new SimpleDomainEventMulticaster(null, RetryPolicy.fixed(1, Duration.ofMillis(30)));
            multicaster.addListener(listener);
            multicaster.addInterceptor(new DomainEventInterceptor<>() {
                @Override
                public void onError(DomainEventInterceptionContext<DomainEvent<?, ?>> context, Throwable throwable) {
                    context.requestRetry();
                }
            });

            // when
            long started = System.nanoTime();
            multicaster.multicastEvent(sampleEvent());
            long elapsedMs = Duration.ofNanos(System.nanoTime() - started).toMillis();

            // then
            then(attempts.get())
                    .as("multicastEvent should execute the initial attempt plus one granted retry")
                    .isEqualTo(2);
            then(elapsedMs)
                    .as("multicastEvent should apply the retry backoff delay before the next attempt")
                    .isGreaterThanOrEqualTo(20L);
        }

        @Test
        @DisplayName("Returns null from type extraction when type is not parameterized")
        void multicastEvent_ShouldTreatTypeAsUnresolved_WhenExtractedTypeIsNotParameterized() {
            // given
            Type type = String.class;

            // when
            Class<?> result = invokeExtractEventTypeArgument(type);

            // then
            then(result)
                    .as("multicastEvent type extraction should return null when the input type is not parameterized")
                    .isNull();
        }

        @Test
        @DisplayName("Returns null from type extraction when raw type is not a Class")
        void multicastEvent_ShouldTreatTypeAsUnresolved_WhenExtractedRawTypeIsNotClass() {
            // given
            Type nonClassRaw = new Type() {
                @Override
                public String getTypeName() {
                    return "NonClassRaw";
                }
            };
            ParameterizedType type = parameterizedType(nonClassRaw, SampleEvent.class);

            // when
            Class<?> result = invokeExtractEventTypeArgument(type);

            // then
            then(result)
                    .as("multicastEvent type extraction should return null when the parameterized raw type is not a " +
                            "Class")
                    .isNull();
        }

        @Test
        @DisplayName("Returns null from type extraction when raw class is not DomainEventListener")
        void multicastEvent_ShouldTreatTypeAsUnresolved_WhenExtractedRawClassIsNotDomainEventListener() {
            // given
            ParameterizedType type = parameterizedType(Comparable.class, String.class);

            // when
            Class<?> result = invokeExtractEventTypeArgument(type);

            // then
            then(result)
                    .as("multicastEvent type extraction should return null when the raw class is not " +
                            "DomainEventListener")
                    .isNull();
        }

        @Test
        @DisplayName("Resolves Class type argument through listener type extraction")
        void multicastEvent_ShouldResolveDeclaredType_WhenExtractedTypeArgumentIsClass() {
            // given
            ParameterizedType type = parameterizedType(DomainEventListener.class, SampleEvent.class);

            // when
            Class<?> result = invokeExtractEventTypeArgument(type);

            // then
            then(result)
                    .as("multicastEvent type extraction should return the Class type argument of DomainEventListener")
                    .isEqualTo(SampleEvent.class);
        }

        @Test
        @DisplayName("Resolves nested parameterized type argument through listener type extraction")
        void multicastEvent_ShouldResolveNestedRawClass_WhenExtractedTypeArgumentIsParameterized() {
            // given
            ParameterizedType nestedEventType = parameterizedType(
                    DomainEvent.class, SampleIdentifier.class, String.class);
            ParameterizedType type = parameterizedType(DomainEventListener.class, nestedEventType);

            // when
            Class<?> result = invokeExtractEventTypeArgument(type);

            // then
            then(result)
                    .as("multicastEvent type extraction should unwrap a nested ParameterizedType argument to its raw " +
                            "Class")
                    .isEqualTo(DomainEvent.class);
        }

        @Test
        @DisplayName("Returns null when nested parameterized raw type is not a Class")
        void multicastEvent_ShouldTreatTypeAsUnresolved_WhenNestedRawTypeIsNotClass() {
            // given
            Type nonClassNestedRaw = new Type() {
                @Override
                public String getTypeName() {
                    return "NonClassNestedRaw";
                }
            };
            ParameterizedType nested = parameterizedType(nonClassNestedRaw, String.class);
            ParameterizedType type = parameterizedType(DomainEventListener.class, nested);

            // when
            Class<?> result = invokeExtractEventTypeArgument(type);

            // then
            then(result)
                    .as("multicastEvent type extraction should return null when the nested ParameterizedType raw type" +
                            " is not a Class")
                    .isNull();
        }

        @Test
        @DisplayName("Returns null when type argument is neither Class nor ParameterizedType")
        void multicastEvent_ShouldTreatTypeAsUnresolved_WhenExtractedTypeArgumentIsUnsupported() {
            // given
            Type unsupportedArg = new Type() {
                @Override
                public String getTypeName() {
                    return "UnsupportedArg";
                }
            };
            ParameterizedType type = parameterizedType(DomainEventListener.class, unsupportedArg);

            // when
            Class<?> result = invokeExtractEventTypeArgument(type);

            // then
            then(result)
                    .as("multicastEvent type extraction should return null when the type argument is neither Class " +
                            "nor ParameterizedType")
                    .isNull();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers and test doubles
    // -------------------------------------------------------------------------

    private static SampleEvent sampleEvent() {
        return new SampleEvent(new SampleIdentifier("agg-1"), "payload");
    }

    private static DomainEventInterceptor<DomainEvent<?, ?>> orderedBeforePublishInterceptor(
            String name, int order, List<String> sink) {
        return new DomainEventInterceptor<>() {
            @Override
            public int getOrder() {
                return order;
            }

            @Override
            public void beforePublish(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                sink.add(name);
            }
        };
    }

    private static DomainEventInterceptor<DomainEvent<?, ?>> orderedAfterPublishInterceptor(
            String name, int order, List<String> sink) {
        return new DomainEventInterceptor<>() {
            @Override
            public int getOrder() {
                return order;
            }

            @Override
            public void afterPublish(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
                sink.add(name);
            }
        };
    }

    private record SampleIdentifier(String value) implements Identifier<String> {
    }

    private static final class SampleEvent extends AbstractDomainEvent<SampleIdentifier, String> {
        SampleEvent(SampleIdentifier aggregateId, String payload) {
            super(aggregateId, payload, SAMPLE_TYPE);
        }
    }

    private static final class OtherSampleEvent extends AbstractDomainEvent<SampleIdentifier, String> {
        OtherSampleEvent(SampleIdentifier aggregateId, String payload) {
            super(aggregateId, payload, OTHER_TYPE);
        }
    }

    private static class RecordingListener implements DomainEventListener<DomainEvent<?, ?>> {

        private final String id;
        private final int order;
        private final List<String> sharedOrder;
        private final List<DomainEvent<?, ?>> processed = new CopyOnWriteArrayList<>();

        RecordingListener(String id) {
            this(id, Integer.MAX_VALUE, null);
        }

        RecordingListener(String id, int order, List<String> sharedOrder) {
            this.id = id;
            this.order = order;
            this.sharedOrder = sharedOrder;
        }

        @Override
        public void process(DomainEvent<?, ?> event) {
            processed.add(event);
            if (sharedOrder != null) {
                sharedOrder.add(id);
            }
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public String getListenerId() {
            return id;
        }

        @Override
        public boolean supportsAsyncExecution() {
            return false;
        }

        List<DomainEvent<?, ?>> processedEvents() {
            return List.copyOf(processed);
        }
    }

    private static final class FailingListener implements DomainEventListener<DomainEvent<?, ?>> {

        private final String id;
        private final RuntimeException failure;
        private final List<Exception> handled = new CopyOnWriteArrayList<>();

        FailingListener(String id, RuntimeException failure) {
            this.id = id;
            this.failure = failure;
        }

        @Override
        public void process(DomainEvent<?, ?> event) {
            throw failure;
        }

        @Override
        public String getListenerId() {
            return id;
        }

        @Override
        public boolean supportsAsyncExecution() {
            return false;
        }

        @Override
        public ListenerExceptionHandler exceptionHandler() {
            return (exception, _) -> handled.add(exception);
        }

        List<Exception> handledExceptions() {
            return List.copyOf(handled);
        }
    }

    private static final class SmartRecordingListener implements DomainEventSmartListener<DomainEvent<?, ?>> {

        private final String id;
        private final EventType supportedType;
        private final Predicate<DomainEvent<?, ?>> filter;
        private final List<DomainEvent<?, ?>> processed = new CopyOnWriteArrayList<>();

        SmartRecordingListener(String id, EventType supportedType, Predicate<DomainEvent<?, ?>> filter) {
            this.id = id;
            this.supportedType = supportedType;
            this.filter = filter;
        }

        @Override
        public void process(DomainEvent<?, ?> event) {
            processed.add(event);
        }

        @Override
        public boolean supportsEventType(EventType eventType) {
            return supportedType.equals(eventType);
        }

        @Override
        public Predicate<DomainEvent<?, ?>> getFilter() {
            return filter;
        }

        @Override
        public String getListenerId() {
            return id;
        }

        @Override
        public boolean supportsAsyncExecution() {
            return false;
        }

        List<DomainEvent<?, ?>> processedEvents() {
            return List.copyOf(processed);
        }
    }

    private static final class RecordingInterceptor implements DomainEventInterceptor<DomainEvent<?, ?>> {
    }

    private static final class SampleEventOnlyListener implements DomainEventListener<SampleEvent> {

        private final AtomicInteger processed = new AtomicInteger();

        @Override
        public void process(SampleEvent event) {
            processed.incrementAndGet();
        }

        @Override
        public String getListenerId() {
            return "sample-only";
        }

        @Override
        public boolean supportsAsyncExecution() {
            return false;
        }

        int processedCount() {
            return processed.get();
        }
    }

    private abstract static class AbstractTypedListener implements DomainEventListener<SampleEvent> {

        private final AtomicInteger processed = new AtomicInteger();

        @Override
        public void process(SampleEvent event) {
            processed.incrementAndGet();
        }

        @Override
        public boolean supportsAsyncExecution() {
            return false;
        }

        int processedCount() {
            return processed.get();
        }
    }

    private static final class SuperclassTypedListener extends AbstractTypedListener {

        @Override
        public String getListenerId() {
            return "superclass-typed";
        }
    }

    private static final class DomainEventTypedListener implements DomainEventListener<DomainEvent<?, ?>> {

        private final AtomicInteger processed = new AtomicInteger();

        @Override
        public void process(DomainEvent<?, ?> event) {
            processed.incrementAndGet();
        }

        @Override
        public String getListenerId() {
            return "domain-event-typed";
        }

        @Override
        public boolean supportsAsyncExecution() {
            return false;
        }

        int processedCount() {
            return processed.get();
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class RawStyleListener implements DomainEventListener {

        private final AtomicInteger processed = new AtomicInteger();

        @Override
        public void process(DomainEvent event) {
            processed.incrementAndGet();
        }

        @Override
        public String getListenerId() {
            return "raw-style";
        }

        @Override
        public boolean supportsAsyncExecution() {
            return false;
        }

        int processedCount() {
            return processed.get();
        }
    }

    private static final class NestedGenericListener
            implements DomainEventListener<DomainEvent<SampleIdentifier, String>> {

        private final AtomicInteger processed = new AtomicInteger();

        @Override
        public void process(DomainEvent<SampleIdentifier, String> event) {
            processed.incrementAndGet();
        }

        @Override
        public String getListenerId() {
            return "nested-generic";
        }

        @Override
        public boolean supportsAsyncExecution() {
            return false;
        }

        int processedCount() {
            return processed.get();
        }
    }

    private static final class ListenerWithComparableInterface
            implements Comparable<String>, DomainEventListener<SampleEvent> {

        private final AtomicInteger processed = new AtomicInteger();

        @Override
        public int compareTo(String other) {
            return 0;
        }

        @Override
        public void process(SampleEvent event) {
            processed.incrementAndGet();
        }

        @Override
        public String getListenerId() {
            return "comparable-and-listener";
        }

        @Override
        public boolean supportsAsyncExecution() {
            return false;
        }

        int processedCount() {
            return processed.get();
        }
    }

    private static final class TypeVariableListener<E extends DomainEvent<?, ?>> implements DomainEventListener<E> {

        private final AtomicInteger processed = new AtomicInteger();

        @Override
        public void process(E event) {
            processed.incrementAndGet();
        }

        @Override
        public String getListenerId() {
            return "type-variable-listener";
        }

        @Override
        public boolean supportsAsyncExecution() {
            return false;
        }

        int processedCount() {
            return processed.get();
        }
    }

    private static Class<?> invokeExtractEventTypeArgument(Type type) {
        Method method;
        try {
            method = SimpleDomainEventMulticaster.class.getDeclaredMethod("extractEventTypeArgument", Type.class);
            method.setAccessible(true);
            return (Class<?>) method.invoke(null, type);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static ParameterizedType parameterizedType(Type rawType, Type... typeArguments) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return typeArguments;
            }

            @Override
            public Type getRawType() {
                return rawType;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }
}
