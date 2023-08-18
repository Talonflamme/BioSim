package simulation.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.TextStringBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import simulation.Agent;

import java.awt.*;
import java.util.function.Predicate;

import static utility.Utility.yellow;

public abstract class Selection {
    protected final int begin;
    protected final int end;

    protected Selection(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    static int getInt(ObjectNode node, String propName, int minimum, int... allowed) throws ConfigError {
        JsonNode n = assertType(node, propName, JsonNode::isInt);
        int x = n.asInt();
        if (ArrayUtils.contains(allowed, x)) {
            return x;
        }
        if (x < minimum) {
            throw new ConfigError("Property %s (%d) should at least %d".formatted(propName, x, minimum));
        }
        return x;
    }

    static int getLength(ObjectNode node, String propName, int minimum, int oneHundred) throws ConfigError {
        JsonNode n = assertType(node, propName, x -> x.isTextual() || x.isInt());
        int x;
        if (n.isInt()) {
            x = n.asInt();
        } else {
            String text = n.asText();
            if (!text.matches("^\\d{1,2}%|100%$")) {
                throw new ConfigError("Property %s (%s) is no percentage".formatted(propName, text));
            }
            x = Math.round(Integer.parseInt(text.substring(0, text.length() - 1)) / 100f * oneHundred);
        }
        if (x < minimum) {
            throw new ConfigError("Property %s (%s) less than minimum %d".formatted(propName, n, minimum));
        }
        return x;
    }

    @Contract(value = "_, _, true -> _; _, _, false -> !null", pure = true)
    @Nullable
    static ObjectNode getObj(ObjectNode node, String propName, boolean allowNull) throws ConfigError {
        JsonNode n = assertType(node, propName, x -> true);
        if (n.isNull()) {
            if (allowNull) return null;
            throw new ConfigError("Property %s is null".formatted(propName));
        }
        if (!n.isObject()) {
            throw new ConfigError("Property %s is no object".formatted(propName));
        }
        return (ObjectNode) n;
    }

    static SelectionType getType(ObjectNode node) throws ConfigError {
        JsonNode n = assertType(node, "type", JsonNode::isTextual);
        String s = n.asText();
        try {
            return SelectionType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new ConfigError("Unexpected type: %s".formatted(s));
        }
    }

    static JsonNode assertType(ObjectNode node, String propName, Predicate<JsonNode> predicate) throws ConfigError {
        if (!node.has(propName)) {
            throw new ConfigError("Missing property: " + propName);
        }
        JsonNode n = node.get(propName);
        if (!predicate.test(n)) {
            throw new ConfigError("Property %s (%s) invalid".formatted(propName, n));
        }
        return n;
    }

    public static Selection parse(ObjectNode obj, SimulationConfig config) throws ConfigError {
        int begin = getInt(obj, "begin", 0);
        int end = getInt(obj, "end", begin + 1, -1);
        ObjectNode args = getObj(obj, "args", true);
        return switch (getType(obj)) {
            case DeathZone -> new DeathZone(begin, end, args, config);
        };
    }

    public static Selection[] parse(ObjectNode[] array, SimulationConfig config) throws ConfigError {
        Selection[] result = new Selection[array.length];

        if (result.length == 0) {
            System.out.println(yellow("WARN: Death reasons are empty."));
        }

        for (int i = 0; i < result.length; i++) {
            result[i] = parse(array[i], config);
        }

        return result;
    }

    public abstract SelectionType getType();

    /**
     * Returns whether the agent survives this selection.
     */
    public abstract boolean survives(Agent agent);

    /**
     * Write to the keyframes.
     * @param sb The TextBuilder which is used in the animations section.
     * @param timeStart the percentage at which to start the animation ({@code 62% = 0.68f}).
     * @param timeEnd the percentage at which to stop the animation ({@code 98% = 0.98}).
     * @param id the id used for the {@code @keyframes} identifier.
     */
    public abstract void writeAnimation(TextStringBuilder sb, float timeStart, float timeEnd, int id);

    /**
     * The amount of IDs this selection uses for the animation.
     */
    public abstract int numAnimationIDs();

    /**
     * Write to the elements.
     * @param elements The TextBuilder which is used in the elements section.
     * @param id the id used for the @keyframes identifier.
     */
    public abstract void writeElements(TextStringBuilder elements, int id);

    /**
     * Draw this selection process. Only called when simulation is controlled through
     * {@link visualization.live.LiveSimulationWindow LiveSimulationWindow}
     * @param g the Graphics2D object to draw with. Is a clone and is automatically disposed.
     * @param animationProgress the animation progress. Value between 0 and 1 representing
     *                         how far the animation was already animated. E.g.: {@code 52%} -> {@code 0.52f}
     */
    public abstract void draw(Graphics2D g, float animationProgress);
}
