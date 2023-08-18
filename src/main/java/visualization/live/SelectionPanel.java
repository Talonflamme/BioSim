package visualization.live;

import simulation.Simulation;

import java.awt.*;
import java.awt.geom.AffineTransform;

import static utility.Utility.TRANSPARENT;

class SelectionPanel extends BoardPanel {
    private final Draw draw;

    SelectionPanel(Simulation simulation, Draw draw) {
        super(simulation);
        this.draw = draw;
        setBackground(TRANSPARENT);
    }

    @Override
    void draw(Graphics2D g, float animationProgress) {
        draw.agents.draw(g, animationProgress, animationProgress < 0.5f ? 1 : 2 * (1f - animationProgress));
        AffineTransform tx = g.getTransform();
        tx.scale(factor, factor);
        g.setTransform(tx);

        for (var selection : simulation.getConfig().getSelection()) {
            selection.draw(g, animationProgress);
        }
    }
}