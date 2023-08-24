package visualization;

import simulation.*;
import simulation.config.SimulationConfig;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Board {
    static final int PADDING_X = 220;
    static final int PADDING_Y = 220;
    static final int SCALE = 20;
    public static final Color BACKGROUND_COLOR = new Color(0x1E1F22);
    public static final Color RECT_OUTLINE_COLOR = new Color(0xadd8e6); // light blue

    private final SimulationConfig config;
    private boolean recording;
    private BoardAnimation animation;

    public Board(SimulationConfig config) {
        this.config = config;
    }

    private int width() {
        return config.getBoardWidth() * SCALE + PADDING_X;
    }

    private int height() {
        return config.getBoardHeight() * SCALE + PADDING_Y;
    }

    public boolean isRecording() {
        return recording;
    }

    private void drawAgents(Agent[] agents, Graphics2D g) {
        int w = width();
        int h = height();
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, w, h);
        g.setColor(RECT_OUTLINE_COLOR);
        g.setStroke(new BasicStroke(4));
        g.drawRect(PADDING_X / 2, PADDING_Y / 2, w - PADDING_X, h - PADDING_Y);

        for (Agent agent : agents) {
            if (!agent.isAlive()) continue;
            g.setColor(agent.getBrain().getColor());
            float x = agent.getX() * SCALE + PADDING_X / 2f;
            float y = agent.getY() * SCALE + PADDING_Y / 2f;
            g.fillOval(Math.round(x), Math.round(y), SCALE, SCALE);
        }
    }

    public BufferedImage createImage(Agent[] agents) {
        int w = width();
        int h = height();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        drawAgents(agents, g);
        g.dispose();
        return img;
    }

    /**
     * Creates a snapshot of the current position of agents.
     */
    public void addMovement(Agent[] agents) {
        if (!recording) {
            return;
        }
        animation.addFrame(agents);
    }

    /**
     * Adds the animation for visualizing how selections kill agents/don't kill agents.
     *
     * @param config  the Config from which to take the {@link SimulationConfig#getSelection selections}.
     * @param seconds the amount of seconds this animation should last.
     */
    public void addSelection(SimulationConfig config, float seconds) {
        animation.addFrame(config.getSelection(), seconds);
    }

    /**
     * Creates a snapshot. Only processes the alive state of agents. Not the position.
     *
     * @param fadeAwayTransition the transition in seconds of the agents fading away.
     */
    public void addAlive(Agent[] agents, float fadeAwayTransition) {
        animation.addAlive(agents, fadeAwayTransition);
    }

    /**
     * The method to start recording. This essentially only assigns initializes {@link #animation}.
     * Still needs to be called before calling {@link #addMovement}
     */
    public void startRecording() {
        animation = new BoardAnimation(config);
        recording = true;
    }

    public void saveRecording(String path) {
        if (animation == null) {
            throw new NullPointerException("animation is null, did you forget to call `startRecording()`?");
        }
        animation.save(path);
    }

    public void clear() {
        animation.clear();
    }

    /**
     * Calls {@link #saveRecording} and {@link #clear} in a new Thread.
     */
    public void saveRecordingThreaded(final String path) {
        final BoardAnimation anim = animation;
        startRecording(); // create new animation: this way, there are no conflicts
        Thread thread = new Thread(() -> {
            anim.save(path);
            anim.clear();
        });
        thread.start();
    }
}
