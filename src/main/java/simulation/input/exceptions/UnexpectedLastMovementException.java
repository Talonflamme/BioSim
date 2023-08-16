package simulation.input.exceptions;

import simulation.Agent;
import simulation.Direction;

public class UnexpectedLastMovementException extends SensorException {
    public UnexpectedLastMovementException() {
    }

    public UnexpectedLastMovementException(String message) {
        super(message);
    }

    public UnexpectedLastMovementException(Direction lastDirection) {
        this("Unexpected last movement of agent: %s".formatted(lastDirection));
    }

    public UnexpectedLastMovementException(Agent agent) {
        this(agent.getForward());
    }
}
