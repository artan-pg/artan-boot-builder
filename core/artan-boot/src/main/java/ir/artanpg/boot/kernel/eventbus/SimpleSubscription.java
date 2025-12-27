package ir.artanpg.boot.kernel.eventbus;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

class SimpleSubscription<T> implements Subscription {

	private final Consumer<T> listener;
	private final Class<T> eventType;
	private final Priority priority;
	private final Object identity;
	private final Runnable onUnsubscribe;

	private final AtomicBoolean active = new AtomicBoolean(true);

	SimpleSubscription(Consumer<T> listener, Class<T> eventType, Priority priority, Runnable onUnsubscribe) {
		this.listener = listener;
		this.eventType = eventType;
		this.priority = priority != null ? priority : Priority.NORMAL;
		this.identity = listener;
		this.onUnsubscribe = onUnsubscribe;
	}

	void invoke(Object event, String topic, SubscriberExceptionHandler handler) {
		if (!active.get()) return;

		if (this.eventType.isInstance(event)) {
			try {
				listener.accept(this.eventType.cast(event));
			} catch (Throwable ex) {
				handler.handleException(topic, event, identity, ex);
			}
		}
	}

	@Override
	public void close() {
		unSubscribe();
	}

	@Override
	public boolean isActive() {
		return active.get();
	}

	@Override
	public boolean unSubscribe() {
		if (active.compareAndSet(true, false)) {
			if (onUnsubscribe != null) {
				onUnsubscribe.run();
			}
			return true;
		}
		return false;
	}

	@Override
	public Priority getPriority() {
		return priority;
	}

	Object getIdentity() {
		return identity;
	}
}
