package ir.artanpg.boot.kernel.context;

import io.micrometer.core.instrument.MeterRegistry;
import ir.artanpg.boot.kernel.PluginDescriptor;
import ir.artanpg.boot.kernel.eventbus.EventBus;
import ir.artanpg.boot.kernel.routing.Router;
import ir.artanpg.boot.kernel.service.SharedServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractPluginContext implements ConfigurablePluginContext {

	private final PluginDescriptor descriptor;
	private final Logger logger;
	private final MeterRegistry meterRegistry;
	private final EventBus eventBus;
	private final Router router;
	private final SharedServiceRegistry sharedServiceRegistry;

	public AbstractPluginContext(PluginDescriptor descriptor,
								MeterRegistry meterRegistry,
								EventBus eventBus,
								Router router,
								SharedServiceRegistry sharedServiceRegistry) {
		this.descriptor = descriptor;
		this.meterRegistry = meterRegistry;
		this.eventBus = eventBus;
		this.router = router;
		this.sharedServiceRegistry = sharedServiceRegistry;

		// Logger اختصاصی با نام بر اساس id پلاگین
		String loggerName = "plugin." + descriptor.getId();
		this.logger = LoggerFactory.getLogger(loggerName);
	}

	@Override
	public PluginDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public MeterRegistry getMeterRegistry() {
		return meterRegistry;
	}

	@Override
	public EventBus getEventBus() {
		return eventBus;
	}

	@Override
	public Router getRouter() {
		return router;
	}

	@Override
	public SharedServiceRegistry getSharedServiceRegistry() {
		return sharedServiceRegistry;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getConfig(String key, Class<T> type) {
		return descriptor.getExtension(key, type);
	}

	@Override
	public <T> T getConfig(String key, T defaultValue) {
		T value = getConfig(key, (Class<T>) defaultValue.getClass());
		return value != null ? value : defaultValue;
	}
}
