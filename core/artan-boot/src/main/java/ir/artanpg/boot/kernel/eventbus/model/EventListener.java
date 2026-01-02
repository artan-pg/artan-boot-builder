package ir.artanpg.boot.kernel.eventbus.model;

import ir.artanpg.boot.kernel.eventbus.exception.DefaultListenerExceptionHandler;
import ir.artanpg.boot.kernel.eventbus.exception.ListenerExceptionHandler;

import java.util.Objects;
import java.util.UUID;

@FunctionalInterface
public interface EventListener {

	void handle(Event event);

	default int priority() {
		return 0;
	}

	default UUID identifier() {
		return UUID.randomUUID();
	}

	default ListenerExceptionHandler exceptionHandler() {
		return DefaultListenerExceptionHandler.getInstance();
	}

	default EventListener andThen(EventListener after) {
		Objects.requireNonNull(after);
		return (Event event) -> {
			handle(event);
			after.handle(event);
		};
	}
}
