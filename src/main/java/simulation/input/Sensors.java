package simulation.input;

import simulation.*;

import static simulation.Simulation.activeSimulation;
import static simulation.Simulation.config;

import static utility.Utility.*;
import simulation.input.exceptions.*;

public class Sensors {
    /**
     * The radius used for Population scanning. Must be greater than 0.
     *
     * @see #getPopulation
     * @see #getPopulationForward
     * @see #getPopulationPerpendicular
     * @see InputType#Population
     * @see InputType#PopulationForward
     * @see InputType#PopulationPerpendicular
     */
    public static int POPULATION_SCAN_RADIUS = 6;

    private Sensors() {
    }

    /**
     * Value function for {@link InputType#Random}
     *
     * @return a random value between 0 and 1
     */
    public static float getRandom() {
        return Rand.nextFloat();
    }

    /**
     * Value function for {@link InputType#Age}
     *
     * @return a value representing the age. Starts at 0 and approaches 1 as the generation grows older.
     */
    public static float getAge() {
        int step = activeSimulation.getProgress().getStep();
        int max = config().getStepsPerGen();
        if (step < 0 || step > max) {
            throw new StepOutOfRangeException(step, max);
        }
        return (float) step / max;
    }

    /**
     * Value function for {@link InputType#DistanceLeft}
     */
    public static float getDistanceLeft(Agent agent) {
        int x = agent.getX();
        int w = config().getBoardWidth();
        if (x < 0 || x >= w) {
            throw new AgentPositionException("x=%d, width=%d".formatted(x, w));
        }
        return x / (w - 1f);
    }

    /**
     * Value function for {@link InputType#DistanceTop}
     */
    public static float getDistanceTop(Agent agent) {
        int y = agent.getY();
        int h = config().getBoardHeight();
        if (y < 0 || y >= h) {
            throw new AgentPositionException("y=%d, height=%d".formatted(y, h));
        }
        return y / (h - 1f);
    }

    /**
     * Value function for {@link InputType#DistanceX}
     *
     * @return the distance to the nearest edge in horizontal direction. Scaled up, so the highest value (1) is in the
     * middle of the board while either left or right are value 0.
     */
    public static float getNearestDistanceX(Agent agent) {
        int x = agent.getX();
        int width = config().getBoardWidth();

        if (x < 0 || x >= width) {
            throw new AgentPositionException("x=%d, width=%d".formatted(x, width));
        }

        return Math.min(x, width - x - 1) / (width - 1f) * 2;
    }

    /**
     * Value function for {@link InputType#DistanceY}
     *
     * @return the distance to the nearest edge in vertical direction. Scaled up, so the highest value (1) is in the
     * middle of the board while either top or bottom are value 0.
     */
    public static float getNearestDistanceY(Agent agent) {
        int y = agent.getY();
        int height = config().getBoardHeight();

        if (y < 0 || y >= height) {
            throw new AgentPositionException("y=%d, height=%d".formatted(y, height));
        }

        return Math.min(y, height - y - 1) / (height - 1f) * 2;
    }


    /**
     * Value function for {@link InputType#Distance}
     *
     * @return the distance to the nearest edge in any direction. Scaled up, so the highest value (1) is in the
     * middle of the board while either top, right, bottom and left are value 0.
     */
    public static float getNearestDistance(Agent agent) {
        int width = config().getBoardWidth();
        int height = config().getBoardHeight();
        int x = agent.getX();
        int y = agent.getY();

        if (x < 0 || x >= width) {
            throw new AgentPositionException("x=%d, width=%d".formatted(x, width));
        }

        if (y < 0 || y >= height) {
            throw new AgentPositionException("y=%d, height=%d".formatted(y, height));
        }

        float left = x / (width - 1f);
        float right = (width - x - 1f) / (width - 1f);
        float top = y / (height - 1f);
        float bottom = (height - y - 1f) / (height - 1f);

        // *2 since the middle should be 1 and not 0.5
        return Math.min(Math.min(Math.min(left, right), top), bottom) * 2;
    }

