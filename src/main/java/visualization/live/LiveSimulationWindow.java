package visualization.live;

import simulation.Simulation;

import javax.swing.*;

import static utility.Utility.yellow;

public class LiveSimulationWindow {
    private final JFrame frame;
    private final Simulation simulation;
    private final Draw draw;

    public LiveSimulationWindow(Simulation simulation) {
        this.simulation = simulation;
        frame = new JFrame("BioSim");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);

        draw = new Draw(simulation, frame);

        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public void show() {
        frame.setVisible(true);
    }

    /**
     * How many milliseconds it should take to display one step.
     */
    private static final long MS_PER_STEP = 50;
    /**
     * How many milliseconds it should take to display the selection process.
     */
    private static final long MS_SELECTION = 7_000;
    private static final double ANIMATION_REFRESH_RATE = 1000.0 / 60.0;

    public void start() {
        do {
            long startMs = System.currentTimeMillis();
            simulation.liveStep();
            long endMs = System.currentTimeMillis();

            startAnimation(endMs - startMs);
        } while (simulation.getState() != Simulation.SimulationState.Inactive);
        System.out.println(yellow("Finished"));
    }

    /**
     * starts an animation based on the {@link Simulation#getState state}.
     *
     * @param elapsedMs the time in milliseconds that the step took
     */
    private void startAnimation(long elapsedMs) {
        switch (simulation.getState()) {
            case AfterStep -> animateStep(MS_PER_STEP - elapsedMs);
            case AfterSelection -> animateSelection(MS_SELECTION - elapsedMs);
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
     */
    private static void animate(long durationMs, AnimationConsumer animationFunc) {
        double time = 0.0; // elapsed time since method start
        while (time < durationMs) {
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
    private void animateStep(long durationMs) {
        animate(durationMs, draw.agents::animate);
    }

    /**
     * Animates the process of selection.
     *
     * @param durationMs the duration this animation should last.
     */
    private void animateSelection(long durationMs) {
        animate(durationMs, draw.selection::animate);
    }
}
