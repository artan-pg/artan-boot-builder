package ir.artanpg.boot.kernel.eventbus;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public interface EventBus {

	String DEFUALT_TOPIC_NAME = "default";

	<T> void publish(T event);

	<T> void publish(String topic, T event);

	<T> Subscription register(Class<T> eventType, Consumer<T> listener);

	<T> Subscription register(String topic, Class<T> eventType, Consumer<T> listener);

	<T> Subscription register(Class<T> eventType, Subscription.Priority priority, Consumer<T> listener);

	<T> Subscription register(String topic, Class<T> eventType, Subscription.Priority priority, Consumer<T> listener);

	<T> Subscription registerPattern(String pattern, Class<T> eventType, Consumer<T> listener);

	<T> Subscription registerPattern(String pattern, Class<T> eventType, Subscription.Priority priority, Consumer<T> listener);

	SubscriberExceptionHandler getExceptionHandler();

	SubscriberRegistry getSubscriberRegistry();

	ExecutorService getExecutorService();

	MeterRegistry getMeterRegistry();

	void shutdown();
}
