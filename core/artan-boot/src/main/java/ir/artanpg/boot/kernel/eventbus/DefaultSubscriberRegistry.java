package ir.artanpg.boot.kernel.eventbus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class DefaultSubscriberRegistry implements SubscriberRegistry {

	private final Map<String, List<SimpleSubscription<?>>> exactSubscriptions = new ConcurrentHashMap<>();
	private final Map<Pattern, List<SimpleSubscription<?>>> patternSubscriptions = new ConcurrentHashMap<>();

	@Override
	public void register(String topic, SimpleSubscription<?> subscription) {
		exactSubscriptions.computeIfAbsent(topic, _ -> new ArrayList<>()).add(subscription);
	}

	@Override
	public void register(Pattern pattern, SimpleSubscription<?> subscription) {
		patternSubscriptions.computeIfAbsent(pattern, _ -> new ArrayList<>()).add(subscription);
	}

	@Override
	public boolean removeSubscription(String topic, SimpleSubscription<?> subscription) {
		List<SimpleSubscription<?>> list = exactSubscriptions.get(topic);
		if (list != null) {
			boolean removed = list.remove(subscription);
			if (list.isEmpty()) {
				exactSubscriptions.remove(topic);
			}
			return removed;
		}
		return false;
	}

	@Override
	public boolean removePatternSubscription(Pattern pattern, SimpleSubscription<?> subscription) {
		List<SimpleSubscription<?>> list = patternSubscriptions.get(pattern);
		if (list != null) {
			boolean removed = list.remove(subscription);
			if (list.isEmpty()) {
				patternSubscriptions.remove(pattern);
			}
			return removed;
		}
		return false;
	}

	@Override
	public List<SimpleSubscription<?>> getSubscriber(String topic) {
		return exactSubscriptions.getOrDefault(topic, List.of());
	}

	@Override
	public List<PatternSubscription> getAllPatternSubscriptions() {
		return patternSubscriptions.entrySet().stream()
				.map(entry -> new PatternSubscription(entry.getKey(), new ArrayList<>(entry.getValue())))
				.toList();
	}
}
