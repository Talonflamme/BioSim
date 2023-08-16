package simulation.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DeathZone extends Selection {
    private final Region[] regions;

    protected DeathZone(int begin, int end, @Nullable ObjectNode args) throws ConfigError {
        super(begin, end);
        if (args == null) {
            throw new ConfigError("Death Zone needs arguments");
        }

        JsonNode node = assertType(args, "regions", JsonNode::isArray);
        regions = new Region[node.size()];
        for (int i = 0; i < regions.length; i++) {
            regions[i] = Region.parse(node.get(i));
        }
    }

    @Override
    public SelectionType getType() {
        return SelectionType.DeathZone;
    }

    private static abstract class Region {
        static Region parse(JsonNode node) throws ConfigError {
            if (!node.isObject()) {
                throw new ConfigError("Region args should be an object");
            }
            JsonNode n = assertType((ObjectNode) node, "type", JsonNode::isTextual);
            String type = n.asText();
            return switch (type.toLowerCase()) {
                case "rect" -> new RectRegion((ObjectNode) node);
                default -> throw new ConfigError("Unknown type: " + type);
            };
        }
    }

    private static final class RectRegion extends Region {
        private final int x, y, width, height;

        private RectRegion(ObjectNode args) throws ConfigError {
            x = getInt(args, "x", 0);
            y = getInt(args, "y", 0);
            width = getInt(args, "width", 1);
            height = getInt(args, "height", 1);
        }

        @Override
        public String toString() {
            return "RectRegion[x=%d, y=%d, width=%d, height=%d]".formatted(x, y, width, height);
        }
    }
}
