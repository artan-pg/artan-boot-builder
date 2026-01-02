package ir.artanpg.boot.kernel.eventbus;

import ir.artanpg.boot.kernel.eventbus.model.EventListener;
import ir.artanpg.boot.kernel.eventbus.model.EventListenerRecord;

import java.util.List;

public interface SubscriberRegistry {

	void register(String topic, EventListener eventListener);

	void remove(String topic, EventListener eventListener);

	List<EventListenerRecord> get(String topic);
}
