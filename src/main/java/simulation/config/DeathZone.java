package simulation.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.Nullable;
import simulation.Agent;

public class DeathZone extends Selection {
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
    }
}
