package simulation.input.exceptions;

public class SensorException extends RuntimeException {
    public SensorException() {
    }

    public SensorException(String message) {
        super(message);
    }

    public SensorException(String message, Throwable cause) {
        super(message, cause);
    }

    public SensorException(Throwable cause) {
        super(cause);
    }

    protected SensorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
