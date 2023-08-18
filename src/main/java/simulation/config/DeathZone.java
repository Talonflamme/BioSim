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
            if (region.contains(agent.getX(), agent.getY())) {
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

        static Region parse(JsonNode node, SimulationConfig config, boolean reverse) throws ConfigError {
            if (!node.isObject()) {
                throw new ConfigError("Region args should be an object");
            }
            JsonNode n = assertType((ObjectNode) node, "type", JsonNode::isTextual);
            String type = n.asText();
            return switch (type.toLowerCase()) {
                case "rect" -> new RectRegion((ObjectNode) node, config, reverse);
                case "circle" -> new CircleRegion((ObjectNode) node, config, reverse);
                default -> throw new ConfigError("Unknown type: " + type);
            };
        }

        protected void addPath(TextStringBuilder elements, String path, int id) {
            if (reverse) {
                int w = Simulation.config().getBoardWidth();
                int h = Simulation.config().getBoardHeight();
                // language=XML
                elements.appendln("""
                        <path d="M0 0h%dv%dh-%1$d Z %s" fill-rule="evenodd" fill="%s" style="animation-name:kf%d" />
                        """.formatted(w, h, path, format(DEATH_ZONE_COLOR), id));
            } else {
                // language=XML
                elements.appendln("""
                        <path d="%s" fill="%s" style="animation-name:kf%d" />
                        """.formatted(path, format(DEATH_ZONE_COLOR), id));
            }
        }

        abstract boolean contains(int x, int y);

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

    private static final class RectRegion extends Region {
        private final int x, y, width, height;

        private RectRegion(ObjectNode args, SimulationConfig config, boolean reverse) throws ConfigError {
            super(reverse);
            x = getLength(args, "x", 0, config.getBoardWidth());
            y = getLength(args, "y", 0, config.getBoardHeight());
            width = getLength(args, "width", 1, config.getBoardWidth());
            height = getLength(args, "height", 1, config.getBoardHeight());
        }

        @Override
        public String toString() {
            return "RectRegion[x=%d, y=%d, width=%d, height=%d]".formatted(x, y, width, height);
        }

        @Override
        boolean contains(int x, int y) {
            return x >= this.x && y >= this.y && x < this.x + this.width && y < this.y + this.height;
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

        private String rectPath() {
            return "M%d %dh%dv%dH%1$dV%2$d".formatted(x, y, width, height);
        }

        @Override
        public void writeToElements(TextStringBuilder elements, int id) {
            addPath(elements, rectPath(), id);
        }

        @Override
        void draw(Graphics2D g, float animationProgress) {
            g.setStroke(new BasicStroke(2));
            float transparency = 1f - Math.abs(animationProgress - 0.5f) * 2f;
            g.setColor(changeAlpha(DEATH_ZONE_COLOR, transparency));
            g.fillRect(x + 1, y + 1, width - 2, height - 2);
        }
    }

    private static final class CircleRegion extends Region {
        private final int cx, cy, r;

        private CircleRegion(ObjectNode args, SimulationConfig config, boolean reverse) throws ConfigError {
            super(reverse);
            cx = getLength(args, "cx", 0, config.getBoardWidth());
            cy = getLength(args, "cy", 0, config.getBoardHeight());
            r = getInt(args, "r", 1);
        }

        @Override
        public String toString() {
            return "CircleRegion[cx=%d, cy=%d, r=%d]".formatted(cx, cy, r);
        }

        @Override
        boolean contains(int x, int y) {
            int dx = cx - x;
            int dy = cy - y;
            return dx * dx + dy * dy <= r * r;
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

        private String circlePath() {
            return "M%d %d m-%d,0 a%3$d,%3$d 0 1,1 %d,0 a%3$d,%3$d 0 1,1 -%4$d,0".formatted(cx, cy, r, r * 2);
        }

        @Override
        public void writeToElements(TextStringBuilder elements, int id) {
            addPath(elements, circlePath(), id);
        }

        @Override
        void draw(Graphics2D g, float animationProgress) {
            g.setStroke(new BasicStroke(2));
            float transparency = 1f - Math.abs(animationProgress - 0.5f) * 2f;
            g.setColor(changeAlpha(DEATH_ZONE_COLOR, transparency));
            Shape circle = new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2);
            if (reverse) {
                int w = Simulation.config().getBoardWidth();
                int h = Simulation.config().getBoardHeight();
                Area area = new Area(new Rectangle(0, 0, w, h));
                area.subtract(new Area(circle));
                g.fill(area);
            } else {
                g.fill(circle);
            }
        }
    }
}
