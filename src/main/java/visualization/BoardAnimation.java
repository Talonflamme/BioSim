package visualization;

import org.apache.commons.text.TextStringBuilder;
import simulation.Agent;
import simulation.Direction;
import simulation.config.SimulationConfig;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static utility.Utility.*;
import static visualization.Board.*;

class BoardAnimation {
    private final SimulationConfig config;
    private final Hashtable<Agent, ArrayList<Movement>> movementTable;

    BoardAnimation(SimulationConfig config) {
        this.config = config;
        this.movementTable = new Hashtable<>();
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

    private static String format(float f, int maxDecimals) {
        if (maxDecimals <= 0) return String.valueOf(Math.round(f));
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#." + "#".repeat(maxDecimals), symbols);
        return df.format(f);
    }

    private static String format(Color c) {
        String color = "#%08x".formatted(c.getRGB());
        if (c.getAlpha() == 255) {
            color = "#" + color.substring(3);
        } else if (c.getAlpha() == 0) {
            return "#0000";
        } else {
            color = "#" + color.substring(3) + color.substring(1, 3);
        }
        Matcher matcher = colorFormatRegex.matcher(color);
        return matcher.replaceAll("$2$3$4$1");
    }

    void addFrame(Agent[] agents) {
        for (Agent agent : agents) {
            int x = agent.getX();
            int y = agent.getY();
            int lx = agent.getLastX();
            int ly = agent.getLastY();

            ArrayList<Movement> movements;
            if (movementTable.containsKey(agent)) {
                movements = movementTable.get(agent);
            } else {
                movements = new ArrayList<>();
                movementTable.put(agent, movements);
            }

            Movement last = movements.isEmpty() ? null : movements.get(movements.size() - 1);
            boolean addToLast = last != null && last.accept(lx, ly, x, y);
            if (addToLast) {
                last.add(x, y);
            } else {
                movements.add(new Movement(lx, ly, x, y));
            }
        }
    }

    private static void addToKeyframes(TextStringBuilder sb, float durPercentage, int x, int y) {
        sb.appendPadding(4, ' ')
                .append(format(durPercentage * 100, 2)).appendln("% {")
                .appendPadding(8, ' ').append("cx: ").append(x).appendln(";")
                .appendPadding(8, ' ').append("cy: ").append(y).appendln(";")
                .appendPadding(4, ' ').appendln('}');
    }

    private static float convertDurationToSeconds(float x) {
        if (x <= 65) {
            return x * (float) Math.exp(-x / 81.25);
        }
        return 0.1f * (float) Math.pow(x - 65, 0.85) + 29.21f;
//        return x / 30f;
    }

    private static String getSVG(int width, int height, int duration, String animations, String circles) {
        int actualWidth = Math.min(width * SCALE, 1500);
        int actualHeight = Math.min(height * SCALE, 750);
        float seconds = convertDurationToSeconds(duration);
        // language=SVG
        return """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" height="%d" fill="none">
                    <defs>
                        <pattern id="_s" width="1" height="1" patternUnits="userSpaceOnUse">
                            <path d="M1 0H0v1" stroke="%s" stroke-width="0.5" vector-effect="non-scaling-stroke" />
                        </pattern>
                        <pattern id="_g" width="10" height="10" patternUnits="userSpaceOnUse">
                            <path d="M0 0h10v10H0z" fill="url(#_s)" />
                            <path d="M10 0H0v10" stroke="%5$s" vector-effect="non-scaling-stroke" />
                        </pattern>
                    </defs>
                    <style>
                        circle {
                            r: 0.5;
                            animation: %ss linear forwards;
                        }
                %s    </style>
                    <path d="M0 0h%1$dv%2$dH0z" fill="%s" stroke="%5$s" vector-effect="non-scaling-stroke" />
                    <path d="M0 0h%1$dv%2$dH0z" fill="url(#_g)" />
                    <g transform="translate(0.5 0.5)">
                %s    </g>
                </svg>
                """.formatted(width, height, actualWidth, actualHeight,
                format(RECT_OUTLINE_COLOR),
                format(seconds, 2),
                animations.indent(8),
                format(BACKGROUND_COLOR),
                circles.indent(8));
    }


    private static String uniqueIdentifier(int id) {
        String letters0 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String letters1 = "0123456789_-";

        if (id == 0) return "kf" + letters0.charAt(0);

        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            int len = letters0.length() + (sb.isEmpty() ? 0 : letters1.length());
            int mod = id % len;
            id /= len;
            sb.append(mod >= letters0.length() ? letters1.charAt(mod - letters0.length()) : letters0.charAt(mod));
        }
        return sb.toString();
    }

