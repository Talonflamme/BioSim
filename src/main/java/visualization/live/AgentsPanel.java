package visualization.live;

import simulation.Agent;
import simulation.Simulation;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import static utility.Utility.changeAlpha;
import static visualization.Board.BACKGROUND_COLOR;
import static visualization.Board.RECT_OUTLINE_COLOR;

class AgentsPanel extends BoardPanel {
    AgentsPanel(Simulation simulation) {
        super(simulation);
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

    void drawAgents(Graphics2D g, float animationProgress, float deadAgentOpacity) {
        Agent[] agents = simulation.getAgents();
        if (agents == null) return;

        for (Agent agent : agents) {
            float alpha = agent.isAlive() ? 1 : deadAgentOpacity;
            if (alpha == 0) continue;
            if (alpha == 1) {
                g.setColor(agent.getBrain().getColor());
            } else {
                g.setColor(changeAlpha(agent.getBrain().getColor(), alpha));
            }

            int lastX = agent.getLastX();
            int lastY = agent.getLastY();
            int currentX = agent.getX();
            int currentY = agent.getY();
            float x = lastX + animationProgress * (currentX - lastX);
            float y = lastY + animationProgress * (currentY - lastY);

            g.fill(new Ellipse2D.Float(x * factor, y * factor, factor, factor));
        }
    }

    /**
     * Called from {@link LiveSimulationWindow.SelectionPanel#draw}
     *
     * @param deadAgentOpacity the alpha value from 0-1 of dead agents
     */
    void draw(Graphics2D g, float animationProgress, float deadAgentOpacity) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g);
        drawAgents(g, animationProgress, deadAgentOpacity);
    }

    @Override
    void draw(Graphics2D g, float animationProgress) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g);
        drawAgents(g, animationProgress, 0);
    }
}
