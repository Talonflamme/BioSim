package simulation.action;

/**
 * Stores the result of a {@link simulation.brain.Brain#think Brain.think} operation.
 * @param type The type of action.
 * @param intensity The intensity of the action.
 */
public record Action(ActionType type, float intensity) {
}
