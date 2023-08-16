package simulation;

import org.jetbrains.annotations.Contract;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.List;

public class Grid {
    protected final BitSet bitSet;
    private final int height;
    private final int width;

    public Grid(int width, int height) {
        this.height = height;
        this.width = width;
        this.bitSet = new BitSet(width * height);
    }

    public Grid(Grid copy) {
        this.width = copy.getWidth();
        this.height = copy.getHeight();
        this.bitSet = (BitSet) copy.bitSet.clone();
    }

    private void checkBounds(int x, int y) {
        if (x < 0 || x >= width) {
            throw new IndexOutOfBoundsException("%d out of bounds with width %d".formatted(x, width));
        }
        if (y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("%d out of bounds with height %d".formatted(y, height));
        }
    }

    public boolean isBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean get(int x, int y) {
        checkBounds(x, y);
        return bitSet.get(x * height + y);
    }

    public void set(int x, int y, boolean value) {
        checkBounds(x, y);
        bitSet.set(x * height + y, value);
    }

    public void clear() {
        bitSet.clear();
    }

    /**
     * Sets all positions of {@link Agent#isAlive alive} agents to true
     *
     * @param agents The agents which to process
     */
    public void setAgents(Iterable<Agent> agents) {
        for (Agent agent : agents) {
            if (!agent.isAlive()) continue;
            set(agent.getX(), agent.getY(), true);
        }
    }

    public void setAgents(Agent[] agents) {
        setAgents(List.of(agents));
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @SuppressWarnings("unused")
    @Contract(pure = true)
    public BufferedImage getRepresentation() {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                img.setRGB(i, j, get(i, j) ? Color.WHITE.getRGB() : 0);
            }
        }
        return img;
    }

    @Override
    public String toString() {
        return "Grid[width=%d, height=%d]".formatted(width, height);
    }
}
