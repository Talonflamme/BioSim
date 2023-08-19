package visualization;

import simulation.brain.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;

public class BrainVisualizer {
    private static final Color BACKGROUND_COLOR = new Color(0x0a1628);
    private static final Color PRIMARY_COLOR = new Color(0x8655ff);
    private static final Color SECONDARY_COLOR = new Color(0x0091ff);
    private static final Color RED_COLOR = new Color(0xfb3d5d);
    private static final Color GREEN_COLOR = new Color(0x3cfa60);
    public static final int WIDTH = 860;
    public static final int HEIGHT = 910;
    private static final int PADDING = 80;
    private static final int RADIUS = 90;

    private final Brain brain;

    public BrainVisualizer(Brain brain) {
        this.brain = brain;
    }

    private static InputNode[] getInputNodes(Connection[] connections) {
        HashSet<InputNode> set = new HashSet<>();
        for (Connection con : connections) {
            if (con.source() instanceof InputNode) {
                set.add((InputNode) con.source());
            }
        }
        return set.toArray(InputNode[]::new);
    }

    private static InnerNode[] getInnerNodes(Connection[] connections) {
        HashSet<InnerNode> set = new HashSet<>();
        for (Connection con : connections) {
            if (con.source() instanceof InnerNode) {
                set.add((InnerNode) con.source());
            }
            if (con.target() instanceof InnerNode) {
                set.add((InnerNode) con.target());
            }
        }
        return set.toArray(InnerNode[]::new);
    }

    private static OutputNode[] getOutputNodes(Connection[] connections) {
        HashSet<OutputNode> set = new HashSet<>();
        for (Connection con : connections) {
            if (con.target() instanceof OutputNode) {
                set.add((OutputNode) con.target());
            }
        }
        return set.toArray(OutputNode[]::new);
    }

    /**
     * Computes the position of all nodes (Input, Inner & Output).
     *
     * @return a dictionary containing the nodes mapped to the center of their circle.
     */
    private HashMap<Node, Point> computePointsOfNodes(Connection[] connections) {
        InputNode[] inputs = getInputNodes(connections);
        InnerNode[] innerNodes = getInnerNodes(connections);
        OutputNode[] outputs = getOutputNodes(connections);

        int y = Math.round(PADDING * 1.75f);

        HashMap<Node, Point> map = new HashMap<>();

        map.putAll(computePointsOfNodes(inputs, y));
        map.putAll(computePointsOfNodes(innerNodes, (HEIGHT - RADIUS) / 2));
        map.putAll(computePointsOfNodes(outputs, HEIGHT - y - RADIUS));

        return map;
    }

    private void drawNodes(Graphics2D g, HashMap<Node, Point> centers) {
        FontMetrics metrics = g.getFontMetrics();

        centers.forEach((node, point) -> {
            Color color = (node instanceof InnerNode) ? SECONDARY_COLOR : PRIMARY_COLOR;
            int x = point.x - RADIUS / 2;
            int y = point.y - RADIUS / 2;
            g.setColor(color);
            g.fillOval(x, y, RADIUS, RADIUS);
            g.setColor(Color.WHITE);
            String str = node.getShortString();
            int height = metrics.getHeight();
            g.drawString(str, x + (RADIUS - metrics.stringWidth(str)) / 2, y + height / 4 + RADIUS / 2);
        });
    }

    private HashMap<Node, Point> computePointsOfNodes(Node[] nodes, int y) {
        HashMap<Node, Point> map = new HashMap<>();
        if (nodes.length == 0) return map;
        int spaceForEach = (WIDTH - PADDING * 2) / nodes.length;
        for (int i = 0; i < nodes.length; i++) {
            int x = PADDING + spaceForEach * i + (spaceForEach - RADIUS) / 2;
            int y1 = y;
            if (nodes.length > 2) {
                y1 += (1 - i % 2) * 40;
            }
            map.put(nodes[i], new Point(x + RADIUS / 2, y1 + RADIUS / 2));
        }
        return map;
    }

