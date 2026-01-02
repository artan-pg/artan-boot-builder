package ir.artanpg.boot.kernel.eventbus.model;

import java.io.Serializable;

public interface Event extends Serializable {

	String getName();

	EventType getEventType();

	String identifier();

	Long timestamp();

	Object source();

	Class<? extends Event> typeSource();
}
