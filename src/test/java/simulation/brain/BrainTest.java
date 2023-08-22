package simulation.brain;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import simulation.FakeSimulation;
import simulation.FakeSimulationConfig;
import simulation.Simulation;
import simulation.SimulationProgress;
import simulation.config.ConfigError;
import simulation.config.SimulationConfig;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BrainTest {
    private static FakeSimulation simulation;
    private static FakeSimulationConfig config;

    @BeforeAll
    public static void setup() throws ConfigError {
        if (Simulation.activeSimulation != null) {
            config = (FakeSimulationConfig) Simulation.activeSimulation.getConfig();
            simulation = (FakeSimulation) Simulation.activeSimulation;
        } else {
            config = new FakeSimulationConfig();
            config.setBoardWidth(1);
            config.setBoardHeight(1);
            simulation = new FakeSimulation(config);
            simulation.setProgress(new SimulationProgress());
        }

        config.setMaxInnerNeurons(1);
    }


    @ParameterizedTest
    @MethodSource("provideGeneticDiversityParams")
    void similarity(String a, String b, int sameBits) {
        int len = a.length();
        assert len == b.length() && len % 32 == 0 && len != 0;

        int[] g1 = Arrays.stream(a.split("(?<=\\G.{32})")).mapToInt(x -> Integer.parseUnsignedInt(x, 2)).toArray();
        int[] g2 = Arrays.stream(b.split("(?<=\\G.{32})")).mapToInt(x -> Integer.parseUnsignedInt(x, 2)).toArray();
        Brain b1 = new Brain(g1);
        Brain b2 = new Brain(g2);

        float value = b1.similarity(b2);
        assertEquals(sameBits, (int) (value * len));
    }

    static Stream<Arguments> provideGeneticDiversityParams() {
        return Stream.of(
                Arguments.of("00000000000000000000000000000000", "11111111111111111111111111111111", 0),
                Arguments.of("01010101010101010101010101010101", "01010101010101010101010101010101", 32),
                Arguments.of("00000000010000000000000000000000", "11111101111111100000000001000000", 18),
                Arguments.of("10101010101010101010101010101010", "01010101010101010101010101010101", 0),
                Arguments.of("00000000000000000000000000000000", "00000000000000000000000000000000", 32),
                Arguments.of("11111111111111111111111100000000", "01111111111111111111111111111111", 23),
                Arguments.of("11001100110011001100110011001110", "00110011001100110011001100110011", 1),
                Arguments.of("10101010101010101010101010101010", "01010101010101010101010101010101", 0),
                Arguments.of("1100110011001100110011001100110011001100110011001100110011001100",
                        "0011001100110011001100110011001100110011001100110011001100110011", 0),
                Arguments.of("0101010101010101010101010101010110101010101010101010101010101010",
                        "1010101010101010101010101010101010101010101010101010101010101010", 32),
                Arguments.of("1111111111111111111111111111111111111111111111111111111111111111",
                        "0000000000000000000000000000000000000000000000000000000000000000", 0)
        );
    }

}
