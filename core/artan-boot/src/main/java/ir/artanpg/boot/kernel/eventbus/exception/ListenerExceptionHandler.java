package ir.artanpg.boot.kernel.eventbus.exception;

import ir.artanpg.boot.kernel.eventbus.model.Event;

public interface ListenerExceptionHandler {

	/**
	 * Handles an exception that occurred while processing an event.
	 *
	 * @param exception the thrown exception
	 * @param event     the event being processed
	 */
	void handle(Exception exception, Event event);
}
