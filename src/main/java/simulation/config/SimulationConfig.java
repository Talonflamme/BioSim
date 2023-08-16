package simulation.config;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import simulation.Agent;

public class SimulationConfig {
    /**
     * The steps to take after which all agents die.
     */
    private final int stepsPerGen;
    /**
     * The size of each generation.
     */
    private final int populationSize;
    /**
     * The width of the game board.
     */
    private final int boardWidth;
    /**
     * The height of the game board.
     */
    private final int boardHeight;
    /**
     * The amount of genes every agent has.
     */
    private final int genes;
    /**
     * The maximum number of inner neurons the neural network of each agent can have.
     */
    private final int maxInnerNeurons;
    private final Selection[] selection;

    public int getStepsPerGen() {
        return stepsPerGen;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public int getBoardWidth() {
        return boardWidth;
    }

    public int getBoardHeight() {
        return boardHeight;
    }

    public int getGenes() {
        return genes;
    }

    public int getMaxInnerNeurons() {
        return maxInnerNeurons;
    }

    public float getMutationRate() {
        return mutationRate;
    }

    /**
     * The rate of mutations. E.g: 0.001 = 0.1%, every 1 thousand bits copied, there is an error.
     */
    private final float mutationRate;

    @JsonCreator
    public SimulationConfig(@JsonProperty(value = "stepsPerGen", required = true) int stepsPerGen,
                            @JsonProperty(value = "populationSize", required = true) int populationSize,
                            @JsonProperty(value = "boardWidth", required = true) int boardWidth,
                            @JsonProperty(value = "boardHeight", required = true) int boardHeight,
                            @JsonProperty(value = "genes", required = true) int genes,
                            @JsonProperty(value = "maxInnerNeurons", required = true) int maxInnerNeurons,
                            @JsonProperty(value = "mutationRate", required = true) float mutationRate,
                            @JsonProperty(value = "selection", required = true) ObjectNode[] selection) throws ConfigError {
        this.stepsPerGen = stepsPerGen;
        this.populationSize = populationSize;
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.genes = genes;
        this.maxInnerNeurons = maxInnerNeurons;
        this.mutationRate = mutationRate;
        this.selection = Selection.parse(selection, this);

        if (this.stepsPerGen < 1) {
            throw new ConfigError("stepsPerGen should be greater than 1");
        }
        if (this.boardWidth < 1) {
            throw new ConfigError("boardWidth should be greater than 1");
        }
        if (this.boardHeight < 1) {
            throw new ConfigError("boardHeight should be greater than 1");
        }
        if (this.populationSize < 1 || this.populationSize > this.boardWidth * this.boardHeight) {
            throw new ConfigError("populationSize should be between 1 and boardWidth * boardHeight");
        }
        if (this.genes < 1) {
            throw new ConfigError("genes should be greater than 1");
        }
        if (this.maxInnerNeurons < 1 || this.maxInnerNeurons > 128) {
            throw new ConfigError("maxInnerNeurons should be between 1 and 128 (2^7)");
        }
        if (this.mutationRate < 0 || this.mutationRate > 1) {
            throw new ConfigError("mutationRate should be between 0 and 1");
        }
    }

    @Override
    public String toString() {
        return "SimulationConfig[" +
                "stepsPerGen=" + stepsPerGen +
                ", populationSize=" + populationSize +
                ", boardWidth=" + boardWidth +
                ", boardHeight=" + boardHeight +
                ", genes=" + genes +
                ", maxInnerNeurons=" + maxInnerNeurons +
                ", mutationRate=" + mutationRate +
                ']';
    }

    /**
     * Returns whether agent should survive. Defaults to true and checks if any {@link #selection}
     * kills the agent.
     */
    public boolean survives(Agent agent) {
        for (Selection s : selection) {
            if (!s.survives(agent)) {
                return false;
            }
        }
        return true;
    }
}
