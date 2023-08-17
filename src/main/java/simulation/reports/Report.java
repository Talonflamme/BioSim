package simulation.reports;

import simulation.config.SimulationConfig;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.BitSet;

import static utility.Utility.*;

public class Report implements AutoCloseable {
    private final SimulationConfig config;
    private final BitSet enabledFeatures;
    private final PrintStream file;

    public Report(SimulationConfig config, PrintStream file) {
        this.config = config;
        int numFeatures = Feature.values().length;
        this.enabledFeatures = new BitSet(numFeatures);
        this.file = file;
    }

    public static Report create(SimulationConfig config, String file) throws FileNotFoundException {
        // clear file
        PrintWriter clearer = new PrintWriter(file);
        clearer.close();

        PrintStream stream = new PrintStream(file);
        return new Report(config, stream);
    }

    public void mutation(String gene1, String gene2) {
        StringBuilder a = new StringBuilder();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < gene1.length(); i++) {
            char c = gene1.charAt(i);
            if (c == gene2.charAt(i)) {
                a.append(c);
                b.append(c);
            } else {
                a.append(blue(String.valueOf(c)));
                b.append(blue(String.valueOf(gene2.charAt(i))));
            }
        }
        String str = "%s: %s -> %s".formatted(yellow("Mutation"), a, b);
        file.println(str);
        if (isEnabled(Feature.Mutation)) {
            System.out.println(str);
        }
    }

    /**
     * Prints information about the new generation.
     *
     * @param genNum     The generation number
     * @param aliveCount how many agents survived that generation.
     */
    public void newGeneration(int genNum, int aliveCount) {
        int popSize = config.getPopulationSize();
        int numGens = config.getNumGenerations();
        String str = "Generation: %d/%d. [%s] [%s]".formatted(genNum, numGens,
                green(format(aliveCount * 100f / popSize) + "%"),
                red(format(100f - aliveCount * 100f / popSize) + "%"));
        file.println(str);
        if (isEnabled(Feature.AllGenerations) || genNum % 10 == 0) {
            System.out.println(str);
        }
    }

    public void enable(Feature feature) {
        this.enabledFeatures.set(feature.ordinal());
    }

    public void disable(Feature feature) {
        this.enabledFeatures.clear(feature.ordinal());
    }

    public boolean isEnabled(Feature feature) {
        return this.enabledFeatures.get(feature.ordinal());
    }

    @Override
    public void close() {
        file.close();
    }

    public enum Feature {
        Mutation,
        AllGenerations
    }
}
