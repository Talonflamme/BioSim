package simulation;

import simulation.reports.Report;

public class FakeSimulation extends Simulation {
    private SimulationState state;

    public FakeSimulation(FakeSimulationConfig config) {
        super(config, new Report(config, new EmptyStream()));
    }

    public void setProgress(SimulationProgress progress) {
        super.progress = progress;
    }

    protected void setState(SimulationState state) {
        this.state = state;
    }

    @Override
    public SimulationState getState() {
        return state;
    }

    @Override
    public void setGrid(Grid grid) {
        super.setGrid(grid);
    }

    @Override
    public FakeGrid getGrid() {
        Grid grid = super.getGrid();
        if (grid instanceof FakeGrid fg) {
            return fg;
        }
        return new FakeGrid(grid);
    }
}
