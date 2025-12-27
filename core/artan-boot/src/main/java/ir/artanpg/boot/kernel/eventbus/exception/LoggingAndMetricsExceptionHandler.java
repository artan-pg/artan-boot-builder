package ir.artanpg.boot.kernel.eventbus.exception;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class LoggingAndMetricsExceptionHandler extends DefaultExceptionHandler {
	private final Counter errorCounter;

	public LoggingAndMetricsExceptionHandler(MeterRegistry meterRegistry) {
		if (meterRegistry != null) {
			this.errorCounter = Counter.builder("plugin.eventbus.listener.errors")
					.description("Number of EventBus listener errors")
					.register(meterRegistry);
		} else {
			this.errorCounter = null;
		}
	}

	@Override
	public void handleException(String topic, Object event, Object listener, Throwable exception) {
		if (errorCounter != null) {
			errorCounter.increment();
		}
		super.handleException(topic, event, listener, exception);
	}

	@Override
	public void handleRejectedExecution(Runnable runnable, Throwable rejectedException) {
		if (errorCounter != null) {
			errorCounter.increment();
		}
		super.handleRejectedExecution(runnable, rejectedException);
	}
}
