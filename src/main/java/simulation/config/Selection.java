package simulation.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

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

    public static Selection parse(ObjectNode obj) throws ConfigError {
        int begin = getInt(obj, "begin", 0);
        int end = getInt(obj, "end", begin + 1, -1);
        ObjectNode args = getObj(obj, "args", true);
        return switch (getType(obj)) {
            case DeathZone -> new DeathZone(begin, end, args);
        };
    }

    public static Selection[] parse(ObjectNode[] array) throws ConfigError {
        Selection[] result = new Selection[array.length];

        if (result.length == 0) {
            System.out.println(yellow("WARN: Death reasons are empty."));
        }

        for (int i = 0; i < result.length; i++) {
            result[i] = parse(array[i]);
        }

        return result;
    }

    public abstract SelectionType getType();
}
