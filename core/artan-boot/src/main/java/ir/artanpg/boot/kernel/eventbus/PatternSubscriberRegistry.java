package ir.artanpg.boot.kernel.eventbus;

import ir.artanpg.boot.kernel.eventbus.model.EventListener;
import ir.artanpg.boot.kernel.eventbus.model.EventListenerRecord;

import java.util.List;
import java.util.regex.Pattern;

public interface PatternSubscriberRegistry extends SubscriberRegistry {

	void register(Pattern pattern, EventListener eventListener);

	void remove(Pattern pattern, EventListener eventListener);

	List<EventListenerRecord> get(Pattern pattern);
}
