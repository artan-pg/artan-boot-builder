package ir.artanpg.boot.kernel.eventbus;

import java.util.List;
import java.util.regex.Pattern;

public interface SubscriberRegistry {

	SubscriberRegistry DEFAUT_INSTANCE = new DefaultSubscriberRegistry();

	void register(String topic, SimpleSubscription<?> subscription);

	void register(Pattern pattern, SimpleSubscription<?> subscription);

	boolean removeSubscription(String topic, SimpleSubscription<?> subscription);

	boolean removePatternSubscription(Pattern pattern, SimpleSubscription<?> subscription);

	List<SimpleSubscription<?>> getSubscriber(String topic);

	List<PatternSubscription> getAllPatternSubscriptions();

	record PatternSubscription(Pattern pattern, List<SimpleSubscription<?>> subscriptions) {}
}
