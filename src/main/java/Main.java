import simulation.Simulation;
import simulation.config.SimulationConfig;
import simulation.reports.Report;
import visualization.live.LiveSimulationWindow;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        File configFile = new File("rsc/config.json");
        SimulationConfig config = SimulationConfig.fromFile(configFile);

        try (Report report = Report.create(config, "rsc/logs.log")) {
            Simulation simulation = new Simulation(configFile, report);
//            simulation.start();
            LiveSimulationWindow window = new LiveSimulationWindow(simulation);
            window.show();
            window.start();
        }
    }
}