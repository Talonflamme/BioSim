package simulation;

import org.jetbrains.annotations.NotNull;
import simulation.brain.Brain;
import simulation.config.SimulationConfig;
import simulation.reports.Report;
import visualization.Board;
import visualization.live.LiveSimulationWindow;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static utility.Utility.Rand;

public class Simulation {
    public static Simulation activeSimulation;
    protected SimulationProgress progress;
    private final SimulationConfig config;
    private Grid grid;
    private final Board visualizer;
    private Agent[] agents;
    private final ArrayList<QueuedMove> queuedMoves = new ArrayList<>();
    private SimulationState state;
    private final Report report;

    public Simulation(SimulationConfig config, Report report) {
        if (activeSimulation != null) {
            throw new RuntimeException("More than 1 Simulations");
        }

        this.state = SimulationState.Inactive;
        this.config = config;
        this.visualizer = new Board(config);
        this.grid = new Grid(config.getBoardWidth(), config.getBoardHeight());
        activeSimulation = this;
        this.report = report;
    }

    public Simulation(File configFile, Report report) throws IOException {
        this(SimulationConfig.fromFile(configFile), report);
    }

    private Agent[] getAgentsFromBrains(Brain[] brains) {
        Agent[] agents = new Agent[brains.length];

        for (int i = 0; i < brains.length; i++) {
            int x, y;
            boolean positionAlreadyUsed;

            do {
                x = Rand.nextInt(config.getBoardWidth());
                y = Rand.nextInt(config.getBoardHeight());
                positionAlreadyUsed = false;

                for (int j = 0; j < i; j++) {
                    if (agents[j].getX() == x && agents[j].getY() == y) {
                        positionAlreadyUsed = true;
                        break;
                    }
                }
            } while (positionAlreadyUsed);

            agents[i] = new Agent(x, y, brains[i], i);
        }
        return agents;
    }

    /**
     * Generate the agents for the new generation.
     *
     * @return the generated agents as an array.
     */
    private Agent @NotNull [] generateAgents() {
        Brain[] brain = new Brain[config.getPopulationSize()];
        for (int i = 0; i < brain.length; i++) {
            brain[i] = Brain.generate();
        }
        return getAgentsFromBrains(brain);
    }

    /**
     * Generate the agents for the new generation.
     *
     * @param parents The agents of the previous generation that survived. They will pass on the genes.
     *                Can be null to generate complete random agents.
     * @return the generated agents as an array.
     */
    private Agent @NotNull [] generateAgents(Agent[] parents) {
        if (parents == null) {
            return generateAgents();
        }

        int agentNum = config.getPopulationSize();
        Brain[] brains = new Brain[agentNum];

        ArrayList<Agent> aliveParents = new ArrayList<>();
        for (Agent agent : parents) {
            if (agent.isAlive()) {
                aliveParents.add(agent);
            }
        }
        if (aliveParents.isEmpty()) throw new RuntimeException("No agents alive");

        int minChildren = Math.floorDiv(agentNum, aliveParents.size());
        int count = 0;
        for (int i = 0; i < minChildren; i++) {
            for (Agent parent : parents) {
                if (!parent.isAlive()) continue;

                brains[count++] = parent.getBrain().generateChild();
            }
        }
        Rand.shuffle(aliveParents);
        for (int i = agentNum - count; i > 0; i--) {
            brains[count++] = aliveParents.get(i).getBrain().generateChild(); // todo
        }

        return getAgentsFromBrains(brains);
    }

    /**
     * Kill all agents where {@link #config config}.{@link SimulationConfig#survives} returns {@code false}.
     *
     * @return the amount of agents that survived.
     */
    private int killAgents() {
        assert state == SimulationState.Selection;
        int count = 0;
        for (Agent agent : agents) {
            if (!config.survives(agent)) {
                agent.die();
            } else {
                count++;
            }
        }
        return count;
    }

    private void clearAnimations() {
        // clear old animations
        File dir = new File("rsc/gens");
        if (!dir.exists()) {
            assert dir.mkdir();
            return;
        }
        File[] files = dir.listFiles();
        assert files != null;
        for (File f : files) {
            assert f.delete();
        }
    }

