package ir.artanpg.boot.kernel.eventbus.model;

public record EventListenerRecord(EventListener listener) {

	public static EventListenerRecord of(EventListener listener) {
		return new EventListenerRecord(listener);
	}
}
