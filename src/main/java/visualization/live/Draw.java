package visualization.live;

import simulation.Simulation;

import javax.swing.*;
import java.awt.*;

class Draw extends JPanel {
    final AgentsPanel agents;
    final SelectionPanel selection;

    Draw(Simulation simulation, JFrame frame) {

        setLayout(null);
        setBackground(new Color(43, 45, 48));
        frame.setContentPane(this);
        setPreferredSize(new Dimension(1180, 770));

        agents = new AgentsPanel(simulation);
        add(agents);
        agents.center(1180, 770);

        selection = new SelectionPanel(simulation, this);
        add(selection);
        selection.center(1180, 770);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
}
