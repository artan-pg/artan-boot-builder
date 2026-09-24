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

import ir.artanpg.boot.application.annotation.DomainEventHandler;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.exception.DomainException;
import ir.artanpg.boot.infrastructure.event.support.AnotherTestDomainEvent;
import ir.artanpg.boot.infrastructure.event.support.TestDomainEvent;
import ir.artanpg.boot.infrastructure.event.support.TestIdentifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link DomainEventHandlerMethodAdapter}.
 *
 * @author Mohammad Yazdian
 */
class DomainEventHandlerMethodAdapterTests {

    @Test
    void process_ShouldInvokeTargetMethod_WhenEventIsCompatible() throws Exception {
        // given
        AtomicReference<DomainEvent> captured = new AtomicReference<>();
        HandlerBean bean = new HandlerBean(captured);
        Method method = HandlerBean.class.getMethod("onTestEvent", TestDomainEvent.class);
        DomainEventHandlerMethodAdapter adapter = new DomainEventHandlerMethodAdapter(bean, method);
        TestDomainEvent event = new TestDomainEvent(new TestIdentifier("id-1"), "data");

        // when
        adapter.process(event);

        // then
        then(captured.get()).isSameAs(event);
    }

    @Test
    void supportsEventType_ShouldReturnTrue_WhenEventTypeMatchesParameter() throws Exception {
        // given
        HandlerBean bean = new HandlerBean(new AtomicReference<>());
        Method method = HandlerBean.class.getMethod("onTestEvent", TestDomainEvent.class);
        DomainEventHandlerMethodAdapter adapter = new DomainEventHandlerMethodAdapter(bean, method);

        // when / then
        then(adapter.supportsEventType(TestDomainEvent.class)).isTrue();
        then(adapter.supportsEventType(AnotherTestDomainEvent.class)).isFalse();
    }

    @Test
    void supportsEventType_ShouldReturnTrue_WhenEventTypeIsDeclaredInAnnotation() throws Exception {
        // given
        MultiTypeHandlerBean bean = new MultiTypeHandlerBean();
        Method method = MultiTypeHandlerBean.class.getMethod("onMultiple", DomainEvent.class);
        DomainEventHandlerMethodAdapter adapter = new DomainEventHandlerMethodAdapter(bean, method);

        // when / then
        then(adapter.supportsEventType(TestDomainEvent.class)).isTrue();
        then(adapter.supportsEventType(AnotherTestDomainEvent.class)).isTrue();
    }

    @Test
    void getOrder_ShouldReturnAnnotationOrder_WhenAnnotationSpecifiesOrder() throws Exception {
        // given
        OrderedHandlerBean bean = new OrderedHandlerBean();
        Method method = OrderedHandlerBean.class.getMethod("onEvent", TestDomainEvent.class);
        DomainEventHandlerMethodAdapter adapter = new DomainEventHandlerMethodAdapter(bean, method);

        // when / then
        then(adapter.getOrder()).isEqualTo(5);
        then(adapter.supportsAsyncExecution()).isTrue();
        then(adapter.getListenerId()).isEqualTo(OrderedHandlerBean.class.getName() + "#onEvent");
    }

    @Test
    void constructor_ShouldThrowDomainException_WhenMethodHasNoAnnotation() throws Exception {
        // given
        HandlerBean bean = new HandlerBean(new AtomicReference<>());
        Method method = HandlerBean.class.getMethod("notAHandler", String.class);

        // when / then
        thenThrownBy(() -> new DomainEventHandlerMethodAdapter(bean, method))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("@DomainEventHandler");
    }

    @Test
    void constructor_ShouldThrowDomainException_WhenMethodHasWrongParameterCount() throws Exception {
        // given
        InvalidHandlerBean bean = new InvalidHandlerBean();
        Method method = InvalidHandlerBean.class.getMethod("noParams");

        // when / then
        thenThrownBy(() -> new DomainEventHandlerMethodAdapter(bean, method))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("exactly one parameter");
    }

    // --- helper beans ---

    static class HandlerBean {

        private final AtomicReference<DomainEvent> captured;

        HandlerBean(AtomicReference<DomainEvent> captured) {
            this.captured = captured;
        }

        @DomainEventHandler
        public void onTestEvent(TestDomainEvent event) {
            captured.set(event);
        }

        public void notAHandler(String value) {
            // not a handler
        }
    }

    static class MultiTypeHandlerBean {

        @DomainEventHandler({TestDomainEvent.class, AnotherTestDomainEvent.class})
        public void onMultiple(DomainEvent event) {
            // no-op
        }
    }

    static class OrderedHandlerBean {

        @DomainEventHandler(order = 5, async = true)
        public void onEvent(TestDomainEvent event) {
            // no-op
        }
    }

    static class InvalidHandlerBean {

        @DomainEventHandler
        public void noParams() {
            // invalid signature
        }
    }
}
