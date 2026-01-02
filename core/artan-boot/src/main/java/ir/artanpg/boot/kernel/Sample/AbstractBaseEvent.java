package ir.artanpg.boot.kernel.Sample;

import ir.artanpg.boot.kernel.eventbus.model.Event;

import java.io.Serial;
import java.time.Instant;
import java.util.UUID;

public abstract class AbstractBaseEvent implements Event {
	@Serial
	private static final long serialVersionUID = 1803673722177234069L;

	private final Object source;

	/**
	 * Constructs a prototypical Event.
	 *
	 * @param source the object on which the Event initially occurred
	 * @throws IllegalArgumentException if source is null
	 */
	public AbstractBaseEvent(Object source) {
		this.source = source;
	}

	@Override
	public String identifier() {
		return UUID.randomUUID().toString();
	}

	@Override
	public Long timestamp() {
		return Instant.now().getEpochSecond();
	}

	@Override
	public Object source() {
		return source;
	}
}
