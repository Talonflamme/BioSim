package simulation;

import org.jetbrains.annotations.NotNull;
import simulation.brain.Brain;
import simulation.config.SimulationConfig;
import visualization.Board;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static utility.Utility.ObjectMapper;
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

    public Simulation(SimulationConfig config) {
        if (activeSimulation != null) {
            throw new RuntimeException("More than 1 Simulations");
        }

        this.state = SimulationState.Inactive;
        this.config = config;
        this.visualizer = new Board(config);
        this.grid = new Grid(config.getBoardWidth(), config.getBoardHeight());
        activeSimulation = this;
    }

    public Simulation(File configFile) throws IOException {
        this(ObjectMapper.readValue(configFile, SimulationConfig.class));
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

        int aliveCount = 0;
        for (Agent agent : parents) {
            if (agent.isAlive()) {
                aliveCount++;
            }
        }
        if (aliveCount == 0) throw new RuntimeException("No agents alive");

        int minChildren = Math.floorDiv(agentNum, aliveCount);
        for (int i = 0; i < minChildren; i++) {
            for (int j = 0; j < parents.length; j++) {
                if (!parents[j].isAlive()) continue;

                brains[i * minChildren + j] = parents[j].getBrain().generateChild();
            }
        }
        for (int i = 0; i < agentNum - aliveCount; i++) {
            while (true) {
                Agent agent = Rand.choice(parents);
                if (!agent.isAlive()) continue;

                brains[aliveCount + i] = agent.getBrain().generateChild();
                break;
            }
        }

        return getAgentsFromBrains(brains);
    }

    /**
     * Kill all agents where {@link #config config}.{@link SimulationConfig#survives} returns {@code false}.
     */
    private void killAgents() {
        for (Agent agent : agents) {
            if (!config.survives(agent)) {
                agent.die();
            }
        }
    }

    /**
     * Start the simulation.
     *
     * @param generations The amount of generations to generate.
     */
    public void start(int generations) throws IOException {
        assert getState() == SimulationState.Inactive;
        state = SimulationState.Init;
        progress = new SimulationProgress();
        grid.clear();

        this.agents = generateAgents();
        grid.setAgents(agents);

        int steps = config.getStepsPerGen();

        snapshot("snapshots/img.png");
        visualizer.startRecording();

        for (int gen = 0; gen < generations; gen++) {
            for (int i = 0; i < steps; i++) {
                progress.setStep(i);
                step();
            }
            if (gen % 10 == 9) {
                System.out.printf("Generation done: %d/%d%n", gen + 1, generations);
            }

            visualizer.addSelection(config, 2);
            killAgents();
            visualizer.addFrame(agents);
            visualizer.waitSeconds(2);

            if (true) break;
            if (gen == generations - 1) continue;

            state = SimulationState.NewGeneration;
            progress.setGeneration(gen + 1);
            this.agents = generateAgents(this.agents);

            grid.clear();
            grid.setAgents(agents);
        }

        state = SimulationState.Report;
        visualizer.saveRecording("snapshots/anim.svg");

        state = SimulationState.Inactive;
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
    public void snapshot(String path) throws IOException {
        snapshot(new File(path));
    }

    /**
     * Creates an image of the current state and saves it uniquely 'snapshots/img.png'. Does not throw an exception.
     */
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

    public Grid getGrid() {
        return grid;
    }

    protected void setGrid(Grid grid) {
        this.grid = grid;
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

        state = SimulationState.OnAgentThink;
        for (Agent agent : agents) {
            agent.step();
        }

        state = SimulationState.MoveUpdate;
        updateMoves();
        for (Agent agent : agents) {
            agent.afterStep(agent.getX(), agent.getY(), progress);
        }
        // create image
        visualizer.addFrame(agents);
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

    protected enum SimulationState {
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
         * State when a new generation is created
         */
        NewGeneration,
        /**
         * State when {@link #start} finishes and a report is created
         */
        Report
    }

    protected SimulationState getState() {
        return state;
    }
}
