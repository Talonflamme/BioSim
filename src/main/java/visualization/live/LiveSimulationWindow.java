package visualization.live;

import simulation.Agent;
import simulation.Simulation;
import simulation.config.Selection;

import javax.swing.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import static utility.Utility.*;
import static visualization.Board.*;

public class LiveSimulationWindow {
    private final JFrame frame;
    private final Simulation simulation;
    private final Draw draw;

    public LiveSimulationWindow(Simulation simulation) {
        this.simulation = simulation;
        frame = new JFrame("BioSim");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);

        draw = new Draw();

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
    private static final long MS_SELECTION = 15_000;

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
            case NewGeneration -> {
                System.out.println("New Generation!");
            }
            case Selection -> animateSelection(MS_SELECTION - elapsedMs);
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

    private static final double ANIMATION_REFRESH_RATE = 1000.0 / 60.0;

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
        long start = System.currentTimeMillis();
        while (time < durationMs) {
            time = System.currentTimeMillis() - start;
            float progress = (float) Math.min(time / durationMs, 1);
            animationFunc.accept(progress);

            double timeLeft = Math.max(0, durationMs - time);
            sleep(Math.min(ANIMATION_REFRESH_RATE, timeLeft));
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

    private class Draw extends JPanel {
        final AgentsPanel agents;
        final SelectionPanel selection;

        Draw() {

            setLayout(null);
            setBackground(new Color(43, 45, 48));
            LiveSimulationWindow.this.frame.setContentPane(this);
            setPreferredSize(new Dimension(1180, 770));

            agents = new AgentsPanel();
            add(agents);
            agents.center(1180, 770);

            selection = new SelectionPanel();
            add(selection);
            selection.center(1180, 770);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
        }
    }

    private abstract class BoardPanel extends JPanel {
        static final int MAX_HEIGHT = 700;
        static final int MAX_WIDTH = 900;

        /**
         * Convert in game units to pixels.
         */
        float factor;
        private float animationProgress;

        protected BoardPanel() {
            super();
            animationProgress = 1f;
        }

        void center(int parentWidth, int parentHeight) {
            int boardWidth = simulation.getConfig().getBoardWidth();
            int boardHeight = simulation.getConfig().getBoardHeight();
            factor = Math.min((float) MAX_WIDTH / boardWidth, (float) MAX_HEIGHT / boardHeight);
            float w = boardWidth * factor;
            float h = boardHeight * factor;
            setBounds((int) Math.floor((parentWidth - w) / 2f),
                    (int) Math.floor((parentHeight - h) / 2f),
                    (int) Math.ceil(w),
                    (int) Math.ceil(h));
        }

        @Override
        protected void paintComponent(Graphics g1d) {
            super.paintComponent(g1d);
            Graphics2D g = (Graphics2D) g1d.create();
            draw(g, animationProgress);
            g.dispose();
        }


        /**
         * Animates between the last state and the current.
         *
         * @param progress The progress of the animation. Should be between 0 and 1.
         */
        void animate(float progress) {
            assert 0f <= progress && progress <= 1f;
            this.animationProgress = progress;
            this.repaint();
        }

        /**
         * Draw this component.
         *
         * @param g                 The Graphics2D to draw on. It is cloned and automatically disposed.
         * @param animationProgress How far the animation is in progress. Value from of 0 to 1.
         */
        abstract void draw(Graphics2D g, float animationProgress);
    }

    private class AgentsPanel extends BoardPanel {

        AgentsPanel() {
            super();
            setLayout(null);
            setBackground(BACKGROUND_COLOR);
        }

        void drawGrid(Graphics2D g) {
            int w = getWidth();
            int h = getHeight();
            Stroke thin = new BasicStroke(0.5f);
            Stroke thick = new BasicStroke(1f);
            g.setColor(RECT_OUTLINE_COLOR);
            Color other = new Color(RECT_OUTLINE_COLOR.getRed(), RECT_OUTLINE_COLOR.getGreen(), RECT_OUTLINE_COLOR.getBlue(), 100);
            // vertical
            // we need a counter variable, since floating points miss precision to check if it is divisible
            int count = 0;
            for (float i = 0; i <= w; i += factor) {
                boolean isThick = count++ % 10 == 0;
                g.setStroke(isThick ? thick : thin);
                g.setColor(isThick ? RECT_OUTLINE_COLOR : other);
                g.draw(new Line2D.Float(i, 0, i, h));
            }
            // horizontal
            count = 0;
            for (float i = 0; i <= h; i += factor) {
                boolean isThick = count++ % 10 == 0;
                g.setStroke(isThick ? thick : thin);
                g.setColor(isThick ? RECT_OUTLINE_COLOR : other);
                g.draw(new Line2D.Float(0, i, w, i));
            }

            g.setStroke(thick);
            g.setColor(RECT_OUTLINE_COLOR);
            g.draw(new Line2D.Float(0.5f, 0, 0.5f, h)); // vertical left
            g.draw(new Line2D.Float(0, 0.5f, w, 0.5f)); // horizontal top
        }

        void drawAgents(Graphics2D g, float animationProgress) {
            Agent[] agents = simulation.getAgents();
            if (agents == null) return;

            for (Agent agent : agents) {
                if (!agent.isAlive()) return;
                int lastX = agent.getLastX();
                int lastY = agent.getLastY();
                int currentX = agent.getX();
                int currentY = agent.getY();
                float x = lastX + animationProgress * (currentX - lastX);
                float y = lastY + animationProgress * (currentY - lastY);

                g.setColor(agent.getBrain().getColor());
                g.fill(new Ellipse2D.Float(x * factor, y * factor, factor, factor));
            }
        }

        @Override
        void draw(Graphics2D g, float animationProgress) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawGrid(g);
            drawAgents(g, animationProgress);
        }
    }

    private class SelectionPanel extends BoardPanel {
        SelectionPanel() {
            super();
            setBackground(TRANSPARENT);
        }

        @Override
        void draw(Graphics2D g, float animationProgress) {
            // todo
        }
    }
}
