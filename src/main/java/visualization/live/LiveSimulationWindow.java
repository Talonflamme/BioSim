package visualization.live;

import simulation.Agent;
import simulation.Simulation;

import javax.swing.*;

public class LiveSimulationWindow {
    private final JFrame frame;
    private final Simulation simulation;
    private final Draw draw;
    private Thread simulationThread;
    Agent inspectedAgent;

    public LiveSimulationWindow(Simulation simulation) {
        this.simulation = simulation;
        frame = new JFrame("BioSim");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);

        draw = new Draw(simulation, frame, this);

        frame.pack();
        frame.setLocationRelativeTo(null);

        frame.addKeyListener(draw.information.userInput.getListener());
    }

    public void show() {
        frame.setVisible(true);
    }

    private static final double ANIMATION_REFRESH_RATE = 1000.0 / 60.0;

    /**
     * Creates and starts a new thread that calls the {@link #start()} method. First, checks whether there is already
     * a thread performing this task. If there is, the method simply returns, neither stopping the already running
     * thread nor restarting or reassigning it. The name of the thread is {@code "simulation-thread"}.
     */
    public void startInThread() {
        boolean isRunning = simulationThread != null && simulationThread.isAlive();
        if (isRunning) return;
        simulationThread = new Thread(this::start, "simulation-thread");
        simulationThread.start();
    }

    public void start() {
        do {
            long startMs = System.currentTimeMillis();
            simulation.liveStep();
            long endMs = System.currentTimeMillis();

            switch (draw.information.userInput.state) {
                case FastForward -> {
                    boolean isStep = simulation.getState() == Simulation.SimulationState.AfterStep;
                    startAnimationFixedTime(isStep ? 0.1 : 249);
                }
                case Skip -> {
                    boolean isNewGen = simulation.getState() == Simulation.SimulationState.AfterSelection;
                    if (isNewGen) {
                        // 20% -> agents opacity = 1; selection opacity = 0.4
                        draw.selection.animate(0.2f);
                        onNewGeneration();
                    }
                }
                default -> startAnimation(endMs - startMs);
            }
        } while (simulation.getState() != Simulation.SimulationState.Inactive &&
                 draw.information.userInput.state != UserInputPanel.UserInputState.Pause);
    }

    private void onNewGeneration() {
        simulation.liveStep(); // create new generation

        float surviveRate = 1f;
        if (simulation.getReport().getLastGeneration() != null) {
            surviveRate = simulation.getReport().getLastGeneration().surviveRate();
        }

        inspectedAgent = null;
        draw.information.onNewGeneration(simulation.getProgress().getGeneration(), simulation.getAgents(), surviveRate);
    }

    private void startAnimationFixedTime(double time) {
        switch (simulation.getState()) {
            case AfterStep -> animateStep(time, UserInputPanel.UserInputState.FastForward);
            case AfterSelection -> animateSelection(time, UserInputPanel.UserInputState.FastForward);
            case NewGeneration, AfterInit -> onNewGeneration();
        }
    }

    /**
     * starts an animation based on the {@link Simulation#getState state}.
     *
     * @param elapsedMs the time in milliseconds that the step took
     */
    private void startAnimation(long elapsedMs) {
        switch (simulation.getState()) {
            case AfterStep -> animateStep(simulation.getConfig().getMsPerStep() - elapsedMs, UserInputPanel.UserInputState.None);
            case AfterSelection -> animateSelection(simulation.getConfig().getMsPerSelection() - elapsedMs, UserInputPanel.UserInputState.None);
            case NewGeneration, AfterInit -> onNewGeneration();
        }
    }

    private static void sleep(double ms) {
        if (ms == 0) return;
        long ms_long = (long) ms;
        int ns = (int) ((ms % 1) * 1e6);
        try {
            Thread.sleep(ms_long, ns);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void inspect(Agent agent) {
        boolean isNew = agent != inspectedAgent;
        inspectedAgent = agent;
        if (!isNew) return;
        draw.information.onInspect();
    }

    @FunctionalInterface
    private interface AnimationConsumer {
        /**
         * Called when something is supposed to be animated.
         *
         * @param animationProgress a floating value between 0 and 1 (inclusive). This value represents the progress
         *                          of the animation. For example, at the start this value be equal to {@code 0}, at
         *                          the end it'll equal {@code 1} and after 41% of the time it'll equal {@code 0.41f}.
         * @see #animate
         */
        void accept(float animationProgress);
    }

    /**
     * Animates anything.
     *
     * @param durationMs    the duration of this animation in milliseconds.
     * @param animationFunc the animation function. This function takes in a float between 0 and 1, which
     *                      represents the progress of this animation.
     * @param expectedState the expected state for this animation. The animation is cancelled when the state changes.
     */
    private void animate(double durationMs, AnimationConsumer animationFunc, UserInputPanel.UserInputState expectedState) {
        if (durationMs == 0) return;
        double time = 0.0; // elapsed time since method start
        while (time < durationMs && draw.information.userInput.state == expectedState) {
            long start = System.currentTimeMillis();
            float progress = (float) Math.min(time / durationMs, 1);
            animationFunc.accept(progress);

            double timeLeft = Math.max(0, durationMs - time);
            sleep(Math.min(ANIMATION_REFRESH_RATE, timeLeft));

            time += ANIMATION_REFRESH_RATE - start + System.currentTimeMillis();
        }
    }

    /**
     * Animates the movement of all agents for the last step.
     *
     * @param durationMs the duration this animation should last.
     */
    private void animateStep(double durationMs, UserInputPanel.UserInputState expectedState) {
        animate(durationMs, draw.agents::animate, expectedState);
    }

    /**
     * Animates the process of selection.
     *
     * @param durationMs the duration this animation should last.
     */
    private void animateSelection(double durationMs, UserInputPanel.UserInputState expectedState) {
        animate(durationMs, draw.selection::animate, expectedState);
    }
}
