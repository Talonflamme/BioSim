package simulation.input.exceptions;

public class StepOutOfRangeException extends AgentPositionException {
    public StepOutOfRangeException() {
    }

    public StepOutOfRangeException(String message) {
        super(message);
    }

    public StepOutOfRangeException(int step, int stepsPerGeneration) {
        this("step=%d, stepsPerGen=%d".formatted(step, stepsPerGeneration));
    }
}
