package ir.artanpg.boot.kernel.eventbus;

import io.micrometer.common.util.StringUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AbstractEventBus implements EventBus {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final MeterRegistry meterRegistry;
	private final SubscriberRegistry subscriberRegistry;

	private ExecutorService executorService;

	protected AbstractEventBus(MeterRegistry meterRegistry, SubscriberRegistry subscriberRegistry) {
		if (meterRegistry == null) throw new IllegalArgumentException("the meterRegistry cannot be null");
		if (subscriberRegistry == null) throw new IllegalArgumentException("the subscriberRegistry cannot be null");

		this.meterRegistry = meterRegistry;
		this.subscriberRegistry = subscriberRegistry;

		ExecutorService fixedThreadPool =
				Executors.newSingleThreadExecutor(threadFactory -> {
							Thread thread = new Thread(threadFactory, "EventBus-Serial-Thread");
							thread.setDaemon(true);
							return thread;
						}
				);

		this.executorService = ExecutorServiceMetrics.monitor(meterRegistry, fixedThreadPool, "eventbus.executor");
	}

	protected AbstractEventBus(MeterRegistry meterRegistry,
							   ExecutorService executorService,
							   SubscriberRegistry subscriberRegistry) {
		if (meterRegistry == null) throw new IllegalArgumentException("the meterRegistry cannot be null");
		if (executorService == null) throw new IllegalArgumentException("the executorService cannot be null");
		if (subscriberRegistry == null) throw new IllegalArgumentException("the subscriberRegistry cannot be null");

		this.meterRegistry = meterRegistry;
		this.executorService = ExecutorServiceMetrics.monitor(meterRegistry, executorService, "eventbus.executor");
		this.subscriberRegistry = subscriberRegistry;
	}

	protected void incrementPublishCounter(String topic, String... additionalTags) {
		try {
			Tags tags = buildTags(topic, additionalTags);
			meterRegistry.counter("eventbus.publish.total", tags).increment();
		} catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to increment publish counter for topic: {}", topic, e);
			}
		}
	}

	private Tags buildTags(String topic, String... additionalTags) {
		var tags = Tags.of("topic", topic);

		if (additionalTags == null || additionalTags.length == 0) return tags;

		validateAdditionalTags(additionalTags);

		for (int i = 0; i < additionalTags.length; i += 2) {
			var key = additionalTags[i];
			var value = additionalTags[i + 1];

			if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
				tags = tags.and(key, value);
			} else {
				logger.debug("Skipping empty tag pair at index {}: key={}, value={}", i, key, value);
			}
		}

		return tags;
	}

	private void validateAdditionalTags(String[] additionalTags) {
		if (additionalTags.length % 2 != 0) {
			var exception =
					new IllegalArgumentException(
							String.format(
									"Additional tags must be in key-value pairs. Received %d tags",
									additionalTags.length));

			logger.error("Error while recording Metric: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	@Override
	public void shutdown() {
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	protected Logger getLogger() {
		return logger;
	}

	protected MeterRegistry getMeterRegistry() {
		return meterRegistry;
	}

	protected SubscriberRegistry getSubscriberRegistry() {
		return subscriberRegistry;
	}

	protected ExecutorService getExecutorService() {
		return executorService;
	}
}
