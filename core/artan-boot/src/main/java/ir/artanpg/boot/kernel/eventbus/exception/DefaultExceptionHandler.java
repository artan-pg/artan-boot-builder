package ir.artanpg.boot.kernel.eventbus.exception;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultExceptionHandler extends AbstractSubscriberExceptionHandler {

	private final static Integer DEFAULT_MAX_CONSECUTIVE_FAILURES = 5;
	private final Map<Object, AtomicInteger> failureCounts = new ConcurrentHashMap<>();

	private int maxConsecutiveFailures = DEFAULT_MAX_CONSECUTIVE_FAILURES;

	@Override
	public void handleException(String topic, Object event, Object listener, Throwable exception) {
		AtomicInteger count = failureCounts.computeIfAbsent(listener, _ -> new AtomicInteger(0));
		int failures = count.incrementAndGet();

		getLogger().error("Error in event listener: topic='{}', event='{}', listener='{}'",
				topic,
				event.getClass().getSimpleName(),
				listener.getClass().getSimpleName(),
				exception);

		if (failures >= maxConsecutiveFailures) {
			subscription.unsubscribe(); // حذف واقعی از registry
			getLogger().debug("Listener {} automatically disabled after {} consecutive failures", listener, failures);
		}
	}

	public int getMaxConsecutiveFailures() {
		return maxConsecutiveFailures;
	}

	public void setMaxConsecutiveFailures(int maxConsecutiveFailures) {
		this.maxConsecutiveFailures = maxConsecutiveFailures;
	}
}
