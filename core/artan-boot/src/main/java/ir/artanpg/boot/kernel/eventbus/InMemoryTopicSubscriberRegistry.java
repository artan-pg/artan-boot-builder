package ir.artanpg.boot.kernel.eventbus;

import ir.artanpg.boot.kernel.eventbus.model.EventListener;
import ir.artanpg.boot.kernel.eventbus.model.EventListenerRecord;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryTopicSubscriberRegistry implements SubscriberRegistry {

	private final AtomicLong registrationCounter = new AtomicLong(0);
	private final Map<String, List<EventListenerRecord>> topicSubscriptions = new ConcurrentHashMap<>();

	@Override
	public void register(String topic, EventListener eventListener) {
		if (topic == null || topic.isBlank()) throw new IllegalArgumentException("The topic cannot be null or empty");
		if (eventListener == null) throw new IllegalArgumentException("The eventListener cannot be null");

		topicSubscriptions
				.computeIfAbsent(topic, _ -> new CopyOnWriteArrayList<>())
				.add(EventListenerRecord.of(eventListener));

		topicSubscriptions.forEach((_, eventListenerRecordList) ->
				eventListenerRecordList.sort(
						Comparator
								.comparingInt((EventListenerRecord record) -> record.listener().priority())
								.thenComparingLong(_ -> registrationCounter.incrementAndGet())));
	}

	@Override
	public void remove(String topic, EventListener eventListener) {
		List<EventListenerRecord> eventListenerRecordList = topicSubscriptions.get(topic);
		if (eventListenerRecordList != null) {
			eventListenerRecordList.remove(EventListenerRecord.of(eventListener));
			if (eventListenerRecordList.isEmpty()) {
				topicSubscriptions.remove(topic);
			}
		}
	}

	@Override
	public List<EventListenerRecord> get(String topic) {
		return topicSubscriptions.getOrDefault(topic, List.of());
	}
}