    private static String bytesToString(int bytes) {
        if (bytes >= 1e6) {
            return "%sMB".formatted(Math.round(bytes / 1e5) / 10f);
        } else if (bytes >= 1e3) {
            return "%sKiB".formatted(Math.round(bytes / 1e2) / 10f);
        }
        return bytes + "B";
    }

    private static final Pattern uselessWhitespace = Pattern.compile(";[\r\n\\s]*(?=})|([>{};:])\\s+|\r?\n|\\s+(?=[\\s<;>{}@]|$|/>)|(?<=^|\\D)0(?=\\.)");
    private static final Pattern compressKeyframeNamesRegex = Pattern.compile("(?<=@keyframes\\s|animation-name:)kf(\\d+)");
    private static final Pattern duplicateKeyFramesPortions = Pattern.compile("([\\d.]+%)\\{([^}]+)}([\\d.]+%)\\{\\2");
    private static final Pattern oneHundredPercentPortion = Pattern.compile("(?:(?<=[{]|\\d%,)100%|(\\d%\\{cx:\\d+;cy:\\d+})100%)(?=\\{cx)");
    private static final Pattern unnecessaryCxCyDefinition = Pattern.compile("(<circle[^/]*)\\s*c[xy]=\"0\"\\s*([^/]*/)");

    /**
     * Compresses the svg to be as small as possible
     */
    private static String compress(String svg) {
        int beforeLength = svg.getBytes().length;
        // white spaces removed
        String str = uselessWhitespace.matcher(svg).replaceAll("$1");

        str = duplicateKeyFramesPortions.matcher(str).replaceAll("$1,$3{$2");
        str = oneHundredPercentPortion.matcher(str).replaceAll("$1to");
        str = unnecessaryCxCyDefinition.matcher(str).replaceAll("$1$2");

        Matcher matcher = compressKeyframeNamesRegex.matcher(str);
        str = matcher.replaceAll(matchResult -> uniqueIdentifier(Integer.parseInt(matchResult.group(1))));

        int afterLength = str.getBytes().length;

        System.out.printf("compressed: %s - %s = %s%n",
                blue(bytesToString(beforeLength)),
                yellow(Math.round(1000 - (float) afterLength / beforeLength * 1000) / 10f + "%"),
                green(bytesToString(afterLength)));
        return str;
    }

    void save(File file) {
        int width = config.getBoardWidth();
        int height = config.getBoardHeight();

        TextStringBuilder circles = new TextStringBuilder();
        TextStringBuilder animations = new TextStringBuilder();
        final int duration = config.getStepsPerGen(); // in seconds

        movementTable.forEach((agent, movements) -> {
            Movement first = movements.get(0);
            boolean hasAnimation = movements.size() > 1 || first.didMove();

            circles.append("""
                    <circle cx="%d" cy="%d" fill="%s"%s />
                    """, first.fromX, first.fromY, format(agent.getBrain().getUniqueColor()), hasAnimation ? " style=\"animation-name: kf" + agent.getID() + '"' : "");

            if (!hasAnimation) return;

            animations.append("@keyframes kf")
                    .append(agent.getID())
                    .appendln(" {");
            addToKeyframes(animations, 0, first.fromX, first.fromY);

            int currentTime = 0;
            for (Movement movement : movements) {
                currentTime += movement.duration;
                float durPercentage = currentTime / (float) duration;
                addToKeyframes(animations, durPercentage, movement.toX, movement.toY);
            }
            assert currentTime == duration;
            animations.appendln('}');
        });

        String svg = getSVG(width, height, duration, animations.toString(), circles.toString());
        svg = compress(svg);
        writeToFile(svg, file);
    }

    private static void writeToFile(String content, File file) {
        try (var fileOutput = new FileOutputStream(file)) {
            fileOutput.write(content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void save(String path) {
        save(new File(path));
    }

    static class Movement {
        int fromX, toX;
        int fromY, toY;
        int duration;

        Movement(int fromX, int fromY, int toX, int toY) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.duration = 1;
        }

        Direction direction() {
            return Direction.get(toX - fromX, toY - fromY);
        }

        boolean accept(int oldX, int oldY, int newX, int newY) {
            return Direction.get(newX - oldX, newY - oldY) == direction();
        }

        void add(int newX, int newY) {
            this.toX = newX;
            this.toY = newY;
            this.duration++;
        }

        boolean didMove() {
            return this.fromX != this.toX || this.fromY != this.toY;
        }

        @Override
        public String toString() {
            return "Movement[%d -> %d, %d -> %d, duration=%d]".formatted(fromX, toX, fromY, toY, duration);
        }
    }
}
