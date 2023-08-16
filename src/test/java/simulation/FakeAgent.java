package simulation;

import simulation.brain.Brain;

import static utility.Utility.Rand;

public class FakeAgent extends Agent {
    private int x, y;
    private Direction forward = null;
    private boolean forwardSet = false;
    public FakeAgent(int x, int y, Brain brain) {
        super(x, y, brain, Rand.nextInt());
        this.x = x;
        this.y = y;
    }

    public FakeAgent(int x, int y) {
        this(x, y, null);
    }

    public FakeAgent() {
        this(0, 0);
    }

    @Override
    public String toString() {
        return "FakeAgent[x=%d, y=%d%s]".formatted(getX(), getY(), forward == null ? "" : ", fwd=" + getForward());
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setXY(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public Direction getForward() {
        if (!forwardSet) {
            throw new IllegalStateException("Forward direction not yet set, but is accessed");
        }
        return forward;
    }

    public void setForward(Direction forward) {
        this.forward = forward;
        forwardSet = true;
    }
}
