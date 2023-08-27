package simulation;

import com.fasterxml.jackson.databind.node.ObjectNode;
import simulation.config.ConfigError;
import simulation.config.SimulationConfig;

public class FakeSimulationConfig extends SimulationConfig {
    private Integer stepsPerGen;
    private Integer populationSize;
    private Integer boardWidth;
    private Integer boardHeight;
    private Integer genes;
    private Integer maxInnerNeurons;
    private Float mutationRate;

    public FakeSimulationConfig() throws ConfigError {
        super(1, Type.Compute, 1, 1, 1, 1, 1, 1, 1, 0, new ObjectNode[0], false, false, 1, 1);
    }

    @Override
    public int getStepsPerGen() {
        if (stepsPerGen == null) {
            throw new IllegalStateException("stepsPerGen is null");
        }
        return stepsPerGen;
    }

    public void setStepsPerGen(int stepsPerGen) {
        this.stepsPerGen = stepsPerGen;
    }

    @Override
    public int getPopulationSize() {
        if (populationSize == null) {
            throw new IllegalStateException("populationSize is null");
        }
        return populationSize;
    }

    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }

    @Override
    public int getBoardWidth() {
        if (boardWidth == null) {
            throw new IllegalStateException("boardWidth is null");
        }
        return boardWidth;
    }

    public void setBoardWidth(int boardWidth) {
        this.boardWidth = boardWidth;
    }

    @Override
    public int getBoardHeight() {
        if (boardHeight == null) {
            throw new IllegalStateException("boardHeight is null");
        }
        return boardHeight;
    }

    public void setBoardHeight(int boardHeight) {
        this.boardHeight = boardHeight;
    }

    @Override
    public int getGenes() {
        if (genes == null) {
            throw new IllegalStateException("genes is null");
        }
        return genes;
    }

    public void setGenes(int genes) {
        this.genes = genes;
    }

    @Override
    public int getMaxInnerNeurons() {
        if (maxInnerNeurons == null) {
            throw new IllegalStateException("maxInnerNeurons is null");
        }
        return maxInnerNeurons;
    }

    public void setMaxInnerNeurons(int maxInnerNeurons) {
        this.maxInnerNeurons = maxInnerNeurons;
    }

    @Override
    public float getMutationRate() {
        if (mutationRate == null) {
            throw new IllegalStateException("mutationRate is null");
        }
        return mutationRate;
    }

    public void setMutationRate(float mutationRate) {
        this.mutationRate = mutationRate;
    }
}
