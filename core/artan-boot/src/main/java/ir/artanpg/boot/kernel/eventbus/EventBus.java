package ir.artanpg.boot.kernel.eventbus;

import ir.artanpg.boot.kernel.eventbus.model.Event;
import ir.artanpg.boot.kernel.eventbus.model.EventListener;

public interface EventBus {

	void publish(Event event);

	void publish(String topic, Event event);

	void registerSubscribe(String topic, EventListener eventListener);

	void removeSubscribe(String topic, EventListener eventListener);

	void shutdown();
}
