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

package ir.artanpg.boot.infrastructure.event;

import ir.artanpg.boot.application.port.driven.event.DomainEventInterceptor;
import ir.artanpg.boot.application.port.driven.event.DomainEventListener;
import ir.artanpg.boot.application.port.driven.event.DomainEventSmartListener;
import ir.artanpg.boot.application.port.driven.event.ListenerExceptionHandler;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.infrastructure.event.support.AnotherTestDomainEvent;
import ir.artanpg.boot.infrastructure.event.support.TestDomainEvent;
import ir.artanpg.boot.infrastructure.event.support.TestIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

/**
 * Unit tests for {@link SimpleDomainEventMulticaster}.
 *
 * @author Mohammad Yazdian
 */
@ExtendWith(MockitoExtension.class)
class SimpleDomainEventMulticasterTests {

    private SimpleDomainEventMulticaster multicaster;

    private TestDomainEvent event;

    @Mock
    private DomainEventListener listener1;

    @Mock
    private DomainEventListener listener2;

    @Mock
    private DomainEventSmartListener smartListener;

    @Mock
    private DomainEventInterceptor interceptor;

    @Mock
    private ListenerExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        multicaster = new SimpleDomainEventMulticaster();
        event = new TestDomainEvent(new TestIdentifier("agg-1"), "payload");
    }

    @Test
    void multicastEvent_ShouldInvokeMatchingListeners_WhenListenersAreRegistered() {
        // given
        multicaster.addDomainEventListener(listener1);
        multicaster.addDomainEventListener(listener2);

        // when
        multicaster.multicastEvent(event);

        // then
        then(listener1).should().process(event);
        then(listener2).should().process(event);
    }

    @Test
    void multicastEvent_ShouldInvokeListenersInOrder_WhenOrdersAreDifferent() {
        // given
        List<String> invocationOrder = new ArrayList<>();

        DomainEventListener highPriority = new DomainEventListener() {
            @Override
            public void process(DomainEvent e) {
                invocationOrder.add("high");
            }

            @Override
            public int getOrder() {
                return 1;
            }
        };

        DomainEventListener lowPriority = new DomainEventListener() {
            @Override
            public void process(DomainEvent e) {
                invocationOrder.add("low");
            }

            @Override
            public int getOrder() {
                return 100;
            }
        };

        multicaster.addDomainEventListener(lowPriority);
        multicaster.addDomainEventListener(highPriority);

        // when
        multicaster.multicastEvent(event);

        // then
        then(invocationOrder).containsExactly("high", "low");
    }

    @Test
    void multicastEvent_ShouldSkipSmartListener_WhenEventTypeIsNotSupported() {
        // given
        given(smartListener.supportsEventType(TestDomainEvent.class)).willReturn(false);
        multicaster.addDomainEventListener(smartListener);

        // when
        multicaster.multicastEvent(event);

        // then
        then(smartListener).should(never()).process(any());
    }

    @Test
    void multicastEvent_ShouldInvokeSmartListener_WhenEventTypeIsSupported() {
        // given
        given(smartListener.supportsEventType(TestDomainEvent.class)).willReturn(true);
        given(smartListener.getFilter()).willReturn(null);
        multicaster.addDomainEventListener(smartListener);

        // when
        multicaster.multicastEvent(event);

        // then
        then(smartListener).should().process(event);
    }

    @Test
    void multicastEvent_ShouldSkipSmartListener_WhenFilterRejectsEvent() {
        // given
        given(smartListener.supportsEventType(TestDomainEvent.class)).willReturn(true);
        Predicate<DomainEvent> filter = e -> false;
        given(smartListener.getFilter()).willReturn(filter);
        multicaster.addDomainEventListener(smartListener);

        // when
        multicaster.multicastEvent(event);

        // then
        then(smartListener).should(never()).process(any());
    }

    @Test
    void multicastEvent_ShouldCallInterceptorCallbacks_WhenListenerSucceeds() {
        // given
        multicaster.addDomainEventListener(listener1);
        multicaster.addInterceptor(interceptor);

        // when
        multicaster.multicastEvent(event);

        // then
        var inOrder = inOrder(interceptor, listener1);
        inOrder.verify(interceptor).beforeHandle(event, listener1);
        inOrder.verify(listener1).process(event);
        inOrder.verify(interceptor).afterHandle(event, listener1);
        then(interceptor).should(never()).onError(any(), any(), any());
    }

    @Test
    void multicastEvent_ShouldCallOnErrorAndExceptionHandler_WhenListenerThrows() {
        // given
        RuntimeException failure = new RuntimeException("boom");
        given(listener1.exceptionHandler()).willReturn(exceptionHandler);
        org.mockito.BDDMockito.willThrow(failure).given(listener1).process(event);

        multicaster.addDomainEventListener(listener1);
        multicaster.addInterceptor(interceptor);

        // when
        multicaster.multicastEvent(event);

        // then
        then(interceptor).should().beforeHandle(event, listener1);
        then(interceptor).should().onError(event, listener1, failure);
        then(interceptor).should(never()).afterHandle(any(), any());
        then(exceptionHandler).should().handleError(failure, event);
    }

    @Test
    void multicastEvent_ShouldDoNothing_WhenNoListenersAreRegistered() {
        // given
        // no listeners

        // when / then
        multicaster.multicastEvent(event); // must not throw
    }

    @Test
    void multicastEvent_ShouldThrowNullPointerException_WhenEventIsNull() {
        // given / when / then
        thenThrownBy(() -> multicaster.multicastEvent(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("event must not be null");
    }

    @Test
    void addDomainEventListener_ShouldThrowNullPointerException_WhenListenerIsNull() {
        // given / when / then
        thenThrownBy(() -> multicaster.addDomainEventListener(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("listener must not be null");
    }

    @Test
    void removeDomainEventListener_ShouldPreventFurtherInvocation_WhenListenerIsRemoved() {
        // given
        multicaster.addDomainEventListener(listener1);
        multicaster.removeDomainEventListener(listener1);

        // when
        multicaster.multicastEvent(event);

        // then
        then(listener1).should(never()).process(any());
    }

    @Test
    void removeAllListeners_ShouldClearAllRegisteredListeners_WhenCalled() {
        // given
        multicaster.addDomainEventListener(listener1);
        multicaster.addDomainEventListener(listener2);

        // when
        multicaster.removeAllListeners();
        multicaster.multicastEvent(event);

        // then
        then(listener1).should(never()).process(any());
        then(listener2).should(never()).process(any());
    }

    @Test
    void addInterceptor_ShouldThrowNullPointerException_WhenInterceptorIsNull() {
        // given / when / then
        thenThrownBy(() -> multicaster.addInterceptor(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("interceptor must not be null");
    }

    @Test
    void removeInterceptor_ShouldStopCallingInterceptor_WhenRemoved() {
        // given
        multicaster.addDomainEventListener(listener1);
        multicaster.addInterceptor(interceptor);
        multicaster.removeInterceptor(interceptor);

        // when
        multicaster.multicastEvent(event);

        // then
        then(interceptor).should(never()).beforeHandle(any(), any());
        then(interceptor).should(never()).afterHandle(any(), any());
    }
}
