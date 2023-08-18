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
        agents.calcBounds(-130, 0, 1180, 770);
        System.out.println(agents.getLocation());

        selection = new SelectionPanel(simulation, this);
        add(selection);
        selection.calcBounds(-130, 0, 1180, 770);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
}
