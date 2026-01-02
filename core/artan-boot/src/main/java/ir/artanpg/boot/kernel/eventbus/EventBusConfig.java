package ir.artanpg.boot.kernel.eventbus;

import java.time.Duration;

public class EventBusConfig {

	public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
	public static final Duration DEFAULT_EXECUTOR_KEEP_ALIVE = Duration.ofSeconds(60);

	private String defaultTopicName = "default";

	private int executorCorePoolSize;
	private int executorMaxPoolSize;

	private Duration timeout;
	private Duration executorKeepAlive;

	private RetryConfig retry = new RetryConfig();

	public String getDefaultTopicName() {
		return defaultTopicName;
	}

	public void setDefaultTopicName(String defaultTopicName) {
		this.defaultTopicName = defaultTopicName;
	}

	public int getExecutorCorePoolSize() {
		return executorCorePoolSize;
	}

	public void setExecutorCorePoolSize(int executorCorePoolSize) {
		this.executorCorePoolSize = executorCorePoolSize;
	}

	public int getExecutorMaxPoolSize() {
		return executorMaxPoolSize;
	}

	public void setExecutorMaxPoolSize(int executorMaxPoolSize) {
		this.executorMaxPoolSize = executorMaxPoolSize;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public Duration getExecutorKeepAlive() {
		return executorKeepAlive;
	}

	public void setExecutorKeepAlive(Duration executorKeepAlive) {
		this.executorKeepAlive = executorKeepAlive;
	}

	public RetryConfig getRetry() {
		return retry;
	}

	public void setRetry(RetryConfig retry) {
		this.retry = retry;
	}

	public static class RetryConfig {
		private int maxAttempts = 3;
		private long backoffMs = 1000;
		private String dlqTopic = "eventbus.dlq";

		public int getMaxAttempts() {
			return maxAttempts;
		}

		public void setMaxAttempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
		}

		public long getBackoffMs() {
			return backoffMs;
		}

		public void setBackoffMs(long backoffMs) {
			this.backoffMs = backoffMs;
		}

		public String getDlqTopic() {
			return dlqTopic;
		}

		public void setDlqTopic(String dlqTopic) {
			this.dlqTopic = dlqTopic;
		}
	}
}
