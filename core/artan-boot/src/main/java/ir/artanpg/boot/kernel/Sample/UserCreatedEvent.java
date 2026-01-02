package ir.artanpg.boot.kernel.Sample;

import ir.artanpg.boot.kernel.eventbus.model.Event;
import ir.artanpg.boot.kernel.eventbus.model.EventType;

import java.io.Serial;

public class UserCreatedEvent extends AbstractBaseEvent {
	@Serial
	private static final long serialVersionUID = -8517578291518981557L;

	public UserCreatedEvent(UserEntity userEntity) {
		super(userEntity);
	}

	@Override
	public String getName() {
		return UserEventType.USER_CREATED.getTitle();
	}

	@Override
	public EventType getEventType() {
		return UserEventType.USER_CREATED;
	}

	@Override
	public Class<? extends Event> typeSource() {
		return UserCreatedEvent.class;
	}
}
