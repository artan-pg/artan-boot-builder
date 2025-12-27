package ir.artanpg.boot.kernel.context;

import ir.artanpg.boot.kernel.PluginException;
import ir.artanpg.boot.kernel.PluginState;

public interface PluginLifecycle {

	/**
	 * مرحله 2: اعتبارسنجی پلاگین
	 * بررسی وابستگی‌ها، امضاها و مجوزها
	 */
	void validate() throws PluginException;

	/**
	 * مرحله 3: مقداردهی اولیه پلاگین
	 * ایجاد نمونه‌ها و آماده‌سازی اولیه
	 */
	void initialize() throws PluginException;

	/**
	 * مرحله 4: شروع پلاگین
	 * پلاگین شروع به کار می‌کند
	 */
	void start() throws PluginException;

	/**
	 * مرحله 5: توقف پلاگین
	 * پلاگین متوقف می‌شود
	 */
	void stop() throws PluginException;

	/**
	 * مرحله 6: تخلیه پلاگین
	 * آزادسازی منابع و تخلیه از حافظه
	 */
	void unload() throws PluginException;

	/**
	 * بازگرداندن وضعیت فعلی پلاگین
	 */
	PluginState getState();

	/**
	 * بررسی آیا پلاگین فعال است
	 */
	default boolean isActive() {
		return getState() == PluginState.ACTIVE;
	}
}