    /**
     * Start the simulation.
     */
    public void start() {
        clearAnimations();
        assert getState() == SimulationState.Inactive;
        int generations = config.getNumGenerations();

        state = SimulationState.Init;
        progress = new SimulationProgress();
        grid.clear();

        this.agents = generateAgents();
        grid.setAgents(agents);

        int steps = config.getStepsPerGen();

        visualizer.startRecording();

        for (int gen = 0; gen < generations; gen++) {
            for (int i = 0; i < steps; i++) {
                state = SimulationState.BeforeStep;
                progress.setStep(i);
                step();
            }

            state = SimulationState.Selection;
            visualizer.addSelection(config, 5);
            int aliveCount = killAgents();
            getReport().newGeneration(gen, aliveCount, agents);

            visualizer.addAlive(agents, 2.5f);

            if (gen % config.getAnimationFrequency() == 0 || gen == generations - 1) {
                visualizer.saveRecordingThreaded("rsc/gens/gen" + gen + ".svg");
            } else {
                visualizer.clear();
            }

            if (gen == generations - 1) continue;

            state = SimulationState.NewGeneration;
            progress.setGeneration(gen + 1);
            this.agents = generateAgents(this.agents);

            grid.clear();
            grid.setAgents(agents);
        }

        visualizer.saveRecording("snapshots/generation.svg");

        state = SimulationState.Inactive;
    }

    /**
     * Called by {@link LiveSimulationWindow live window}
     */
    public void liveStep() {
        switch (state) {
            case Inactive -> {
                state = SimulationState.Init;
                progress = new SimulationProgress();
                grid.clear();
                agents = generateAgents();
                grid.setAgents(agents);
                state = SimulationState.BeforeStep;
            }
            case BeforeStep -> {
                step();
                state = SimulationState.AfterStep;
            }
            case AfterStep -> {
                int step = progress.getStep();
                int gen = progress.getGeneration();
                if (step + 1 >= config.getStepsPerGen()) {
                    if (gen + 1 >= config.getNumGenerations()) {
                        state = SimulationState.Inactive;
                    } else {
                        state = SimulationState.Selection;
                    }
                } else {
                    state = SimulationState.BeforeStep;
                    progress.setStep(step + 1);
                }
            }
            case Selection -> {
                int gen = progress.getGeneration();
                int aliveCount = killAgents();
                getReport().newGeneration(gen, aliveCount, agents);
                state = SimulationState.AfterSelection;
            }
            case AfterSelection -> state = SimulationState.NewGeneration;
            case NewGeneration -> {
                int gen = progress.getGeneration();
                progress.setGeneration(gen + 1);
                progress.setStep(0);
                agents = generateAgents(agents);
                grid.clear();
                grid.setAgents(agents);
                state = SimulationState.BeforeStep;
            }
        }
    }

    /**
     * Creates an image of the current state at path.
     *
     * @param path The path where to put the resulting image
     */
    public void snapshot(File path) throws IOException {
        BufferedImage img = visualizer.createImage(agents);
        ImageIO.write(img, "PNG", path);
        System.out.printf("Snapshot: %s%n", path.getPath());
    }

    /**
     * Creates an image of the current state at path.
     *
     * @param path The path as a string. Will be wrapping using {@link File#File(String) new File(path)}
     */
    @SuppressWarnings("unused")
    public void snapshot(String path) throws IOException {
        snapshot(new File(path));
    }

    /**
     * Creates an image of the current state and saves it uniquely 'snapshots/img.png'. Does not throw an exception.
     */
    @SuppressWarnings("unused")
    public void snapshot() {
        String filename = "snapshots/img_%d.png".formatted(System.currentTimeMillis());
        try {
            snapshot(new File(filename));
        } catch (IOException e) {
            System.err.printf("Error while saving image to path: %s: %n%s%n", filename, e);
        }
    }

    public SimulationConfig getConfig() {
        return config;
    }

