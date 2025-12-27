package ir.artanpg.boot.kernel.eventbus.exception;

import ir.artanpg.boot.kernel.eventbus.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FaultTolerantExceptionHandler extends AbstractSubscriberExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(FaultTolerantExceptionHandler.class);

	private final Map<Object, Integer> failureCount = new ConcurrentHashMap<>();
	private final Map<Object, Subscription> subscriptions = new ConcurrentHashMap<>();
	private final int maxFailures;

	public FaultTolerantExceptionHandler(int maxFailures) {
		this.maxFailures = maxFailures;
	}

	public void trackSubscription(Object listenerIdentity, Subscription subscription) {
		subscriptions.put(listenerIdentity, subscription);
	}

	@Override
	public void handleException(String topic, Object event, Object listener, Throwable exception) {
		getLogger().error("Error in event listener: topic='{}', eventType='{}', listener='{}'",
				topic,
				event.getClass().getSimpleName(),
				listener.getClass().getSimpleName(),
				exception);

		int count = failureCount.merge(listener, 1, Integer::sum);

		if (count >= maxFailures) {
			Subscription subscription = subscriptions.remove(listener);
			if (subscription != null && subscription.isActive()) {
				subscription.close();
				logger.info("Listener {} was disabled due to {} errors being thrown",
						listener.getClass().getName(), count, exception);
			}
			failureCount.remove(listener);
		}
	}
}
