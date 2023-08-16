package visualization;

import simulation.*;
import simulation.config.SimulationConfig;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Board {
    static final int PADDING_X = 220;
    static final int PADDING_Y = 220;
    static final int SCALE = 20;
//    static final Color BACKGROUND_COLOR = Color.WHITE;
    static final Color BACKGROUND_COLOR = new Color(0x1E1F22);
    static final Color RECT_OUTLINE_COLOR = new Color(0xadd8e6); // light blue

    private final SimulationConfig config;
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
            g.setColor(agent.getBrain().getUniqueColor());
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

    public void addFrame(Agent[] agents) {
        if (animation == null) {
            throw new NullPointerException("animation is null, did you forget to call `startRecording()`?");
        }
        animation.addFrame(agents);
    }

    /**
     * The method to start recording. This essentially only assigns initializes {@link #animation}.
     * Still needs to be called before calling {@link #addFrame}
     */
    public void startRecording() {
        animation = new BoardAnimation(config);
    }

    public void saveRecording(String path) {
        if (animation == null) {
            throw new NullPointerException("animation is null, did you forget to call `startRecording()`?");
        }
        animation.save(path);
    }
}
