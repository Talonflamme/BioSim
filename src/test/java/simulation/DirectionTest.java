package simulation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

public class DirectionTest {

    @ParameterizedTest
    @MethodSource("provideRotateTestCases")
    void rotate(Direction direction, int rotation, Direction expected) {
        if (expected == null) {
            assertThrows(IllegalArgumentException.class, () -> direction.rotate(rotation));
        } else {
            assertEquals(expected, direction.rotate(rotation));
        }
    }

    @ParameterizedTest
    @MethodSource("provideGetTestCases")
    void get(int x, int y, Direction expected) {
        assertEquals(expected, Direction.get(x, y));
    }

    @Test
    void reverse() {
        assertEquals(Direction.W, Direction.E.reverse());
        assertEquals(Direction.E, Direction.W.reverse());
        assertEquals(Direction.S, Direction.N.reverse());
        assertEquals(Direction.N, Direction.S.reverse());
        assertEquals(Direction.SE, Direction.NW.reverse());
        assertEquals(Direction.SW, Direction.NE.reverse());
        assertEquals(Direction.NW, Direction.SE.reverse());
        assertEquals(Direction.NE, Direction.SW.reverse());
        assertEquals(Direction.CENTER, Direction.CENTER.reverse());
    }

    @Test
    void random() {
        // get a few samples & check whether the count is expected as the .random() function should be uniformly
        // distributed. When samples is too small, this test might falsely fail.
        int samples = 10000;
        Direction[] directions = Direction.values();
        float expected = 1f / directions.length;
        int[] counts = new int[directions.length];

        for (int i = 0; i < samples; i++) {
            Direction d = Direction.random();
            counts[d.ordinal()]++;
        }

        for (int i = 0; i < counts.length; i++) {
            Direction dir = directions[i];
            assertEquals((float) counts[i] / samples, expected, 0.1f, "Probability outside of acceptable range for Direction.%s".formatted(dir));
        }
    }

    private static Stream<Arguments> provideRotateTestCases() {
        return Stream.of(
                Arguments.of(Direction.N, 45, Direction.NE),
                Arguments.of(Direction.S, 90, Direction.W),
                Arguments.of(Direction.E, -225, Direction.SW),
                Arguments.of(Direction.W, 180, Direction.E),
                Arguments.of(Direction.W, 0, Direction.W),
                Arguments.of(Direction.NE, 225, Direction.W),
                Arguments.of(Direction.SW, 270, Direction.SE),
                Arguments.of(Direction.NW, 675, Direction.W),
                Arguments.of(Direction.SE, 360, Direction.SE),
                Arguments.of(Direction.N, 90, Direction.E),
                Arguments.of(Direction.S, 540, Direction.N),
                Arguments.of(Direction.E, 270, Direction.N),
                Arguments.of(Direction.E, 0, Direction.E),
                Arguments.of(Direction.W, 360, Direction.W),
                Arguments.of(Direction.NE, 45, Direction.E),
                Arguments.of(Direction.SW, -270, Direction.NW),
                Arguments.of(Direction.NW, 135, Direction.E),
                Arguments.of(Direction.SE, -180, Direction.NW),
                Arguments.of(Direction.N, -90, Direction.W),
                Arguments.of(Direction.N, 0, Direction.N),
                Arguments.of(Direction.S, -360, Direction.S),
                Arguments.of(Direction.S, 0, Direction.S),
                Arguments.of(Direction.E, 45, Direction.SE),
                Arguments.of(Direction.W, 90, Direction.N),
                Arguments.of(Direction.CENTER, -315, Direction.CENTER),
                Arguments.of(Direction.CENTER, 180, Direction.CENTER),
                Arguments.of(Direction.CENTER, 0, Direction.CENTER),
                Arguments.of(Direction.S, 39, null),
                Arguments.of(Direction.E, 1, null),
                Arguments.of(Direction.CENTER, 1, Direction.CENTER),
                Arguments.of(Direction.CENTER, -99, Direction.CENTER)
        );
    }

    private static Stream<Arguments> provideGetTestCases() {
        return Stream.of(
                Arguments.of(0, 0, Direction.CENTER),
                Arguments.of(0, 1, Direction.S),
                Arguments.of(0, -1, Direction.N),
                Arguments.of(-1, 0, Direction.W),
                Arguments.of(1, 0, Direction.E),
                Arguments.of(-1, 1, Direction.SW),
                Arguments.of(1, 1, Direction.SE),
                Arguments.of(1, -1, Direction.NE),
                Arguments.of(-1, -1, Direction.NW)
        );
    }
}
