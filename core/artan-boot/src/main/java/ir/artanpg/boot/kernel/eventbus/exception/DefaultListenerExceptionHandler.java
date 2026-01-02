package ir.artanpg.boot.kernel.eventbus.exception;

import ir.artanpg.boot.kernel.eventbus.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultListenerExceptionHandler implements ListenerExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(DefaultListenerExceptionHandler.class);

	private static DefaultListenerExceptionHandler INSTANCE;

	private DefaultListenerExceptionHandler() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	@Override
	public void handle(Exception exception, Event event) {
		logger.error(
				"Error invoking in the EventListener handler: the Event is: {}",
				event.getClass().getSimpleName(), exception);
	}

	public static synchronized DefaultListenerExceptionHandler getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new DefaultListenerExceptionHandler();
		}
		return INSTANCE;
	}
}