    /**
     * Value function for {@link InputType#Population}
     */
    public static float getPopulation(Agent agent, Grid grid) {
        int value = 0;
        int maxValue = 0;

        for (int i = -POPULATION_SCAN_RADIUS; i <= POPULATION_SCAN_RADIUS; i++) {
            for (int j = -POPULATION_SCAN_RADIUS; j <= POPULATION_SCAN_RADIUS; j++) {
                if (i == 0 && j == 0) continue;

                int x = agent.getX() + i;
                int y = agent.getY() + j;

                if (!grid.isBounds(x, y)) continue;

                maxValue++;
                if (grid.get(x, y)) {
                    value++;
                }
            }
        }

        return (float) value / maxValue;
    }

    /**
     * Value function for {@link InputType#PopulationForward}
     */
    public static float getPopulationForward(Agent agent, Grid grid) {
        return getPopulationAlongAxis(agent, grid, agent.getForward());
    }

    /**
     * Value function for {@link InputType#PopulationPerpendicular}
     */
    public static float getPopulationPerpendicular(Agent agent, Grid grid) {
        return getPopulationAlongAxis(agent, grid, agent.getForward().getPerpendicular());
    }

    public static float getPopulationAlongAxis(Agent agent, Grid grid, Direction forward) {
        if (forward == Direction.CENTER) return 0f;

        int value = 0;
        int maxValue = 0;

        for (int i = -POPULATION_SCAN_RADIUS; i <= POPULATION_SCAN_RADIUS; i++) {
            if (i == 0) continue;

            int x = agent.getX() + forward.getX() * i;
            int y = agent.getY() + forward.getY() * i;

            if (!grid.isBounds(x, y)) continue;

            int dst = POPULATION_SCAN_RADIUS - Math.abs(i) + 1;
            int nearness = dst * dst;
            maxValue += nearness;
            if (!grid.get(x, y)) continue;

            // calculate how much position is worth
            // nearness: how close position is the agent. Higher = better. Value between 1 and POPULATION_SCAN_RADIUS / 2
            value += nearness;
        }

        return maxValue == 0 ? 0f : (float) value / maxValue;
    }

    /**
     * Value function for {@link InputType#LastMovementX}
     *
     * @return the {@link Direction#getX x component} of the {@link Agent#getForward direction} mapped like this: [-1, 0, 1] -> [0, 0.5, 1]
     */
    public static float getLastMovementX(Agent agent) {
        if (agent.getForward() == null) {
            throw new UnexpectedLastMovementException(agent);
        }

        return switch (agent.getForward().getX()) {
            case -1 -> 0f;
            case 0 -> 0.5f;
            case 1 -> 1f;
            default ->
                    throw new UnexpectedLastMovementException(agent);
        };
    }

    /**
     * Value function for {@link InputType#LastMovementY}
     *
     * @return the {@link Direction#getY y component} of the {@link Agent#getForward direction} mapped like this: [-1, 0, 1] -> [0, 0.5, 1]
     */
    public static float getLastMovementY(Agent agent) {
        if (agent.getForward() == null) {
            throw new UnexpectedLastMovementException(agent);
        }

        return switch (agent.getForward().getY()) {
            case -1 -> 0f;
            case 0 -> 0.5f;
            case 1 -> 1f;
            default ->
                    throw new UnexpectedLastMovementException(agent);
        };
    }

    /**
     * Returns a value between 0 and 1 which corresponds to the given type.
     *
     * @param agent The agent on which to call certain methods (e.g. {@link #getDistanceLeft}).
     *              Not needed for some methods.
     * @param type  The type of the {@link simulation.brain.InputNode InputNode}
     */
    public static float getValue(Agent agent, InputType type) {
        return switch (type) {
            case DistanceLeft -> getDistanceLeft(agent);
            case DistanceTop -> getDistanceTop(agent);
            case DistanceX -> getNearestDistanceX(agent);
            case DistanceY -> getNearestDistanceY(agent);
            case Distance -> getNearestDistance(agent);
            case Age -> getAge();
            case LastMovementX -> getLastMovementX(agent);
            case LastMovementY -> getLastMovementY(agent);
            case Population -> getPopulation(agent, activeSimulation.getGrid());
            case PopulationForward -> getPopulationForward(agent, activeSimulation.getGrid());
            case PopulationPerpendicular -> getPopulationPerpendicular(agent, activeSimulation.getGrid());
            case Random -> getRandom();
        };
    }
}
