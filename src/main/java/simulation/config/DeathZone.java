package simulation.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.text.TextStringBuilder;
import org.jetbrains.annotations.Nullable;
import simulation.Agent;

import java.awt.*;

import static utility.Utility.*;

public class DeathZone extends Selection {
    public static final Color DEATH_ZONE_COLOR = new Color(0x8655ff);

    private final Region[] regions;

    protected DeathZone(int begin, int end, @Nullable ObjectNode args, SimulationConfig config) throws ConfigError {
        super(begin, end);
        if (args == null) {
            throw new ConfigError("Death Zone needs arguments");
        }

        JsonNode node = assertType(args, "regions", JsonNode::isArray);
        regions = new Region[node.size()];
        for (int i = 0; i < regions.length; i++) {
            regions[i] = Region.parse(node.get(i), config);
        }
    }

    @Override
    public SelectionType getType() {
        return SelectionType.DeathZone;
    }

    @Override
    public boolean survives(Agent agent) {
        for (Region region : regions) {
            if (region.contains(agent.getX(), agent.getY())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void writeAnimation(TextStringBuilder sb, float timeStart, float timeEnd, int id) {
        for (Region region : regions) {
            region.writeToAnimation(sb, timeStart, timeEnd, id);
            id += region.numAnimationIDs();
        }
    }

    @Override
    public int numAnimationIDs() {
        int id = 0;
        for (Region region : regions) {
            id += region.numAnimationIDs();
        }
        return id;
    }

    @Override
    public void writeElements(TextStringBuilder elements, int id) {
        for (Region region : regions) {
            region.writeToElements(elements, id);
            id += region.numAnimationIDs();
        }
    }

    @Override
    public void draw(Graphics2D g, float animationProgress) {
        for (Region region : regions) {
            region.draw(g, animationProgress);
        }
    }

    private static abstract class Region {
        static Region parse(JsonNode node, SimulationConfig config) throws ConfigError {
            if (!node.isObject()) {
                throw new ConfigError("Region args should be an object");
            }
            JsonNode n = assertType((ObjectNode) node, "type", JsonNode::isTextual);
            String type = n.asText();
            return switch (type.toLowerCase()) {
                case "rect" -> new RectRegion((ObjectNode) node, config);
                default -> throw new ConfigError("Unknown type: " + type);
            };
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

        private RectRegion(ObjectNode args, SimulationConfig config) throws ConfigError {
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

        @Override
        public void writeToElements(TextStringBuilder elements, int id) {
            // language=XML
            elements.appendln("""
                    <path d="M%d %dh%dv%dH%1$dV%2$d" stroke="%s" stroke-width="2" style="animation-name:kf%d" />
                    """.formatted(x + 1, y + 1, width - 2, height - 2, format(DEATH_ZONE_COLOR), id));
        }

        @Override
        void draw(Graphics2D g, float animationProgress) {
            g.setStroke(new BasicStroke(2));
            float transparency = 1f - Math.abs(animationProgress - 0.5f) * 2f;
            g.setColor(changeAlpha(DEATH_ZONE_COLOR, transparency));
            g.drawRect(x + 1, y + 1, width - 2, height - 2);
        }
    }
}
