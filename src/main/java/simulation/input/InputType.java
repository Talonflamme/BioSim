package simulation.input;

public enum InputType {
    /**
     * The distance to the left edge.
     */
    DistanceLeft,
    /**
     * The distance to the bottom edge.
     */
    DistanceTop,
    /**
     * The distance to the nearest edge in x direction.
     */
    DistanceX,
    /**
     * The distance to the nearest edge in y direction.
     */
    DistanceY,
    /**
     * The distance to the nearest edge in any direction.
     */
    Distance,
    /**
     * The age of the agent.
     */
    Age,
    /**
     * The movement of the last step in x direction.
     */
    LastMovementX,
    /**
     * The movement of the last step in y direction.
     */
    LastMovementY,
    /**
     * Population Density. Calculated by taking the distance of each agent in a {@value Sensors#POPULATION_SCAN_RADIUS}-unit radius.
     */
    Population,
    /**
     * Population Density along the forward axis. {@value Sensors#POPULATION_SCAN_RADIUS}-units in each direction.
     */
    PopulationForward,
    /**
     * Population Density along the perpendicular axis. {@value Sensors#POPULATION_SCAN_RADIUS}-units in each direction.
     */
    PopulationPerpendicular,
    /**
     * Generates a random input
     */
    Random
}