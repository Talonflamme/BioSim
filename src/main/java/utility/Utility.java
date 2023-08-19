package utility;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Utility {
    private static final String reset = "\u001B[0m";
    public static final ObjectMapper ObjectMapper = new ObjectMapper();
    public static final BetterRandom Rand = new BetterRandom(42);
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    public static Color changeAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    public static Color changeAlpha(Color c, float a) {
        return new Color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, a);
    }

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


    public static String format(float f) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#.##", symbols);
        return df.format(f);
    }

    /**
     * Small optimization for shortening color hex codes.
     * Replaces the following:
     * <p>{@code #ffffff}</p>
     * <p>{@code #aabbccdd}</p>
     * Does not replace the following:
     * <p>{@code #fffeff}</p>
     * <p>{@code #aabbccde}</p>
     */
    private static final Pattern colorFormatRegex = Pattern.compile("(.)\\1(.)\\2(.)\\3(?:(.)\\4)?$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    public static String format(Color c) {
        String color = "#%08x".formatted(c.getRGB());
        if (c.getAlpha() == 255) {
            color = "#" + color.substring(3);
        } else if (c.getAlpha() == 0) {
            return "#0000";
        } else {
            color = "#" + color.substring(3) + color.substring(1, 3);
        }
        Matcher matcher = colorFormatRegex.matcher(color);
        return optimizeColor(matcher.replaceAll("$1$2$3$4"));
    }


    private static String optimizeColor(String hex) {
        return switch (hex) {
            case "#f5f5dc" -> "beige";
            case "#ffe4c4" -> "bisque";
            case "#a52a2a" -> "brown";
            case "#ff7f50" -> "coral";
            case "#ffd700" -> "gold";
            case "#808080" -> "gray";
            case "#008000" -> "green";
            case "#4b0082" -> "indigo";
            case "#fffff0" -> "ivory";
            case "#f0e68c" -> "khaki";
            case "#faf0e6" -> "linen";
            case "#800000" -> "maroon";
            case "#000080" -> "navy";
            case "#808000" -> "olive";
            case "#ffa500" -> "orange";
            case "#da70d6" -> "orchid";
            case "#cd853f" -> "peru";
            case "#ffc0cb" -> "pink";
            case "#dda0dd" -> "plum";
            case "#800080" -> "purple";
            case "#f00" -> "red";
            case "#fa8072" -> "salmon";
            case "#a0522d" -> "sienna";
            case "#c0c0c0" -> "silver";
            case "#fffafa" -> "snow";
            case "#d2b48c" -> "tan";
            case "#008080" -> "teal";
            case "#ff6347" -> "tomato";
            case "#ee82ee" -> "violet";
            case "#f5deb3" -> "wheat";
            default -> hex;
        };
    }

    private Utility() {
    }
}
