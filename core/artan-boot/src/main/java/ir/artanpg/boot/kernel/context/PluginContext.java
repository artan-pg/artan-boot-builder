package ir.artanpg.boot.kernel.context;

import io.micrometer.core.instrument.MeterRegistry;
import ir.artanpg.boot.kernel.PluginDescriptor;
import ir.artanpg.boot.kernel.eventbus.EventBus;
import ir.artanpg.boot.kernel.routing.Router;
import ir.artanpg.boot.kernel.service.SharedServiceRegistry;
import org.slf4j.Logger;

public interface PluginContext {

	/**
	 * بازگشت descriptor پلاگین جاری.
	 *
	 * @return descriptor پلاگین
	 */
	PluginDescriptor getDescriptor();

	/**
	 * دسترسی به logger اختصاصی پلاگین.
	 * نام logger بر اساس id پلاگین تنظیم می‌شود (مثل com.coreplugin.plugin.my-plugin).
	 *
	 * @return logger اختصاصی
	 */
	Logger getLogger();

	/**
	 * دسترسی به MeterRegistry برای ثبت metrics، counters، timers و ...
	 * تمام metrics با تگ plugin.id و plugin.version پیش‌تگ‌گذاری می‌شوند.
	 *
	 * @return registry مشترک observability
	 */
	MeterRegistry getMeterRegistry();

	/**
	 * دسترسی به EventBus مرکزی برای انتشار و دریافت رویدادها.
	 *
	 * @return event bus مشترک
	 */
	EventBus getEventBus();

	/**
	 * دسترسی به Router مرکزی برای ثبت endpointهای پروتکلی (REST, gRPC, WebSocket و ...).
	 * پلاگین‌ها باید routeهای تعریف‌شده در descriptor را اینجا ثبت کنند.
	 *
	 * @return router مرکزی
	 */
	Router getRouter();

	/**
	 * دسترسی به رجیستری سرویس‌های مشترک (Shared Services).
	 * پلاگین‌ها می‌توانند سرویس‌های خود را export کنند یا سرویس‌های دیگر پلاگین‌ها را consume کنند.
	 *
	 * @return رجیستری سرویس‌های مشترک
	 */
	SharedServiceRegistry getSharedServiceRegistry();

	/**
	 * دسترسی به مقدار یک کلید configuration از extensions در descriptor.
	 * متد کمکی برای دسترسی راحت‌تر.
	 *
	 * @param key  کلید در extensions
	 * @param <T>  نوع مورد انتظار
	 * @return مقدار یا null
	 */
	<T> T getConfig(String key, Class<T> type);

	/**
	 * دسترسی به مقدار configuration با مقدار پیش‌فرض.
	 *
	 * @param key          کلید
	 * @param defaultValue مقدار پیش‌فرض در صورت عدم وجود
	 * @param <T>          نوع
	 * @return مقدار موجود یا پیش‌فرض
	 */
	<T> T getConfig(String key, T defaultValue);
}
