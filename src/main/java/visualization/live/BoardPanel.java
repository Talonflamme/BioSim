package visualization.live;

import simulation.Simulation;

import javax.swing.*;
import java.awt.*;


abstract class BoardPanel extends JPanel {
    static final int MAX_HEIGHT = 700;
    static final int MAX_WIDTH = 900;

    /**
     * Convert in game units to pixels.
     */
    float factor;
    private float animationProgress;
    protected final Simulation simulation;

    protected BoardPanel(Simulation simulation) {
        super();
        animationProgress = 1f;
        this.simulation = simulation;
    }

    void calcBounds(int xOff, int yOff, int parentWidth, int parentHeight) {
        int boardWidth = simulation.getConfig().getBoardWidth();
        int boardHeight = simulation.getConfig().getBoardHeight();
        factor = Math.min((float) MAX_WIDTH / boardWidth, (float) MAX_HEIGHT / boardHeight);
        float w = boardWidth * factor;
        float h = boardHeight * factor;
        setBounds(xOff + (int) Math.floor((parentWidth - w) / 2f),
                yOff + (int) Math.floor((parentHeight - h) / 2f),
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
