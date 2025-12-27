package ir.artanpg.boot.kernel.eventbus;

import io.micrometer.common.util.StringUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import ir.artanpg.boot.kernel.eventbus.exception.LoggingAndMetricsExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class AbstractEventBus implements EventBus {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final SubscriberRegistry subscriberRegistry;
	private final ExecutorService executorService;
	private final MeterRegistry meterRegistry;

	private final SubscriberExceptionHandler exceptionHandler;

	protected AbstractEventBus() {
		this(null);
	}

	protected AbstractEventBus(MeterRegistry meterRegistry) {
		this(
				meterRegistry,
				DefaultSubscriberRegistry.DEFAUT_INSTANCE,
				meterRegistry == null ?
						SubscriberExceptionHandler.DEFAUT_INSTANCE :
						new LoggingAndMetricsExceptionHandler(meterRegistry));
	}

	protected AbstractEventBus(MeterRegistry meterRegistry,
							   SubscriberRegistry subscriberRegistry,
							   SubscriberExceptionHandler exceptionHandler) {
		this.meterRegistry = meterRegistry;
		this.subscriberRegistry = subscriberRegistry != null ? subscriberRegistry : SubscriberRegistry.DEFAUT_INSTANCE;
		this.exceptionHandler = exceptionHandler != null ? exceptionHandler : SubscriberExceptionHandler.DEFAUT_INSTANCE;

		ExecutorService fixedThreadPool = new ThreadPoolExecutor(
				1,
				1,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(),
				threadFactory -> {
					Thread thread = new Thread(threadFactory, "EventBus-SerialWorker");
					thread.setDaemon(true);
					thread.setUncaughtExceptionHandler((thread1, throwable) -> {
						var handler =
								exceptionHandler != null ? exceptionHandler : SubscriberExceptionHandler.DEFAUT_INSTANCE;
						handler.handleRejectedExecution(thread1, throwable);
					});
					return thread;
				},
				(runnable, _) -> {
					var handler =
							exceptionHandler != null ? exceptionHandler : SubscriberExceptionHandler.DEFAUT_INSTANCE;
					Throwable rejectedEx = new RejectedExecutionException("Publish rejected - EventBus is shutting down");
					handler.handleRejectedExecution(runnable, rejectedEx);

					if (meterRegistry != null) {
						meterRegistry.counter("eventbus.publish.rejected", "reason", "shutdown").increment();
					}
				}
		);

		if (meterRegistry != null) {
			this.executorService =
					ExecutorServiceMetrics
							.monitor(meterRegistry, fixedThreadPool, "eventbus.executor", Tags.of("type", "serial"));
		} else {
			this.executorService = fixedThreadPool;
		}
	}

	protected void incrementPublishCounter(String topic, String... additionalTags) {
		if (meterRegistry == null) return;

		var effectiveTopic = topic != null ? topic : DEFUALT_TOPIC_NAME;

		try {
			var tags = buildTags(effectiveTopic, additionalTags);
			meterRegistry.counter("eventbus.publish.total", tags).increment();
		} catch (Exception e) {
			logger.warn("Failed to increment publish counter for topic: {}", topic, e);
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
	public SubscriberExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

	@Override
	public SubscriberRegistry getSubscriberRegistry() {
		return subscriberRegistry;
	}

	@Override
	public ExecutorService getExecutorService() {
		return executorService;
	}

	@Override
	public MeterRegistry getMeterRegistry() {
		return meterRegistry;
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
}
