package ir.artanpg.boot.kernel.eventbus;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.lang.String.format;

public class SimpleEventBus extends AbstractEventBus {

	public SimpleEventBus(MeterRegistry meterRegistry) {
		super(meterRegistry);
	}

	@Override
	public <T> void publish(T event) {
		publish(DEFUALT_TOPIC_NAME, event);
	}

	@Override
	public <T> void publish(String topic, T event) {
		if (topic == null || event == null) return;

		incrementPublishCounter(topic);

		Timer.Sample sample = Timer.start(getMeterRegistry());

		getExecutorService().execute(() -> {
			String status = "success";
			SubscriberExceptionHandler handler = getExceptionHandler();

			try {
				List<SimpleSubscription<T>> exactSubs = sortedByPriority(getSubscriberRegistry().getSubscriber(topic));
				for (SimpleSubscription<T> subscription : exactSubs) {
					subscription.invoke(event, topic, handler);
				}

				for (SubscriberRegistry.PatternSubscription pattern : getSubscriberRegistry().getAllPatternSubscriptions()) {
					if (pattern.pattern().matcher(topic).matches()) {
						List<SimpleSubscription<T>> sortedSubscription = sortedByPriority(pattern.subscriptions());
						for (SimpleSubscription<T> sub : sortedSubscription) {
							sub.invoke(event, topic, handler);
						}
					}
				}
			} catch (Throwable ex) {
				status = "error";
			} finally {
				if (getMeterRegistry() != null) {
					sample.stop(Timer.builder("eventbus.publish.latency")
							.description("Latency from publish to listener completion")
							.tags(Tags.of("topic", topic, "status", status))
							.register(getMeterRegistry()));
				}
			}
		});
	}

	@Override
	public <T> Subscription register(Class<T> eventType, Consumer<T> listener) {
		return register(DEFUALT_TOPIC_NAME, eventType, listener);
	}

	@Override
	public <T> Subscription register(String topic, Class<T> eventType, Consumer<T> listener) {
		return register(topic, eventType, Subscription.Priority.NORMAL, listener);
	}

	@Override
	public <T> Subscription register(Class<T> eventType, Subscription.Priority priority, Consumer<T> listener) {
		return register(DEFUALT_TOPIC_NAME, eventType, priority, listener);
	}

	@Override
	public <T> Subscription register(String topic, Class<T> eventType, Subscription.Priority priority, Consumer<T> listener) {
//		SimpleSubscription<T> subscription = new
//				SimpleSubscription<>(listener, eventType, priority, () -> getSubscriberRegistry().removeSubscription(topic, subscription));
//		getSubscriberRegistry().register(topic, subscription);
//		return subscription;

		Consumer<T> resilientListener = decorateWithFaultTolerance(listener, topic);

		Runnable onUnsubscribe = () -> getSubscriberRegistry().removeSubscription(topic, subscription);

		SimpleSubscription<T> subscription = new SimpleSubscription<>(
				resilientListener, eventType, priority, onUnsubscribe
		);

		getSubscriberRegistry().register(topic, subscription);
		return subscription;
	}

	@Override
	public <T> Subscription registerPattern(String pattern, Class<T> eventType, Consumer<T> listener) {
		return registerPattern(pattern, eventType, Subscription.Priority.NORMAL, listener);
	}

	@Override
	public <T> Subscription registerPattern(String pattern, Class<T> eventType, Subscription.Priority priority, Consumer<T> listener) {
		if (pattern == null || pattern.isBlank()) {
			throw new IllegalArgumentException("Pattern cannot be null or empty");
		}

		if (eventType == null || listener == null) {
			throw new IllegalArgumentException("eventType and listener cannot be null");
		}

		Subscription.Priority effectivePriority = priority != null ? priority : Subscription.Priority.NORMAL;

		try {
			if (pattern.contains("|") || pattern.contains("(") || pattern.contains("[") || pattern.contains("{") ||
					pattern.contains("^") || pattern.contains("$")) {
				throw new IllegalArgumentException(
						"Advanced regex features are not allowed. Only simple wildcard '*' is supported");
			}

			String regex = pattern
					.replace(".", "\\.")
					.replace("*", ".*")
					.replace("?", ".");

			regex = "^".concat(regex).concat("$");
			Pattern compiled = Pattern.compile(regex);

			SimpleSubscription<T> subscription = new SimpleSubscription<>(listener, eventType, effectivePriority);
			getSubscriberRegistry().register(compiled, subscription);

			return subscription;

		} catch (PatternSyntaxException e) {
			throw new IllegalArgumentException(
					format("Invalid pattern syntax: '%s'. Only simple wildcards like '*' are supported. Error: %s",
							pattern, e.getMessage()), e);
		} catch (Exception e) {
			throw new IllegalStateException(format("Failed to compile pattern: '%s'", pattern), e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> List<SimpleSubscription<T>> sortedByPriority(List<SimpleSubscription<?>> subscriptions) {
		return subscriptions.stream()
				.map(sub -> (SimpleSubscription<T>) sub)
				.sorted(Comparator.comparingInt(sub -> sub.getPriority().getLevel()))
				.toList();
	}

	private <T> Consumer<T> decorateWithFaultTolerance(Consumer<T> original, String reference) {
		EventBusConfig.FaultToleranceMode mode = config.getFaultToleranceMode();

		if (mode == EventBusConfig.FaultToleranceMode.DISABLED || mode == EventBusConfig.FaultToleranceMode.INTERNAL) {
			return original;
		}

		if (mode == EventBusConfig.FaultToleranceMode.RESILIENCE4J) {
			CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
					.failureRateThreshold(50)
					.waitDurationInOpenState(Duration.ofSeconds(30))
					.permittedNumberOfCallsInHalfOpenState(10)
					.ringBufferSizeInClosedState(100)
					.build();

			CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(cbConfig);
			CircuitBreaker cb = registry.circuitBreaker("listener-" + reference.hashCode());

			return event -> {
				cb.executeSupplier(() -> {
					original.accept(event);
					return null;
				});
			};
		}

		if (mode == EventBusConfig.FaultToleranceMode.SPRING_RETRY) {
			RetryTemplate retryTemplate = new RetryTemplate();

			SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(config.getMaxConsecutiveFailures());

			ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
			backOffPolicy.setInitialInterval(1000L);
			backOffPolicy.setMultiplier(2.0);
			backOffPolicy.setMaxInterval(10000L);

			retryTemplate.setRetryPolicy(retryPolicy);
			retryTemplate.setBackOffPolicy(backOffPolicy);

			return event -> retryTemplate.execute(context -> {
				original.accept(event);
				return null;
			});
		}

		return original;
	}
}
