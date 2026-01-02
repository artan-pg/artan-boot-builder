package ir.artanpg.boot.kernel.Sample;

import ir.artanpg.boot.kernel.eventbus.model.EventType;

public enum UserEventType implements EventType {

	USER_CREATED("user.created"),
	USER_DELETED("user.deleted"),
	;

	private final String title;

	UserEventType(String title) {
		this.title = title;
	}

	@Override
	public String getTitle() {
		return this.title;
	}
}
