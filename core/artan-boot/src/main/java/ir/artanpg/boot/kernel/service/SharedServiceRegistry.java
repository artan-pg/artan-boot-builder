package ir.artanpg.boot.kernel.service;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public class SharedServiceRegistry {

	// cache سرویس‌ها بر اساس نوع و pluginId
	private final Map<Class<?>, Map<String, Object>> services = new ConcurrentHashMap<>();

	/**
	 * ثبت یک سرویس توسط پلاگین
	 *
	 * @param pluginId شناسه پلاگین
	 * @param serviceType نوع اینترفیس سرویس
	 * @param implementation پیاده‌سازی سرویس
	 * @param <T> نوع سرویس
	 */
	public <T> void registerService(String pluginId, Class<T> serviceType, T implementation) {
		services.computeIfAbsent(serviceType, k -> new ConcurrentHashMap<>())
				.put(pluginId, implementation);
	}

	/**
	 * کشف تمام پیاده‌سازی‌های یک سرویس با استفاده از SPI و ClassLoader پلاگین
	 * (این متد بعداً با ClassLoader پلاگین فراخوانی می‌شه)
	 */
	public <T> Iterable<T> discoverServices(Class<T> serviceType, ClassLoader pluginClassLoader) {
		return ServiceLoader.load(serviceType, pluginClassLoader);
	}

	/**
	 * دریافت یک سرویس خاص از یک پلاگین خاص
	 */
	@SuppressWarnings("unchecked")
	public <T> T getService(String pluginId, Class<T> serviceType) {
		Map<String, Object> pluginServices = services.get(serviceType);
		if (pluginServices != null) {
			return (T) pluginServices.get(pluginId);
		}
		return null;
	}

	/**
	 * دریافت تمام سرویس‌های یک نوع (از همه پلاگین‌ها)
	 */
	@SuppressWarnings("unchecked")
	public <T> Map<String, T> getAllServices(Class<T> serviceType) {
		Map<String, Object> map = services.getOrDefault(serviceType, Map.of());
		return (Map<String, T>) Map.copyOf(map);
	}
}
