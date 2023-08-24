package visualization.live;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;

import static utility.Utility.TRANSPARENT;

class UserInputPanel extends JPanel {
    private static final int WIDTH = 70;
    private static final int HEIGHT = 42;
    private final LiveSimulationWindow window;
    UserInputState state = UserInputState.None;

    UserInputPanel(int right, LiveSimulationWindow window) {
        super();
        this.window = window;

        setBounds(right - WIDTH, 8, WIDTH, HEIGHT);
        setBackground(TRANSPARENT);
    }

    private static final float TRIANGLE_WIDTH = 16f;

    private Path2D getTriangleFastForward(float x) {
        float margin_y = 8f;
        float centerY = HEIGHT / 2f;

        float widthThird = TRIANGLE_WIDTH / 3f;
        Path2D path = new Path2D.Float();
        path.moveTo(x, margin_y);
        path.curveTo(x + widthThird, margin_y, x + widthThird * 2f, centerY - margin_y, x + TRIANGLE_WIDTH, centerY);
        path.curveTo(x + widthThird * 2f, HEIGHT - centerY + margin_y, x + widthThird, HEIGHT - margin_y, x, HEIGHT - margin_y);
        path.closePath();

        return path;
    }

    private void drawSkip(Graphics2D g) {
        float gap = 2f;
        // * 2 since we have 2 gaps with 3 triangles
        float width = TRIANGLE_WIDTH * 3 + gap * 2;

        g.setColor(new Color(1f, 0f, 0f, 0.4f));
        g.setStroke(new BasicStroke(2.5f));

        // left
        float left = (WIDTH - width) / 2f;
        Path2D path = getTriangleFastForward(left);
        g.fill(path);

        // center
        path = getTriangleFastForward(left + TRIANGLE_WIDTH + gap);
        g.fill(path);

        // right
        path = getTriangleFastForward(left + (TRIANGLE_WIDTH + gap) * 2f);
        g.fill(path);
    }

    private void drawFastForward(Graphics2D g) {
        float cx = WIDTH / 2f; // center x

        g.setColor(new Color(0, 0, 0, 0.4f));
        g.setStroke(new BasicStroke(2.5f));

        // right
        Path2D path = getTriangleFastForward(cx + 2f);
        g.fill(path);

        // left
        path = getTriangleFastForward(cx - 17f);
        g.fill(path);
    }

    private void drawPause(Graphics2D g) {
        int cx = WIDTH / 2; // center x

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2.5f));

        // left
        g.drawRoundRect(cx - 12, 6, 9, HEIGHT - 6 * 2, 5, 5);
        g.drawRoundRect(cx + 6, 6, 9, HEIGHT - 6 * 2, 5, 5);
    }

    @Override
    protected void paintComponent(Graphics g1d) {
        super.paintComponent(g1d);

        if (state == UserInputState.None) return;

        Graphics2D g = (Graphics2D) g1d;
        g.setColor(getParent().getBackground());
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(0, 0, 0, 0.1f));
        g.fillRoundRect(0, 0, WIDTH, HEIGHT, 10, 10);

        switch (state) {
            case FastForward -> drawFastForward(g);
            case Skip -> drawSkip(g);
            case Pause -> drawPause(g);
        }
    }

    private void stateChanged() {
        this.repaint();

        if (state == UserInputState.None) {
            JPanel parent = (JPanel) getParent();
            parent.repaint(getBounds()); // make sure parent component is drawn over this
        }
    }

    KeyAdapter getListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_RIGHT && state == UserInputState.None) {
                    state = UserInputState.FastForward;
                    stateChanged();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_RIGHT -> {
                        if (state != UserInputState.FastForward || e.isShiftDown()) {
                            return; // shift = keep fast forwarding even without holding down RIGHT
                        }
                        state = UserInputState.None;
                    }
                    case KeyEvent.VK_SPACE -> {
                        if (state == UserInputState.None) {
                            state = UserInputState.Pause;
                        } else if (state == UserInputState.Pause) {
                            state = UserInputState.None;
                            // we need to start the simulation again
                            // for this, we create a new thread, which acts as the main thread
                            window.startInThread();
                        } else return;
                    }
                    case KeyEvent.VK_K -> {
                        if (state == UserInputState.Skip) {
                            state = UserInputState.None;
                        } else if (state == UserInputState.None) {
                            state = UserInputState.Skip;
                        } else return;
                    }
                    default -> {
                        return;
                    }
                }

                stateChanged();
            }
        };
    }

    // todo: for compute: time of animation based on config variables
    // todo: Bug for compute: second last compression seems to have very few bytes, why?
    // todo: when fast-forward and window out of focus, it seems to be slow again
    enum UserInputState {
        None,
        FastForward,
        Skip,
        Pause
    }
}
