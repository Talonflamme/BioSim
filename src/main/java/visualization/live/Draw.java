package visualization.live;

import simulation.Simulation;

import javax.swing.*;
import java.awt.*;

class Draw extends JPanel {
    private static final int WIDTH = 1180;
    private static final int HEIGHT = 770;

    final AgentsPanel agents;
    final SelectionPanel selection;
    final InformationPanel information;

    Draw(Simulation simulation, JFrame frame) {

        setLayout(null);
        setBackground(new Color(43, 45, 48));
        frame.setContentPane(this);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));

        agents = new AgentsPanel(simulation);
        add(agents);
        agents.calcBounds((int) (WIDTH * -0.18f), 0, WIDTH, HEIGHT);

        selection = new SelectionPanel(simulation, this);
        add(selection);
        selection.calcBounds((int) (WIDTH * -0.18f), 0, WIDTH, HEIGHT);

        int informationLeft = agents.getX() + agents.getWidth();
        information = new InformationPanel(informationLeft, agents.getY(), WIDTH - informationLeft, agents.getHeight());
        add(information);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
}