    /**
     * Draws lines based on the connections provided.
     *
     * @param g           The graphics to draw on.
     * @param connections The connections to draw.
     * @param centers     The center positions of each node. Likely a result of {@link #computePointsOfNodes(Connection[])}
     */
    private void drawConnections(Graphics2D g, Connection[] connections, HashMap<Node, Point> centers) {
        float smallestWeight = Float.MAX_VALUE;
        float biggestWeight = 0;
        for (Connection con : connections) {
            smallestWeight = Math.min(smallestWeight, Math.abs(con.weight()));
            biggestWeight = Math.max(biggestWeight, Math.abs(con.weight()));
        }
        float minStroke = 1;
        float maxStroke = 10;
        for (Connection con : connections) {
            float ratio;
            if (biggestWeight - smallestWeight > 0.1) {
                ratio = (Math.abs(con.weight()) - smallestWeight) / (biggestWeight - smallestWeight);
            } else {
                ratio = 0.75f;
            }
            float stroke = minStroke + ratio * (maxStroke - minStroke);
            g.setStroke(new BasicStroke(stroke));
            g.setColor(con.weight() > 0 ? GREEN_COLOR : RED_COLOR);
            Point source = centers.get(con.source());
            Point target = centers.get(con.target());

            if (con.source() != con.target()) {
                if (con.source() instanceof InnerNode && con.target() instanceof InnerNode) {
                    drawConnectionWithArrow(g, source.x, source.y, target.x, target.y);
                } else {
                    g.drawLine(source.x, source.y, target.x, target.y);
                }
            } else {
                drawCircleToSelf(g, source.x, source.y);
            }
        }
    }

    private void drawConnectionWithArrow(Graphics2D g, int sx, int sy, int tx, int ty) {
        g.drawLine(sx, sy, tx, ty);

        // get point where the tip of the arrow is
        int dx = tx - sx;
        int dy = ty - sy;
        double alpha = Math.atan2(dy, dx); // in radians
        double cx = Math.cos(alpha) * RADIUS * 0.5;
        double cy = Math.sin(alpha) * RADIUS * 0.5;
        double x = tx - cx;
        double y = ty - cy;

        Graphics2D g2 = (Graphics2D) g.create();
        AffineTransform transform = new AffineTransform();
        transform.setToIdentity();
        transform.translate(x, y);
        transform.rotate(alpha);
        double scale = getScale((BasicStroke) g2.getStroke());
        transform.scale(scale, scale);

        Path2D path = new Path2D.Double();
        path.moveTo(0, 0);
        path.lineTo(-5, -5);
        path.lineTo(-5, 5);
        path.closePath();

        g2.setTransform(transform);
        g2.fill(path);

        g2.dispose();
    }

    private static double getScale(BasicStroke stroke) {
        return Math.pow(stroke.getLineWidth() / 1.7, 0.65);
    }

    private void drawCircleToSelf(Graphics2D g, int x, int y) {
        int r = Math.round(RADIUS * 0.6667f);
        int y1 = y - Math.round(RADIUS * 0.85f);
        g.drawOval(x, y1, r, r);
        int y2 = y - RADIUS / 2;

        // triangle arrow
        float scale = (float) getScale((BasicStroke) g.getStroke());
        Path2D path = new Path2D.Double();
        path.moveTo(x + 1, y2);
        path.lineTo(x + 1 + 4 * scale, y2 - 6 * scale);
        path.lineTo(x + 1 - 4 * scale, y2 - 6 * scale);
        path.closePath();

        g.fill(path);
    }

    /**
     * Creates an image that displays all involved nodes and connections.
     */
    public BufferedImage createImage() {
        Connection[] connections = brain.getConnections();
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("Poppins", Font.BOLD, 34));

        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        HashMap<Node, Point> centers = computePointsOfNodes(connections);
        drawConnections(g, connections, centers);
        drawNodes(g, centers);

        g.dispose();
        return img;
    }
}
