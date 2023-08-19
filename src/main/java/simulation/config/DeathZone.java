package simulation.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.text.TextStringBuilder;
import org.jetbrains.annotations.Nullable;
import simulation.Agent;
import simulation.Simulation;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;

import static utility.Utility.changeAlpha;
import static utility.Utility.format;

public class DeathZone extends Selection {
    public static final Color DEATH_ZONE_COLOR = new Color(0x8655ff);

    private final Region[] regions;
    /**
     * Whether to reverse this condition.
     */
    private final boolean reverse;

    protected DeathZone(int begin, int end, @Nullable ObjectNode args, SimulationConfig config) throws ConfigError {
        super(begin, end);
        if (args == null) {
            throw new ConfigError("Death Zone needs arguments");
        }

        JsonNode reverse = args.get("reverse");
        if (reverse == null) {
            this.reverse = false;
        } else {
            if (!reverse.isBoolean()) {
                throw new ConfigError("Reverse must be omitted or a boolean");
            }
            this.reverse = reverse.asBoolean();
        }

        JsonNode node = assertType(args, "regions", JsonNode::isArray);
        regions = new Region[node.size()];
        for (int i = 0; i < regions.length; i++) {
            regions[i] = Region.parse(node.get(i), config, this.reverse);
        }

    }

    @Override
    public SelectionType getType() {
        return SelectionType.DeathZone;
    }

    @Override
    public boolean survives(Agent agent) {
        if (!isActive()) return true;
        for (Region region : regions) {
            if (region.contains(agent.getX() + 0.5f, agent.getY() + 0.5f)) {
                return reverse;
            }
        }
        return !reverse;
    }

    @Override
    public void writeAnimation(TextStringBuilder sb, float timeStart, float timeEnd, int id, int generation) {
        if (!isActive(generation)) return;
        for (Region region : regions) {
            region.writeToAnimation(sb, timeStart, timeEnd, id);
            id += region.numAnimationIDs();
        }
    }

    @Override
    public int numAnimationIDs(int generation) {
        if (!isActive(generation)) return 0;
        int id = 0;
        for (Region region : regions) {
            id += region.numAnimationIDs();
        }
        return id;
    }

    @Override
    public void writeElements(TextStringBuilder elements, int id, int generation) {
        if (!isActive(generation)) return;

        for (Region region : regions) {
            region.writeToElements(elements, id);
            id += region.numAnimationIDs();
        }
    }

    @Override
    public void draw(Graphics2D g, float animationProgress) {
        if (!isActive()) return;
        for (Region region : regions) {
            region.draw(g, animationProgress);
        }
    }

    private static abstract class Region {
        /**
         * This value is the same as {@link DeathZone#reverse}
         */
        final boolean reverse;

        protected Region(boolean reverse) {
            this.reverse = reverse;
        }

        private static ShapeRegion getRectRegion(ObjectNode args, SimulationConfig config, boolean reverse) throws ConfigError {
            int x = getLength(args, "x", 0, config.getBoardWidth());
            int y = getLength(args, "y", 0, config.getBoardHeight());
            int width = getLength(args, "width", 1, config.getBoardWidth());
            int height = getLength(args, "height", 1, config.getBoardHeight());
            return new ShapeRegion(new Rectangle(x, y, width, height), reverse);
        }

        private static ShapeRegion getEllipseRegion(ObjectNode args, SimulationConfig config, boolean reverse) throws ConfigError {
            int cx = getLength(args, "cx", 0, config.getBoardWidth());
            int cy = getLength(args, "cy", 0, config.getBoardHeight());
            float rx;
            float ry;
            JsonNode r_node = args.get("r");
            JsonNode rx_node = args.get("rx");
            JsonNode ry_node = args.get("ry");
            if (r_node != null) {
                if (rx_node != null || ry_node != null) {
                    throw new ConfigError("Either r or rx & ry must be set");
                }

                if (!r_node.isNumber() || r_node.asDouble() <= 0) {
                    throw new ConfigError("r must be a number greater than 0");
                } else {
                    rx = (float) r_node.asDouble();
                    ry = rx;
                }
            } else {
                if (rx_node == null || ry_node == null) {
                    throw new ConfigError("Either r or rx & ry must be set");
                }

                if (!rx_node.isNumber() || rx_node.asDouble() <= 0) {
                    throw new ConfigError("rx must be a number greater than 0");
                }

                if (!ry_node.isNumber() || ry_node.asDouble() <= 0) {
                    throw new ConfigError("ry must be a number greater than 0");
                }

                rx = (float) rx_node.asDouble();
                ry = (float) ry_node.asDouble();
            }

            return new ShapeRegion(new Ellipse2D.Float(cx - rx, cy - ry, rx * 2, ry * 2), reverse);
        }

