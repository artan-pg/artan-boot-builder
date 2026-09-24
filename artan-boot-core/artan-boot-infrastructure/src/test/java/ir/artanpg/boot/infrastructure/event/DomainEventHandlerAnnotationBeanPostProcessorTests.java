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
import ir.artanpg.boot.application.port.driven.event.DomainEventBus;
import ir.artanpg.boot.application.port.driven.event.DomainEventListener;
import ir.artanpg.boot.application.port.driven.event.DomainEventMulticaster;
import ir.artanpg.boot.application.port.driven.event.DomainEventSubscription;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventTopic;
import ir.artanpg.boot.infrastructure.event.support.TestDomainEvent;
import ir.artanpg.boot.infrastructure.event.support.TestIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DomainEventHandlerAnnotationBeanPostProcessor}.
 *
 * @author Mohammad Yazdian
 */
@ExtendWith(MockitoExtension.class)
class DomainEventHandlerAnnotationBeanPostProcessorTests {

    @Mock
    private DomainEventMulticaster multicaster;

    @Mock
    private DomainEventBus eventBus;

    @Mock
    private DomainEventSubscription subscription;

    private DomainEventHandlerAnnotationBeanPostProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new DomainEventHandlerAnnotationBeanPostProcessor(multicaster, eventBus);
    }

    @Test
    void postProcessAfterInitialization_ShouldRegisterOnMulticaster_WhenTopicIsEmpty() {
        // given
        TypeBasedHandler bean = new TypeBasedHandler();

        // when
        processor.postProcessAfterInitialization(bean, "typeBasedHandler");

        // then
        ArgumentCaptor<DomainEventListener> captor = ArgumentCaptor.forClass(DomainEventListener.class);
        verify(multicaster).addDomainEventListener(captor.capture());
        verify(eventBus, never()).subscribe(any(), any(), anyInt());
        then(captor.getValue()).isInstanceOf(DomainEventHandlerMethodAdapter.class);
    }

    @Test
    void postProcessAfterInitialization_ShouldSubscribeOnBus_WhenTopicIsSpecified() {
        // given
        given(eventBus.subscribe(any(EventTopic.class), any(DomainEventListener.class), anyInt()))
                .willReturn(subscription);
        TopicBasedHandler bean = new TopicBasedHandler();

        // when
        processor.postProcessAfterInitialization(bean, "topicBasedHandler");

        // then
        ArgumentCaptor<EventTopic> topicCaptor = ArgumentCaptor.forClass(EventTopic.class);
        ArgumentCaptor<DomainEventListener> listenerCaptor = ArgumentCaptor.forClass(DomainEventListener.class);
        verify(eventBus).subscribe(topicCaptor.capture(), listenerCaptor.capture(), eq(7));
        verify(multicaster, never()).addDomainEventListener(any());
        then(topicCaptor.getValue().getName()).isEqualTo("account.lifecycle");
        then(listenerCaptor.getValue()).isInstanceOf(DomainEventHandlerMethodAdapter.class);
    }

    @Test
    void postProcessAfterInitialization_ShouldThrow_WhenTopicSpecifiedButBusIsNull() {
        // given
        DomainEventHandlerAnnotationBeanPostProcessor noBusProcessor =
                new DomainEventHandlerAnnotationBeanPostProcessor(multicaster, null);
        TopicBasedHandler bean = new TopicBasedHandler();

        // when / then
        thenThrownBy(() -> noBusProcessor.postProcessAfterInitialization(bean, "topicBasedHandler"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DomainEventBus");
    }

    @Test
    void postProcessAfterInitialization_ShouldInvokeHandler_WhenEventIsProcessed() {
        // given
        AtomicReference<DomainEvent> captured = new AtomicReference<>();
        TypeBasedHandler bean = new TypeBasedHandler(captured);
        ArgumentCaptor<DomainEventListener> captor = ArgumentCaptor.forClass(DomainEventListener.class);

        // when
        processor.postProcessAfterInitialization(bean, "typeBasedHandler");
        verify(multicaster).addDomainEventListener(captor.capture());
        TestDomainEvent event = new TestDomainEvent(new TestIdentifier("id-1"), "data");
        captor.getValue().process(event);

        // then
        then(captured.get()).isSameAs(event);
    }

    // --- helper beans ---

    static class TypeBasedHandler {

        private final AtomicReference<DomainEvent> captured;

        TypeBasedHandler() {
            this(new AtomicReference<>());
        }

        TypeBasedHandler(AtomicReference<DomainEvent> captured) {
            this.captured = captured;
        }

        @DomainEventHandler
        public void onEvent(TestDomainEvent event) {
            captured.set(event);
        }
    }

    static class TopicBasedHandler {

        @DomainEventHandler(topic = "account.lifecycle", order = 7)
        public void onLifecycle(TestDomainEvent event) {
            // no-op
        }
    }
}
