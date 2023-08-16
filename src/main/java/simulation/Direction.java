package simulation;

import static utility.Utility.*;

public enum Direction {
    /**
     * North
     */
    N,
    /**
     * East
     */
    E,
    /**
     * South
     */
    S,
    /**
     * West
     */
    W,
    /**
     * North East
     */
    NE,
    /**
     * South East
     */
    SE,
    /**
     * South West
     */
    SW,
    /**
     * North West
     */
    NW,
    /**
     * None
     */
    CENTER;

    /**
     * @return the horizontal component of the direction. Not normalized. One of [-1, 0, 1].
     */
    public int getX() {
        return switch (this) {
            case E, NE, SE -> 1;
            case W, NW, SW -> -1;
            case N, S, CENTER -> 0;
        };
    }

    /**
     * @return the vertical component of the direction. Not normalized. One of [-1, 0, 1].
     */
    public int getY() {
        return switch (this) {
            case N, NE, NW -> -1;
            case S, SE, SW -> 1;
            case E, W, CENTER -> 0;
        };
    }

    /**
     * Whether this has a horizontal component (East or West).
     */
    public boolean isHorizontal() {
        return this.getX() != 0;
    }

    /**
     * Whether this has a vertical component (North or South)
     */
    public boolean isVertical() {
        return this.getY() != 0;
    }

    /**
     * Return the perpendicular of the current axis. This is the same as rotating by 90deg.
     */
    public Direction getPerpendicular() {
        return get(getY(), -getX());
    }

    /**
     * @return the direction provided by x and y components. Does not care about value besides being greater/less/equal 0. So
     * <pre>
     *      get(4, 0) == get(1, 0) == {@link Direction#E Direction.E}
     * </pre>
     */
    public static Direction get(int x, int y) {
        if (x == 0 && y == 0) return CENTER;
        if (x > 0 && y == 0) return E;
        if (x < 0 && y == 0) return W;
        if (x == 0 && y > 0) return S;
        if (x == 0) return N; // y < 0
        if (x > 0 && y > 0) return SE;
        if (x > 0) return NE; // y < 0
        if (y > 0) return SW; // x < 0
        return NW; // x < 0 && y < 0
    }

    /**
     * Returns a random, randomly and uniformly distributed direction
     */
    public static Direction random() {
        return Rand.choice(Direction.values());
    }

    public Direction rotate(int degrees) {
        if (this == CENTER) return this;

        if (degrees % 45 != 0) {
            throw new IllegalArgumentException("Rotation must be a multiple of 45: %d".formatted(degrees));
        }

        // formula:
        // x' = x cos(α) - y sin(α)
        // y' = y cos(α) + x sin(α)
        int x = getX();
        int y = getY();
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians), sin = Math.sin(radians);
        double nx = x * cos - y * sin; // new x
        double ny = y * cos + x * sin; // new y
        int inx = (int) Math.round(nx); // int new x
        int iny = (int) Math.round(ny); // int new y
        return get(inx, iny);
    }

    public Direction reverse() {
        return switch (this) {
            case N -> S;
            case E -> W;
            case S -> N;
            case W -> E;
            case NE -> SW;
            case SE -> NW;
            case SW -> NE;
            case NW -> SE;
            case CENTER -> CENTER;
        };
    }
}
