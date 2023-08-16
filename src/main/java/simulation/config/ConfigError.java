package simulation.config;

public class ConfigError extends Exception {
    public ConfigError() {
    }

    public ConfigError(String message) {
        super(message);
    }

    public ConfigError(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigError(Throwable cause) {
        super(cause);
    }
}
