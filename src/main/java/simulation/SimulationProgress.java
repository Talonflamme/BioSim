package simulation;

/**
 * Represents the progress of an ongoing simulation.
 */
public class SimulationProgress {
    private int generation;
    private int step;

    public SimulationProgress() { }

    public int getGeneration() {
        return generation;
    }

    public int getStep() {
        return step;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    public void setStep(int step) {
        this.step = step;
    }
}
