package simulation;

import simulation.brain.*;

public class Agent {
    private int x;
    private int y;
    /**
     * The current step
     */
    private int step = -1;
    private final int id;
    private Direction lastMovement = Direction.CENTER;
    private final Brain brain;
    private boolean alive = true;

    /**
     * Generate a complete random agent.
     *
     * @param x     The x coordinate.
     * @param y     The y coordinate.
     * @param brain The brain.
     */
    public Agent(int x, int y, Brain brain, int id) {
        this.x = x;
        this.y = y;
        this.brain = brain;
        this.id = id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Brain getBrain() {
        return brain;
    }

    public boolean isAlive() {
        return alive;
    }

    public Direction getForward() {
        return lastMovement;
    }

    @Override
    public String toString() {
        return "Agent[x=%3d, y=%3d, alive=%b, id=%d, brain=\"%s\"]".formatted(x, y, alive, id, brain.genomeToString());
    }

    public void step() {
        brain.think(this);
    }

    /**
     * Queues up the desired move. Does not ensure that the move is executed.
     *
     * @param dir the direction.
     */
    public void queueMove(Direction dir) {
        Simulation.activeSimulation.queueUpMove(this, getX() + dir.getX(), getY() + dir.getY());
    }

    /**
     * Called at the end of the step.
     *
     * @param newX the new x coordinate of the agent. Can be the same as before
     * @param newY the new y coordinate of the agent. Can be the same as before
     */
    public void afterStep(int newX, int newY, SimulationProgress progress) {
        if (this.step == progress.getStep()) return;
        int dx = newX - x;
        int dy = newY - y;
        lastMovement = Direction.get(dx, dy);
        this.x = newX;
        this.y = newY;
        this.step = progress.getStep();
    }

    /**
     * Returns the x coordinate of the last step using {@link #lastMovement}.
     */
    public int getLastX() {
        return x - lastMovement.getX();
    }

    /**
     * Returns the y coordinate of the last step using {@link #lastMovement}
     */
    public int getLastY() {
        return y - lastMovement.getY();
    }

    public int getID() {
        return id;
    }

    /**
     * Sets {@link #alive} to {@code false}.
     */
    public void die() {
        this.alive = false;
    }
}
