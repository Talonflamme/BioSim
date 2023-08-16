package simulation.brain;

import simulation.action.ActionType;

public class OutputNode implements TargetNode {
    public static OutputNode MOVE_X = new OutputNode(ActionType.MoveX);
    public static OutputNode MOVE_Y = new OutputNode(ActionType.MoveY);
    public static OutputNode MOVE_RAND = new OutputNode(ActionType.MoveRand);
    public static OutputNode DO_NOTHING = new OutputNode(ActionType.DoNothing);
    public static OutputNode MOVE_FORWARD = new OutputNode(ActionType.MoveForward);
    public static OutputNode MOVE_REVERSE = new OutputNode(ActionType.MoveReverse);
    public static OutputNode KILL_FORWARD = new OutputNode(ActionType.KillForward);

    public static final OutputNode[] OUTPUT_NODES = {MOVE_X, MOVE_Y, MOVE_RAND, DO_NOTHING, MOVE_FORWARD,
            MOVE_REVERSE, KILL_FORWARD};

    private final ActionType type;

    private OutputNode(ActionType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "OutputNode[%s]".formatted(type);
    }

    @Override
    public String getShortString() {
        return switch (type) {
            case MoveX -> "Mx";
            case MoveY -> "My";
            case MoveRand -> "MRND";
            case DoNothing -> "DN";
            case MoveForward -> "MFw";
            case MoveReverse -> "MRv";
            case KillForward -> "KFw";
        };
    }

    public ActionType getType() {
        return type;
    }
}
