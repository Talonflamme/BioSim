package visualization;

import simulation.brain.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Comparator;
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
    /**
     * How much the y position of nodes may move up or down. Mainly used in {@link #computePointsOfNodes(Node[], int)}
     */
    private static final int Y_DEVIATION = 20;
    private static final int PADDING_X = 80;
    private static final int PADDING_Y = 138;
    private static final int RADIUS = 90;

    private final Brain brain;
    /**
     * The height used for the image creation of this image.
     * Should be either
     * <li>
     * Above or equal to 0
     * </li>
     * <li>
     * -1 to disable (uses {@link #HEIGHT} instead)
     * </li>
     */
    private final int targetHeight;
    /**
     * The padding that is applied at the bottom of the created image. Should be either
     * <li>
     * Above 0
     * </li>
     * <li>
     * -1 to disable (uses {@link #PADDING_Y} instead)
     * </li>
     */
    private final int paddingBottom;

    public BrainVisualizer(Brain brain) {
        this(brain, -1);
    }

    public BrainVisualizer(Brain brain, int targetHeight) {
        this(brain, targetHeight, -1);
    }

    public BrainVisualizer(Brain brain, int targetHeight, int paddingBottom) {
        this.brain = brain;
        this.targetHeight = targetHeight == -1 ? HEIGHT : targetHeight;
        this.paddingBottom = paddingBottom == -1 ? PADDING_X : paddingBottom;

        if (targetHeight <= 0 && targetHeight != -1) {
            throw new IllegalArgumentException("targetHeight must be greater than 0 or equal to -1.");
        }

        if (paddingBottom < -1) {
            throw new IllegalArgumentException("paddingBottom must greater than or equal to 0 or equal to -1.");
        }
    }

    private static InputNode[] getInputNodes(Connection[] connections) {
        HashSet<InputNode> set = new HashSet<>();
        for (Connection con : connections) {
            if (con.source() instanceof InputNode) {
                set.add((InputNode) con.source());
            }
        }
        // we insert "useless" sorting here in order to make it deterministic
        return set.stream().sorted(Comparator.comparingInt(a -> a.getType().ordinal()))
                .toArray(InputNode[]::new);
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
        // we insert "useless" sorting here in order to make it deterministic
        return set.stream().sorted(Comparator.comparingInt(InnerNode::getId))
                .toArray(InnerNode[]::new);
    }

    private static OutputNode[] getOutputNodes(Connection[] connections) {
        HashSet<OutputNode> set = new HashSet<>();
        for (Connection con : connections) {
            if (con.target() instanceof OutputNode) {
                set.add((OutputNode) con.target());
            }
        }
        // we insert "useless" sorting here in order to make it deterministic
        return set.stream().sorted(Comparator.comparingInt(a -> a.getType().ordinal()))
                .toArray(OutputNode[]::new);
    }

    /**
     * Computes the <b>center</b> position of all nodes (Input, Inner & Output).
     *
     * @return a dictionary containing the nodes mapped to the center of their circle.
     */
    private HashMap<Node, Point> computePointsOfNodes(Connection[] connections) {
        InputNode[] inputs = getInputNodes(connections);
        InnerNode[] innerNodes = getInnerNodes(connections);
        OutputNode[] outputs = getOutputNodes(connections);

        HashMap<Node, Point> map = new HashMap<>();

        int availableSpace = targetHeight - PADDING_Y - paddingBottom - Y_DEVIATION * 2; // * 2 since we only care about top and bottom
        int topY = PADDING_Y + Y_DEVIATION; // y of the topmost nodes
        map.putAll(computePointsOfNodes(inputs, topY));
        map.putAll(computePointsOfNodes(innerNodes, topY + (availableSpace - RADIUS) / 2)); // center y
        map.putAll(computePointsOfNodes(outputs, targetHeight - paddingBottom - RADIUS - Y_DEVIATION));

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

    /**
     * Compute the positions of the provided nodes. These positions typically align in a horizontal line
     * with the y coordinate being the same as the parameter. If there are more than 2 nodes, the y position might
     * differ a little bit (max. 20 pixels). Still is roughly at the horizontal line.
     *
     * @param nodes the nodes of which to compute the position.
     * @param y     the basic y coordinate of the positions. Although the position might slightly alter, the y position
     *              will be roughly equal to this param. Note: y does not mark the center,
     *              but instead the top of the circle
     * @return a HashMap which has the Node as Key and its corresponding position as Value.
     */
    private HashMap<Node, Point> computePointsOfNodes(Node[] nodes, int y) {
        HashMap<Node, Point> map = new HashMap<>();
        if (nodes.length == 0) return map;
        int spaceForEach = (WIDTH - PADDING_X * 2) / nodes.length;
        for (int i = 0; i < nodes.length; i++) {
            int x = PADDING_X + spaceForEach * i + (spaceForEach - RADIUS) / 2;
            int y1 = y;
            if (nodes.length > 2) {
                y1 += (1 - i % 2) * Y_DEVIATION * 2 - Y_DEVIATION;
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
        BufferedImage img = new BufferedImage(WIDTH, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("Poppins", Font.BOLD, 34));

        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, WIDTH, targetHeight);

        HashMap<Node, Point> centers = computePointsOfNodes(connections);
        drawConnections(g, connections, centers);
        drawNodes(g, centers);

        g.dispose();
        return img;
    }
}
