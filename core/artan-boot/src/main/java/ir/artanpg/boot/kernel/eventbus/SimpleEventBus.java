package ir.artanpg.boot.kernel.eventbus;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import ir.artanpg.boot.kernel.eventbus.model.Event;
import ir.artanpg.boot.kernel.eventbus.model.EventListener;
import ir.artanpg.boot.kernel.eventbus.model.EventListenerRecord;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleEventBus extends AbstractEventBus {

	private final MeterRegistry meterRegistry;
	private final SubscriberRegistry subscriberRegistry;

	private final EventBusConfig eventBusConfig;

	public SimpleEventBus(MeterRegistry meterRegistry,
						  SubscriberRegistry subscriberRegistry,
						  EventBusConfig eventBusConfig) {
		super(meterRegistry, subscriberRegistry);

		this.meterRegistry = meterRegistry;
		this.subscriberRegistry = subscriberRegistry;
		this.eventBusConfig = eventBusConfig;
	}

	@Override
	public void publish(Event event) {
		publish(eventBusConfig.getDefaultTopicName(), event);
	}

	@Override
	public void publish(String topic, Event event) {
		Counter.builder("eventbus.published")
				.tag("topic", topic)
				.tag("type", event.typeSource().getSimpleName())
				.tag("bus", "simple")
				.register(meterRegistry)
				.increment();

		List<EventListenerRecord> eventListenerRecords = subscriberRegistry.get(topic);
		if (eventListenerRecords == null || eventListenerRecords.isEmpty()) {
			if (getLogger().isTraceEnabled()) {
				getLogger().trace("No listeners are registered for event {}", event.typeSource().getSimpleName());
			}
			return;
		}

		List<EventListenerRecord> eventListenerRecordsCopy = new CopyOnWriteArrayList<>(eventListenerRecords);
		getExecutorService().execute(() -> {
			for (EventListenerRecord record : eventListenerRecordsCopy) {
				try {
					record.listener().handle(event);
					Counter.builder("eventbus.success")
							.tag("topic", event.getName())
							.tag("eventType", event.getEventType().getTitle())
							.tag("identifier", event.identifier())
							.tag("timestamp", event.timestamp().toString())
							.tag("source", event.source().toString())
							.tag("bus", "simple")
							.register(meterRegistry)
							.increment();
				} catch (Exception ex) {
					getLogger().error("Error invoking handler: {}", record.listener().getClass(), ex);
					record.listener().exceptionHandler().handle(ex, event);
					Counter.builder("eventbus.listener.error")
							.tag("type", event.typeSource().getSimpleName())
							.tag("bus", "simple")
							.register(meterRegistry)
							.increment();
				}
			}
		});
	}

	@Override
	public void registerSubscribe(String topic, EventListener eventListener) {
		subscriberRegistry.register(topic, eventListener);
	}

	@Override
	public void removeSubscribe(String topic, EventListener eventListener) {
		subscriberRegistry.remove(topic, eventListener);
	}
}
