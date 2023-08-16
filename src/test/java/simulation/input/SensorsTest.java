package simulation.input;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import simulation.*;
import simulation.config.ConfigError;
import simulation.input.exceptions.*;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SpellCheckingInspection")
public class SensorsTest {
    private static final String TEST_GRID_DATA = """
            XOOOOXOXOXOO
            OOOOXXXXOOOX
            XXXXXXXXXXXX
            OOOXOXOXXXOO
            XXXXXOOXXXOO
            XOOOOOOOOOOO
            OOOOOOOOOOOO
            OOOOOOOOOOOO
            """;
    private static FakeSimulationConfig config;
    private static FakeSimulation simulation;

    /**
     * Computes the sum of the squares from 1² to n². Uses the formula:
     * <pre>
     *     {@code n(n + 1)(2n + 1) / 6}
     * </pre>
     *
     * @param n the upper limit base number
     */
    private static int sumOfSquares(int n) {
        return n * (n + 1) * (2 * n + 1) / 6;
    }

    private static SimulationProgress progress() {
        return simulation.getProgress();
    }

    @BeforeAll
    static void setup() throws ConfigError {
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

    @Test
    void getPopulation() {
        FakeGrid grid = new FakeGrid(TEST_GRID_DATA);
        FakeAgent agent = new FakeAgent();

        // case 1
        Sensors.POPULATION_SCAN_RADIUS = 1;
        agent.setXY(0, 0);
        assertEquals(0f, Sensors.getPopulation(agent, grid));

        // case 2
        Sensors.POPULATION_SCAN_RADIUS = 2;
        agent.setXY(0, 0);
        assertEquals((1 + 2 + 3) / 27f, Sensors.getPopulation(agent, grid));

        // case 3
        Sensors.POPULATION_SCAN_RADIUS = 1;
        agent.setXY(1, 5);
        assertEquals(0.5f, Sensors.getPopulation(agent, grid));
    }

    @ParameterizedTest
    @MethodSource("providePopulationAlongAxisArgs")
    void getPopulationAlongAxis(BiFunction<Agent, Grid, Float> f, int rotation) {
        FakeGrid grid = new FakeGrid(TEST_GRID_DATA);
        FakeAgent agent = new FakeAgent();

        // case 1
        Sensors.POPULATION_SCAN_RADIUS = 6;
        agent.setXY(5, 3);
        agent.setForward(Direction.CENTER);
        assertEquals(0f, f.apply(agent, grid));

        // case 2
        Sensors.POPULATION_SCAN_RADIUS = 4;
        agent.setXY(0, 0);
        agent.setForward(Direction.E.rotate(rotation));
        assertEquals(0f, f.apply(agent, grid));

        // case 3
        Sensors.POPULATION_SCAN_RADIUS = 5;
        assertEquals(1f / sumOfSquares(5), f.apply(agent, grid));

        // case 4
        agent.setForward(Direction.W.rotate(rotation)); // should be same as E
        assertEquals(1f / sumOfSquares(5), f.apply(agent, grid));

        // case 5
        agent.setXY(11, 3);
        agent.setForward(Direction.S.rotate(rotation));
        assertEquals((25f + 16f) / (sumOfSquares(5) * 2 - sumOfSquares(2) - 1), f.apply(agent, grid));

        // case 6
        agent.setForward(Direction.N.rotate(rotation)); // should be same as N
        assertEquals((25f + 16f) / (sumOfSquares(5) * 2 - sumOfSquares(2) - 1), f.apply(agent, grid));

        // case 7
        agent.setXY(0, 2);
        agent.setForward(Direction.NE.rotate(rotation));
        assertEquals(0f, f.apply(agent, grid));

        // case 8
        agent.setForward(Direction.SW.rotate(rotation)); // should be same as NE
        assertEquals(0f, f.apply(agent, grid));

        // case 9
        agent.setXY(11, 3);
        agent.setForward(Direction.NW.rotate(rotation));
        assertEquals(25f / (sumOfSquares(5) - sumOfSquares(2)), f.apply(agent, grid));

        // case 10
        agent.setForward(Direction.SE.rotate(rotation)); // should be same as NW
        assertEquals(25f / (sumOfSquares(5) - sumOfSquares(2)), f.apply(agent, grid));

        // case 11
        agent.setForward(Direction.NW.rotate(rotation));
        agent.setXY(0, 7);
        assertEquals(0f, f.apply(agent, grid));
    }

    private static Stream<Arguments> providePopulationAlongAxisArgs() {
        BiFunction<Agent, Grid, Float> fwd = Sensors::getPopulationForward;
        BiFunction<Agent, Grid, Float> perp = Sensors::getPopulationPerpendicular;
        return Stream.of(
                Arguments.of(fwd, 0),
                Arguments.of(perp, 90)
        );
    }

    @Test
    void getAge() {
        config.setStepsPerGen(300);

        // case 1
        progress().setStep(0);
        assertEquals(0f, Sensors.getAge());

        // case 2
        progress().setStep(50);
        assertEquals(50f / 300f, Sensors.getAge());

        // case 3
        progress().setStep(300);
        assertEquals(1f, Sensors.getAge());

        // case 4
        progress().setStep(301);
        assertThrows(StepOutOfRangeException.class, Sensors::getAge);
    }

    @Test
    void getDistanceLeft() {
        config.setBoardWidth(30);
        FakeAgent agent = new FakeAgent();

        // case 1
        agent.setX(0);
        assertEquals(0f, Sensors.getDistanceLeft(agent));

        // case 2
        agent.setX(29);
        assertEquals(1f, Sensors.getDistanceLeft(agent));

        // case 3
        agent.setX(-1);
        assertThrows(AgentPositionException.class, () -> Sensors.getDistanceLeft(agent));

        // case 4
        agent.setX(30);
        assertThrows(AgentPositionException.class, () -> Sensors.getDistanceLeft(agent));

        // case 5
        config.setBoardWidth(11);
        agent.setX(5);
        assertEquals(0.5f, Sensors.getDistanceLeft(agent));
    }

    @Test
    void getDistanceTop() {
        config.setBoardHeight(20);
        FakeAgent agent = new FakeAgent();

        // case 1
        agent.setY(0);
        assertEquals(0f, Sensors.getDistanceTop(agent));

        // case 2
        agent.setY(19);
        assertEquals(1f, Sensors.getDistanceTop(agent));

        // case 3
        agent.setY(-1);
        assertThrows(AgentPositionException.class, () -> Sensors.getDistanceTop(agent));

        // case 4
        agent.setY(20);
        assertThrows(AgentPositionException.class, () -> Sensors.getDistanceTop(agent));

        // case 5
        config.setBoardHeight(11);
        agent.setY(5);
        assertEquals(0.5f, Sensors.getDistanceTop(agent));
    }

    @Test
    void getNearestDistanceX() {
        config.setBoardWidth(11);
        FakeAgent agent = new FakeAgent();

        // case 1
        agent.setX(0);
        assertEquals(0f, Sensors.getNearestDistanceX(agent));

        // case 2
        agent.setX(1);
        assertEquals(1f / 5f, Sensors.getNearestDistanceX(agent));

        // case 3
        agent.setX(5);
        assertEquals(1f, Sensors.getNearestDistanceX(agent));

        // case 4
        agent.setX(6);
        assertEquals(4f / 5f, Sensors.getNearestDistanceX(agent));

        // case 5
        agent.setX(8);
        assertEquals(2f / 5f, Sensors.getNearestDistanceX(agent));

        // case 6
        agent.setX(10);
        assertEquals(0f, Sensors.getNearestDistanceX(agent));

        // case 7
        agent.setX(-1);
        assertThrows(AgentPositionException.class, () -> Sensors.getNearestDistanceX(agent));

        // case 8
        agent.setX(11);
        assertThrows(AgentPositionException.class, () -> Sensors.getNearestDistanceX(agent));
    }

    @Test
    void getNearestDistanceY() {
        config.setBoardHeight(11);
        FakeAgent agent = new FakeAgent();

        // case 1
        agent.setY(0);
        assertEquals(0f, Sensors.getNearestDistanceY(agent));

        // case 2
        agent.setY(1);
        assertEquals(1f / 5f, Sensors.getNearestDistanceY(agent));

        // case 3
        agent.setY(5);
        assertEquals(1f, Sensors.getNearestDistanceY(agent));

        // case 4
        agent.setY(6);
        assertEquals(4f / 5f, Sensors.getNearestDistanceY(agent));

        // case 5
        agent.setY(8);
        assertEquals(2f / 5f, Sensors.getNearestDistanceY(agent));

        // case 6
        agent.setY(10);
        assertEquals(0f, Sensors.getNearestDistanceY(agent));

        // case 7
        agent.setY(-1);
        assertThrows(AgentPositionException.class, () -> Sensors.getNearestDistanceY(agent));

        // case 8
        agent.setY(11);
        assertThrows(AgentPositionException.class, () -> Sensors.getNearestDistanceY(agent));
    }

    @Test
    void getNearestDistance() {
        config.setBoardWidth(11);
        config.setBoardHeight(15);
        FakeAgent agent = new FakeAgent();

        // case 1
        agent.setXY(0, 0);
        assertEquals(0f, Sensors.getNearestDistance(agent));

        // case 2
        agent.setXY(3, 0);
        assertEquals(0f, Sensors.getNearestDistance(agent));

        // case 3
        agent.setXY(5, 1);
        assertEquals(2f / 14f, Sensors.getNearestDistance(agent));

        // case 5
        agent.setXY(4, 12);
        assertEquals(4f / 14f, Sensors.getNearestDistance(agent));

        // case 6
        agent.setXY(9, 4);
        assertEquals(2f / 10f, Sensors.getNearestDistance(agent));

        // case 7
        agent.setXY(10, 12);
        assertEquals(0f, Sensors.getNearestDistance(agent));

        // case 8
        agent.setXY(5, 7);
        assertEquals(1f, Sensors.getNearestDistance(agent));

        // case 9
        agent.setXY(1, 14);
        assertEquals(0f, Sensors.getNearestDistance(agent));

        // case 10
        agent.setXY(-1, 5);
        assertThrows(AgentPositionException.class, () -> Sensors.getNearestDistance(agent));

        // case 11
        agent.setXY(3, -1);
        assertThrows(AgentPositionException.class, () -> Sensors.getNearestDistance(agent));

        // case 12
        agent.setXY(11, 5);
        assertThrows(AgentPositionException.class, () -> Sensors.getNearestDistance(agent));

        // case 13
        agent.setXY(5, 15);
        assertThrows(AgentPositionException.class, () -> Sensors.getNearestDistance(agent));

        // case 14
        agent.setXY(-1, -49);
        assertThrows(AgentPositionException.class, () -> Sensors.getNearestDistance(agent));

        // case 15
        agent.setXY(15, 19);
        assertThrows(AgentPositionException.class, () -> Sensors.getNearestDistance(agent));
    }

    @Test
    void getRandom() {
        float sum = 0f;
        int samples = 10000;

        for (int i = 0; i < samples; i++) {
            float result = Sensors.getRandom();
            assertTrue(result >= 0f && result < 1f, "%f expected to be in range 0..1".formatted(result));
            sum += result;
        }

        // after 10000 iterations, the average should be ~0.5
        float mean = sum / samples;
        assertTrue(mean > 0.4f && mean < 0.6f,
                "mean after %d iterations expected to be ~0.5, but was %f".formatted(samples, mean));
    }

    @Test
    void getLastMovementX() {
        FakeAgent agent = new FakeAgent();

        // case 1
        agent.setForward(Direction.E);
        assertEquals(1f, Sensors.getLastMovementX(agent));

        // case 2
        agent.setForward(Direction.S);
        assertEquals(0.5f, Sensors.getLastMovementX(agent));

        // case 3
        agent.setForward(Direction.N);
        assertEquals(0.5f, Sensors.getLastMovementX(agent));

        // case 4
        agent.setForward(Direction.W);
        assertEquals(0f, Sensors.getLastMovementX(agent));

        // case 5
        agent.setForward(Direction.NW);
        assertEquals(0f, Sensors.getLastMovementX(agent));

        // case 6
        agent.setForward(Direction.NE);
        assertEquals(1f, Sensors.getLastMovementX(agent));

        // case 7
        agent.setForward(Direction.SE);
        assertEquals(1f, Sensors.getLastMovementX(agent));

        // case 8
        agent.setForward(Direction.SW);
        assertEquals(0f, Sensors.getLastMovementX(agent));

        // case 9
        agent.setForward(Direction.CENTER);
        assertEquals(0.5f, Sensors.getLastMovementX(agent));

        // case 10
        agent.setForward(null);
        assertThrows(UnexpectedLastMovementException.class, () -> Sensors.getLastMovementX(agent));
    }

    @Test
    void getLastMovementY() {
        FakeAgent agent = new FakeAgent();

        // case 1
        agent.setForward(Direction.E);
        assertEquals(0.5f, Sensors.getLastMovementY(agent));

        // case 2
        agent.setForward(Direction.S);
        assertEquals(1f, Sensors.getLastMovementY(agent));

        // case 3
        agent.setForward(Direction.N);
        assertEquals(0f, Sensors.getLastMovementY(agent));

        // case 4
        agent.setForward(Direction.W);
        assertEquals(0.5f, Sensors.getLastMovementY(agent));

        // case 5
        agent.setForward(Direction.NW);
        assertEquals(0f, Sensors.getLastMovementY(agent));

        // case 6
        agent.setForward(Direction.NE);
        assertEquals(0f, Sensors.getLastMovementY(agent));

        // case 7
        agent.setForward(Direction.SE);
        assertEquals(1f, Sensors.getLastMovementY(agent));

        // case 8
        agent.setForward(Direction.SW);
        assertEquals(1f, Sensors.getLastMovementY(agent));

        // case 9
        agent.setForward(Direction.CENTER);
        assertEquals(0.5f, Sensors.getLastMovementY(agent));

        // case 10
        agent.setForward(null);
        assertThrows(UnexpectedLastMovementException.class, () -> Sensors.getLastMovementY(agent));
    }
}