        static Region parse(JsonNode node, SimulationConfig config, boolean reverse) throws ConfigError {
            if (!node.isObject()) {
                throw new ConfigError("Region args should be an object");
            }
            JsonNode n = assertType((ObjectNode) node, "type", JsonNode::isTextual);
            String type = n.asText();
            return switch (type.toLowerCase()) {
                case "rect" -> getRectRegion((ObjectNode) node, config, reverse);
                case "circle" -> getEllipseRegion((ObjectNode) node, config, reverse);
                default -> throw new ConfigError("Unknown type: " + type);
            };
        }

        protected void addPath(TextStringBuilder elements, String path, int id) {
            if (reverse) {
                int w = Simulation.config().getBoardWidth();
                int h = Simulation.config().getBoardHeight();
                // language=XML
                elements.appendln("""
                        <path d="M0 0h%dv%dH0Z%s" fill-rule="evenodd" fill="%s" style="animation-name:kf%d" />
                        """.formatted(w, h, path, format(DEATH_ZONE_COLOR), id));
            } else {
                // language=XML
                elements.appendln("""
                        <path d="%s" fill="%s" style="animation-name:kf%d" />
                        """.formatted(path, format(DEATH_ZONE_COLOR), id));
            }
        }

        abstract boolean contains(float x, float y);

        /**
         * Writes to the keyframes.
         *
         * @param sb    the StringBuilder which is used for appending to the {@code @keyframes}.
         * @param start the percentage at which to start.
         * @param end   the percentage at which to end.
         * @param id    the first id.
         */
        abstract void writeToAnimation(TextStringBuilder sb, float start, float end, int id);

        /**
         * How many IDs are used for the animation
         */
        abstract int numAnimationIDs();

        /**
         * Write to the elements.
         *
         * @param elements the StringBuilder which is used for appending to the elements.
         * @param id       the first id.
         */
        public abstract void writeToElements(TextStringBuilder elements, int id);

        /**
         * Draw this selection process. Only called when simulation is controlled through
         * {@link visualization.live.LiveSimulationWindow LiveSimulationWindow}
         *
         * @param g                 the Graphics2D object to draw with. Is a clone and is automatically disposed.
         * @param animationProgress the animation progress. Value between 0 and 1 representing
         *                          how far the animation was already animated. E.g.: {@code 52%} -> {@code 0.52f}
         */
        abstract void draw(Graphics2D g, float animationProgress);
    }

    private static final class ShapeRegion extends Region {
        private final Shape shape;

        ShapeRegion(Shape shape, boolean reverse) {
            super(reverse);
            this.shape = shape;
        }

        @Override
        boolean contains(float x, float y) {
            return shape.contains(x, y);
        }

        @Override
        void writeToAnimation(TextStringBuilder sb, float start, float end, int id) {
            sb.append("@keyframes kf")
                    .append(id).append("{")
                    .append("0%{opacity:0}")
                    .append(format(start * 100)).append("%{opacity:0}")
                    .append(format((end + start) * 50f)).append("%{opacity:1}")
                    .append(format(end * 100f)).append("%{opacity:0;visibility:hidden}")
                    .appendln("}");
        }

        @Override
        int numAnimationIDs() {
            return 1;
        }


        private static String rectPath(Rectangle r) {
            return "M%d %dh%dv%dH%1$dV%2$d".formatted(r.x, r.y, r.width, r.height);
        }

        private static String ellipsePath(Ellipse2D.Float e) {
            float rx = e.width / 2f;
            float ry = e.height / 2f;
            return String.format("M%s %sa%s %s 0 1 0 %s 0a%s %s 0 1 0-%s 0Z", format(e.x), format(e.y + ry),
                    format(rx), format(ry), format(rx * 2),
                    format(rx), format(ry), format(rx * 2));
        }

        private String toPath() {
            if (shape instanceof Rectangle rect) {
                return rectPath(rect);
            } else if (shape instanceof Ellipse2D.Float ellipse) {
                return ellipsePath(ellipse);
            } else {
                throw new RuntimeException("Unsupported Shape: " + shape);
            }
        }

        @Override
        public void writeToElements(TextStringBuilder elements, int id) {
            addPath(elements, toPath(), id);
        }

        @Override
        void draw(Graphics2D g, float animationProgress) {
            float transparency = 1f - Math.abs(animationProgress - 0.5f) * 2f;
            g.setColor(changeAlpha(DEATH_ZONE_COLOR, transparency));

            if (reverse) {
                int w = Simulation.config().getBoardWidth();
                int h = Simulation.config().getBoardHeight();
                Area area = new Area(new Rectangle(0, 0, w, h));
                area.subtract(new Area(shape));
                g.fill(area);
            } else {
                g.fill(shape);
            }
        }
    }
}
