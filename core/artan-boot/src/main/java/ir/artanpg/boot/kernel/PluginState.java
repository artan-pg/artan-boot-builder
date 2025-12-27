package ir.artanpg.boot.kernel;

public enum PluginState {

	DISCOVERED(0),
	VERIFYED(1),
	INSTALLIG(2),
	ACTIVE(3),
	UNINSTALLING(4),
	DIACTIVE(5),
	FAILED(6),
	;

	private final int code;

	PluginState(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static PluginState valueOf(int code) {
		for (PluginState state : values()) {
			if (state.code == code) return state;
		}
		return null;
	}
}