    public static SimulationConfig config() {
        return activeSimulation.config;
    }

    public SimulationProgress getProgress() {
        return progress;
    }

    public Agent[] getAgents() {
        return agents;
    }

    public Grid getGrid() {
        return grid;
    }

    protected void setGrid(Grid grid) {
        this.grid = grid;
    }

    public Report getReport() {
        return report;
    }

    /**
     * Update all moves if possible. Does this by randomly selecting a move and checking if it is
     * {@link QueuedMove#isExecutable executable}. If it is, the move is {@link QueuedMove#execute executed}.
     * Does this as long as possible. Finally, clears {@link #queuedMoves}. Calling this method does <b>NOT</b>
     * mean every move is executed. If there is a conflict, the agent which succeeds is chosen at random.
     */
    void updateMoves() {
        assert getState() == SimulationState.MoveUpdate;

        while (true) {
            boolean anyExecuted = false;

            for (int i = 0; i < queuedMoves.size(); i++) {
                // random index from i -> N where N is the size - 1
                // this ensures, that previously selected elements do not get chosen again.
                int index = i + Rand.nextInt(queuedMoves.size() - i);
                // swap i with random index
                Collections.swap(queuedMoves, i, index);
                QueuedMove move = queuedMoves.get(i);
                if (!move.isExecutable(grid)) continue;

                move.execute(grid, progress);
                queuedMoves.remove(i);
                anyExecuted = true;
                break;
            }

            if (!anyExecuted) {
                break;
            }
        }

        queuedMoves.clear();
    }

    private void step() {
        assert state == SimulationState.BeforeStep;
        state = SimulationState.OnAgentThink;
        for (Agent agent : agents) {
            agent.step();
        }

        state = SimulationState.MoveUpdate;
        updateMoves();
        for (Agent agent : agents) {
            agent.afterStep(agent.getX(), agent.getY(), progress);
        }

        if (visualizer.isRecording()) {
            visualizer.addMovement(agents);
        }
    }

    /**
     * Queue up a move that will be executed when all agents have {@link Agent#step() stepped}.
     *
     * @param agent the Agent which is to move.
     * @param newX  the new x coordinate for the agent.
     * @param newY  the new y coordinate for the agent.
     */
    public void queueUpMove(Agent agent, int newX, int newY) {
        assert getState() == SimulationState.OnAgentThink;
        if (grid.isBounds(newX, newY) && (agent.getX() != newX || agent.getY() != newY)) {
            synchronized (queuedMoves) {
                queuedMoves.add(new QueuedMove(agent, newX, newY));
            }
        }
    }

    private record QueuedMove(Agent agent, int x, int y) {
        /**
         * Returns whether this move is executable. Checks if it is {@link Grid#isBounds in bounds} and
         * if the position is {@link Grid#get available}.
         */
        boolean isExecutable(Grid grid) {
            return grid.isBounds(x, y) && !grid.get(x, y);
        }

        /**
         * Executes the move by setting the old position on {@code grid} to false and the new position to true.
         * Does <b>NOT</b> check if the move {@link #isExecutable is executable}.
         */
        void execute(Grid grid, SimulationProgress progress) {
            grid.set(agent.getX(), agent.getY(), false);
            grid.set(x, y, true);
            agent.afterStep(x, y, progress);
        }
    }

    public enum SimulationState {
        /**
         * The simulation is inactive.
         */
        Inactive,
        /**
         * Assigned right after {@link #start}
         */
        Init,
        /**
         * State of {@link #step} where {@link Agent#step()} is called
         */
        OnAgentThink,
        /**
         * State of {@link #step} where all {@link #queuedMoves queued up moves} are executed
         */
        MoveUpdate,
        /**
         * State right before calling {@link #step Simulation.step()}.
         */
        BeforeStep,
        /**
         * State when agents are killed and selected. Mainly {@link #killAgents() Simulation.killAgents()} is called.
         */
        Selection,
        AfterStep,
        AfterSelection,
        /**
         * State when a new generation is created
         */
        NewGeneration
    }

    public SimulationState getState() {
        return state;
    }
}
