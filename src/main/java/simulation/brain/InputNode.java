package simulation.brain;

import simulation.input.InputType;

public class InputNode implements SourceNode {
    public static InputNode DISTANCE_LEFT = new InputNode(InputType.DistanceLeft);
    public static InputNode DISTANCE_TOP = new InputNode(InputType.DistanceTop);
    public static InputNode DISTANCE_X = new InputNode(InputType.DistanceX);
    public static InputNode DISTANCE_Y = new InputNode(InputType.DistanceY);
    public static InputNode DISTANCE = new InputNode(InputType.Distance);
    public static InputNode AGE = new InputNode(InputType.Age);
    public static InputNode LAST_MOVEMENT_X = new InputNode(InputType.LastMovementX);
    public static InputNode LAST_MOVEMENT_Y = new InputNode(InputType.LastMovementY);
    public static InputNode POPULATION = new InputNode(InputType.Population);
    public static InputNode POPULATION_FORWARD = new InputNode(InputType.PopulationForward);
    public static InputNode POPULATION_PERPENDICULAR = new InputNode(InputType.PopulationPerpendicular);
    public static InputNode RANDOM = new InputNode(InputType.Random);

    public static InputNode[] INPUT_NODES = {DISTANCE, DISTANCE_X, DISTANCE_Y, DISTANCE_TOP, DISTANCE_LEFT,
            POPULATION, LAST_MOVEMENT_X, LAST_MOVEMENT_Y, AGE, POPULATION_FORWARD, POPULATION_PERPENDICULAR, RANDOM};

    private final InputType type;

    private InputNode(InputType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "InputNode[%s]".formatted(type);
    }

    @Override
    public String getShortString() {
        return switch (type) {
            case DistanceLeft -> "DL";
            case DistanceTop -> "DB";
            case DistanceX -> "DX";
            case DistanceY -> "DY";
            case Distance -> "D";
            case Age -> "AGE";
            case LastMovementX -> "LMX";
            case LastMovementY -> "LMY";
            case Population -> "POP";
            case PopulationForward -> "PFW";
            case PopulationPerpendicular -> "PP";
            case Random -> "RND";
        };
    }

    public InputType getType() {
        return type;
    }
}
