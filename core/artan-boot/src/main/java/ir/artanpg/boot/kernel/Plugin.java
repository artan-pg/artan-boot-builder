package ir.artanpg.boot.kernel;

import ir.artanpg.boot.kernel.context.PluginContext;

public interface Plugin {

	/**
	 * متد اولیه‌سازی پلاگین.
	 * در این متد پلاگین می‌تواند منابع اولیه (مانند configuration خواندن) را بارگذاری کند.
	 *
	 * @param context контекст پلاگین که دسترسی به سرویس‌های مشترک، descriptor و manager را فراهم می‌کند
	 * @throws Exception اگر خطایی در اولیه‌سازی رخ دهد
	 */
	void initialize(PluginContext context) throws Exception;

	/**
	 * متد شروع پلاگین.
	 * اینجا پلاگین می‌تواند سرویس‌ها، listenerها، threadها یا endpointها را راه‌اندازی کند.
	 *
	 * @throws Exception اگر خطایی در شروع رخ دهد
	 */
	void start() throws Exception;

	/**
	 * متد توقف پلاگین.
	 * باید تمام منابع (threadها، connectionها و غیره) را آزاد کند.
	 *
	 * @throws Exception اگر خطایی در توقف رخ دهد
	 */
	void stop() throws Exception;

	/**
	 * متد تخریب پلاگین (قبل از unload).
	 * برای cleanup نهایی استفاده می‌شود.
	 *
	 * @throws Exception اگر خطایی رخ دهد
	 */
	void destroy() throws Exception;

	/**
	 * بازگشت descriptor پلاگین (اختیاری، معمولاً توسط manager ست می‌شود).
	 *
	 * @return descriptor پلاگین
	 */
	PluginDescriptor getDescriptor();
}
