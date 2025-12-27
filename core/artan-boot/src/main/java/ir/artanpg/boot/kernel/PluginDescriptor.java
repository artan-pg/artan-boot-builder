package ir.artanpg.boot.kernel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginDescriptor {

	private String id;                    // اجباری - شناسه منحصر به فرد
	private String name;                  // اختیاری - نام خوانا
	private String version;               // اجباری - نسخه پلاگین
	private String description;           // اختیاری
	private String mainClass;             // اجباری - نام کامل کلاس اصلی که Plugin را پیاده می‌کند
	private List<String> dependencies;    // اختیاری - لیست id پلاگین‌های وابسته
	private List<RouteDefinition> routes; // اختیاری - تعریف endpointهای پروتکلی
	private List<EventSubscription> eventSubscriptions; // اختیاری - اشتراک رویدادها

	private final Map<String, Object> extensions = new HashMap<>(); // فیلدهای اضافی و سفارشی

	// ------------------- Getters & Setters با JavaDoc -------------------

	/** شناسه منحصر به فرد پلاگین (اجباری) */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/** نام خوانا برای پلاگین */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/** نسخه پلاگین (توصیه: Semantic Versioning) */
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/** نام کامل کلاس اصلی که باید {@link Plugin} را پیاده‌سازی کند */
	public String getMainClass() {
		return mainClass;
	}

	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	/** لیست شناسه پلاگین‌هایی که این پلاگین به آن‌ها وابسته است */
	public List<String> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<String> dependencies) {
		this.dependencies = dependencies;
	}

	/** تعریف مسیرهای پروتکلی (REST, gRPC, WebSocket و ...) */
	public List<RouteDefinition> getRoutes() {
		return routes;
	}

	public void setRoutes(List<RouteDefinition> routes) {
		this.routes = routes;
	}

	/** تعریف اشتراک‌های رویداد برای Event Bus */
	public List<EventSubscription> getEventSubscriptions() {
		return eventSubscriptions;
	}

	public void setEventSubscriptions(List<EventSubscription> eventSubscriptions) {
		this.eventSubscriptions = eventSubscriptions;
	}

	/**
	 * دسترسی به فیلدهای اضافی تعریف‌شده در plugin.yaml یا plugin.properties
	 * که جزو فیلدهای استاندارد نیستند.
	 *
	 * @return نقشه غیرقابل تغییر از extensions
	 */
	public Map<String, Object> getExtensions() {
		return Collections.unmodifiableMap(extensions);
	}

	/**
	 * تنظیم extensions (معمولاً توسط descriptor loader فراخوانی می‌شود)
	 */
	public void setExtensions(Map<String, Object> extensions) {
		if (extensions != null) {
			this.extensions.putAll(extensions);
		}
	}

	/**
	 * متد کمکی برای دسترسی تایپ‌شده به یک extension خاص
	 *
	 * @param key  کلید extension
	 * @param type نوع مورد انتظار
	 * @param <T>  نوع بازگشتی
	 * @return مقدار extension یا null اگر وجود نداشته باشد
	 * @throws ClassCastException اگر نوع ناسازگار باشد
	 */
	@SuppressWarnings("unchecked")
	public <T> T getExtension(String key, Class<T> type) {
		Object value = extensions.get(key);
		if (value == null) {
			return null;
		}
		if (type.isInstance(value)) {
			return (T) value;
		}
		throw new ClassCastException("Extension '" + key + "' cannot be cast to " + type.getName());
	}

	// ------------------- Inner Classes -------------------

	/**
	 * تعریف یک route برای پروتکل‌های ارتباطی (REST, gRPC, WebSocket و غیره)
	 */
	public static class RouteDefinition {
		private String protocol; // مثال: "rest", "grpc", "websocket", "mq"
		private String path;     // مثال: "/api/users", "com.example.UserService"
		private String method;   // برای HTTP: GET, POST, ... | برای دیگران می‌تواند خالی باشد
		private String handlerClass; // اختیاری - کلاس هندلر در پلاگین

		public String getProtocol() { return protocol; }
		public void setProtocol(String protocol) { this.protocol = protocol; }

		public String getPath() { return path; }
		public void setPath(String path) { this.path = path; }

		public String getMethod() { return method; }
		public void setMethod(String method) { this.method = method; }

		public String getHandlerClass() { return handlerClass; }
		public void setHandlerClass(String handlerClass) { this.handlerClass = handlerClass; }
	}

	/**
	 * تعریف اشتراک یک رویداد در Event Bus
	 */
	public static class EventSubscription {
		private String topic;        // موضوع یا pattern (مثل "user.*" یا "order/created")
		private String eventType;    // نوع کلاس رویداد (اختیاری، برای فیلتر تایپی)
		private String handlerClass; // کلاس هندلر در پلاگین

		public String getTopic() { return topic; }
		public void setTopic(String topic) { this.topic = topic; }

		public String getEventType() { return eventType; }
		public void setEventType(String eventType) { this.eventType = eventType; }

		public String getHandlerClass() { return handlerClass; }
		public void setHandlerClass(String handlerClass) { this.handlerClass = handlerClass; }
	}
}
