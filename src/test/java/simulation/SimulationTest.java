package simulation;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregationException;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import simulation.config.ConfigError;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("SpellCheckingInspection")
public class SimulationTest {
    private static FakeSimulationConfig config;
    private static FakeSimulation simulation;

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
    }

    private void movementGrid(String data) {
        int count = -1;
        int width = -1;
        int x = 0;
        int y = 0;
        while (++count < data.length()) {
            char ch = data.charAt(count);
            switch (ch) {
                case '\n' -> {
                    y++;
                    if (width == -1)
                        width = x;
                    else if (x != width) {
                        throw new IllegalArgumentException("%s at %d".formatted(data, count));
                    }
                    x = 0;
                }
                case '-', 'C' -> x++;
                case 'N', 'S', 'W', 'E' -> {
                    Direction dir;
                    if ("NS".indexOf(ch) != -1 && count + 1 < data.length() && "ew".indexOf(data.charAt(count + 1)) != -1) {
                        char next = data.charAt(++count);
                        dir = Direction.valueOf(ch + String.valueOf(next).toUpperCase());
                    } else {
                        dir = Direction.valueOf(String.valueOf(ch));
                    }
                    simulation.queueUpMove(new FakeAgent(x, y), x + dir.getX(), y + dir.getY());
                    x++;
                }
                default -> throw new IllegalArgumentException("%s at %d".formatted(data, count));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("provideUpdateMovesArgs")
    void updateMoves(String movementGrid, @AggregateWith(VarargsAggregator.class) String... expected) {
        final FakeGrid grid = new FakeGrid(movementGrid
                .replaceAll("[EWC]|[NS][ew]?", "X")
                .replace('-', 'O'));

        int samples = 100; // with 100 iterations, the chance of a false-negative is extremely tiny
        boolean[] matches = new boolean[expected.length];

        for (int i = 0; i < samples; i++) {
            simulation.setGrid(new FakeGrid(grid));
            simulation.setState(Simulation.SimulationState.OnAgentThink);

            movementGrid(movementGrid);
            simulation.setState(Simulation.SimulationState.MoveUpdate);
            assertDoesNotThrow(simulation::updateMoves);
            String result = simulation.getGrid().toData();

            // should match any
            assertTrue(ArrayUtils.contains(expected, result));

            for (int j = 0; j < expected.length; j++) {
                matches[j] |= expected[j].equals(result);
            }

            // all expected values have been matched at least once
            if (BooleanUtils.and(matches)) {
                break;
            }
        }

        // check if all have been matched
        for (int i = 0; i < expected.length; i++) {
            assertTrue(matches[i], "Expected grid: %n`%n%s`%n not matched".formatted(expected[i]));
        }
    }

    private static Stream<Arguments> provideUpdateMovesArgs() {
        return Stream.of(
                Arguments.of("""
                        -N-WW-
                        Ne--C--
                        --S---
                        ---Sw--
                        ----NS
                        --EW--
                        """, """
                        OXXXOO
                        XOOXOO
                        OOOOOO
                        OOXOXO
                        OOXOOO
                        OOXXOX
                        """),
                Arguments.of("""
                        -S-
                        E--
                        N-Nw
                        """, """
                        OOO
                        XXO
                        XOX
                        """, """
                        OXO
                        XXO
                        OOX
                        """, """
                        OXO
                        XXO
                        XOO
                        """),
                Arguments.of("""
                        EES-
                        -SwE-
                        S--N
                        Ne--N
                        """, """
                        XXXO
                        OOXX
                        XXOX
                        XOOO
                        """, """
                        OXXO
                        OOXX
                        XXOX
                        XOOX
                        """)
        );
    }

    private static class VarargsAggregator implements ArgumentsAggregator {
        @Override
        public Object aggregateArguments(ArgumentsAccessor accessor, ParameterContext context) throws ArgumentsAggregationException {
            return accessor.toList().stream()
                    .skip(context.getIndex())
                    .map(String::valueOf)
                    .toArray(String[]::new);
        }
    }
}
