package visualization.live;

import simulation.Simulation;

import javax.swing.*;

import java.awt.*;
import java.awt.geom.Line2D;

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
     * How many milliseconds should it take to display one step.
     */
    private static final long MS_PER_STEP = 1000;

    @SuppressWarnings({"BusyWait", "CallToPrintStackTrace"})
    public void start() {
        do {
            long startMs = System.currentTimeMillis();
            simulation.liveStep();
            long endMs = System.currentTimeMillis();
            long sleepMs = MS_PER_STEP + startMs - endMs;

            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }

            // todo animation
        } while (simulation.getState() != Simulation.SimulationState.Inactive);
    }

    public void step() {

    }

    private class Draw extends JPanel {
        final AgentsPanel agents;

        Draw() {
            agents = new AgentsPanel();

            setLayout(null);
            setBackground(new Color(43, 45, 48));
            LiveSimulationWindow.this.frame.setContentPane(this);

            add(agents);
            setPreferredSize(new Dimension(1180, 770));
            agents.center(1180, 770);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
        }
    }

    private class AgentsPanel extends JPanel {
        static final int MAX_HEIGHT = 700;
        static final int MAX_WIDTH = 900;

        /**
         * Convert in game units to pixels.
         */
        private float factor;

        AgentsPanel() {
            setLayout(null);
            setBackground(BACKGROUND_COLOR);
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

        @Override
        protected void paintComponent(Graphics g1d) {
            super.paintComponent(g1d);
            Graphics2D g = (Graphics2D) g1d.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawGrid(g);

            g.dispose();
        }
    }
}
