package ir.artanpg.boot.kernel.eventbus;

import ir.artanpg.boot.kernel.eventbus.model.EventListener;
import ir.artanpg.boot.kernel.eventbus.model.EventListenerRecord;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public class InMemoryPaternSubscriberRegistry extends InMemoryTopicSubscriberRegistry implements PatternSubscriberRegistry {

	private final AtomicLong registrationCounter = new AtomicLong(0);
	private final Map<Pattern, List<EventListenerRecord>> patternSubscriptions = new ConcurrentHashMap<>();

	@Override
	public void register(Pattern pattern, EventListener eventListener) {
		patternSubscriptions
				.computeIfAbsent(pattern, _ -> new CopyOnWriteArrayList<>())
				.add(EventListenerRecord.of(eventListener));

		patternSubscriptions.forEach((_, eventListenerRecordList) ->
				eventListenerRecordList.sort(
						Comparator
								.comparingInt((EventListenerRecord record) -> record.listener().priority())
								.thenComparingLong(_ -> registrationCounter.incrementAndGet())));
	}

	@Override
	public void remove(Pattern pattern, EventListener eventListener) {
		List<EventListenerRecord> eventListenerRecordList = patternSubscriptions.get(pattern);
		if (eventListenerRecordList != null) {
			eventListenerRecordList.remove(EventListenerRecord.of(eventListener));
			if (eventListenerRecordList.isEmpty()) {
				patternSubscriptions.remove(pattern);
			}
		}
	}

	@Override
	public List<EventListenerRecord> get(Pattern pattern) {
		return patternSubscriptions.getOrDefault(pattern, List.of());
	}
}
