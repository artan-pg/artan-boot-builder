package ir.artanpg.boot.kernel.context;

import io.micrometer.core.instrument.MeterRegistry;
import ir.artanpg.boot.kernel.PluginDescriptor;
import ir.artanpg.boot.kernel.eventbus.EventBus;
import ir.artanpg.boot.kernel.routing.Router;
import ir.artanpg.boot.kernel.service.SharedServiceRegistry;

public class DefaultPluginContext extends AbstractPluginContext {


	public DefaultPluginContext(PluginDescriptor descriptor,
								MeterRegistry meterRegistry,
								EventBus eventBus,
								Router router,
								SharedServiceRegistry sharedServiceRegistry) {
		super(descriptor, meterRegistry, eventBus, router, sharedServiceRegistry);
	}
}
