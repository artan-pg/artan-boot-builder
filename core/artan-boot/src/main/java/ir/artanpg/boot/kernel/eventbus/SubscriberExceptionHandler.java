package ir.artanpg.boot.kernel.eventbus;

import ir.artanpg.boot.kernel.eventbus.exception.DefaultExceptionHandler;
import org.slf4j.LoggerFactory;

@FunctionalInterface
public interface SubscriberExceptionHandler {

	SubscriberExceptionHandler DEFAUT_INSTANCE = new DefaultExceptionHandler();

	void handleException(String topic, Object event, Object listener, Throwable exception);

	default void handleRejectedExecution(Runnable runnable, Throwable rejectedException) {
		LoggerFactory
				.getLogger(getClass())
				.error("Event publish rejected in the runnable `{}`", runnable.getClass().getName(), rejectedException);
	}
}
