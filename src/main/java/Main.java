import simulation.Simulation;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        File configFile = new File("rsc/config.json");
        Simulation simulation = new Simulation(configFile);
        simulation.start(1000);
    }
}