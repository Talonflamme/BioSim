package simulation;

import org.apache.commons.lang3.StringUtils;

public class FakeGrid extends Grid {
    static int getWidth(String data) {
        return data.indexOf('\n');
    }

    static int getHeight(String data) {
        return StringUtils.countMatches(data, '\n');
    }

    public FakeGrid(String data) {
        super(getWidth(data), getHeight(data));

        int count = 0;
        for (int j = 0; j < super.getHeight(); j++) {
            for (int i = 0; i < super.getWidth(); i++) {
                char ch = data.charAt(count++);
                if (ch == 'X') {
                    set(i, j, true);
                } else if (ch != 'O') {
                    throw new IllegalArgumentException("%s at %d".formatted(data, count - 1));
                }
            }
            if (data.charAt(count++) != '\n') {
                throw new IllegalArgumentException("%s at %d".formatted(data, count - 1));
            }
        }
    }

    public FakeGrid(Grid grid) {
        super(grid);
    }

    @Override
    public String toString() {
        return "FakeGrid[width=%d, height=%d]".formatted(getWidth(), getHeight());
    }

    public String toData() {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < getHeight(); j++) {
            for (int i = 0; i < getWidth(); i++) {
                sb.append(get(i, j) ? 'X' : 'O');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Grid grid && grid.bitSet.equals(this.bitSet);
    }
}
