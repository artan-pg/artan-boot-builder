package ir.artanpg.boot.kernel.routing;

import ir.artanpg.boot.kernel.PluginDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Router {

	private final List<RegisteredRoute> routes = new CopyOnWriteArrayList<>();

	/**
	 * ثبت یک route از descriptor پلاگین
	 */
	public void registerRoute(String pluginId, PluginDescriptor.RouteDefinition routeDef) {
		RegisteredRoute route = new RegisteredRoute(
				pluginId,
				routeDef.getProtocol(),
				routeDef.getPath(),
				routeDef.getMethod(),
				routeDef.getHandlerClass()
		);
		routes.add(route);
	}

	/**
	 * ثبت چندین route
	 */
	public void registerRoutes(String pluginId, List<PluginDescriptor.RouteDefinition> routeDefs) {
		if (routeDefs != null) {
			routeDefs.forEach(def -> registerRoute(pluginId, def));
		}
	}

	/**
	 * لیست تمام routeهای ثبت‌شده
	 */
	public List<RegisteredRoute> getAllRoutes() {
		return new ArrayList<>(routes);
	}

	/**
	 * پیدا کردن route بر اساس protocol و path
	 */
	public List<RegisteredRoute> findRoutes(String protocol, String path) {
		return routes.stream().filter(r -> protocol.equalsIgnoreCase(r.protocol()) && path.equals(r.path())).toList();
	}

	/**
	 * رکورد داخلی برای نگهداری route ثبت‌شده
	 */
	public record RegisteredRoute(
			String pluginId,
			String protocol,
			String path,
			String method,
			String handlerClass
	) {}
}
