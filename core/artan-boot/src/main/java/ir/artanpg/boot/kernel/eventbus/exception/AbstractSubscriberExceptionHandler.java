package ir.artanpg.boot.kernel.eventbus.exception;

import ir.artanpg.boot.kernel.eventbus.SubscriberExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSubscriberExceptionHandler implements SubscriberExceptionHandler {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected Logger getLogger() {
		return logger;
	}
}
