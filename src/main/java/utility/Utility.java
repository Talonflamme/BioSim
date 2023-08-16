package utility;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Utility {
    private static final String reset = "\u001B[0m";
    public static final ObjectMapper ObjectMapper = new ObjectMapper();
    public static final BetterRandom Rand = new BetterRandom(42);

    public static String red(String str) {
        return "\u001B[31m" + str + reset;
    }

    public static String red(Object obj) {
        return green(String.valueOf(obj));
    }

    public static String green(String str) {
        return "\u001B[32m" + str + reset;
    }

    public static String green(Object obj) {
        return green(String.valueOf(obj));
    }

    public static String yellow(String str) {
        return "\u001B[33m" + str + reset;
    }

    public static String yellow(Object obj) {
        return yellow(String.valueOf(obj));
    }

    public static String blue(String str) {
        return "\u001B[34m" + str + reset;
    }

    public static String blue(Object obj) {
        return blue(String.valueOf(obj));
    }

    private Utility() {
    }
}
