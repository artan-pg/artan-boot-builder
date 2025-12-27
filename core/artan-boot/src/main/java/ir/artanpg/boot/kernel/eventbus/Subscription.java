package ir.artanpg.boot.kernel.eventbus;

public interface Subscription extends AutoCloseable {

	void close();

	boolean isActive();

	boolean unSubscribe();

	Priority getPriority();

	enum Priority {
		HIGHEST(1),
		HIGH(2),
		NORMAL(3),
		LOW(4),
		LOWEST(5);

		private final int level;

		Priority(int level) {
			this.level = level;
		}

		public int getLevel() {
			return level;
		}
	}
}
