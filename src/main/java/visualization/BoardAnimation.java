package visualization;

import org.apache.commons.text.TextStringBuilder;
import simulation.Agent;
import simulation.config.Selection;
import simulation.config.SimulationConfig;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static utility.Utility.*;
import static visualization.Board.*;

public class BoardAnimation {
    private final SimulationConfig config;
    private final ArrayList<AnimationKeyframe> keyframes;

    BoardAnimation(SimulationConfig config) {
        this.config = config;
        this.keyframes = new ArrayList<>();
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

    public static String format(float f, int maxDecimals) {
        if (maxDecimals <= 0) return String.valueOf(Math.round(f));
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#." + "#".repeat(maxDecimals), symbols);
        return df.format(f);
    }

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
        return matcher.replaceAll("$2$3$4$1");
    }

    private int getUsedIDs() {
        int id = 0;
        for (AnimationKeyframe kf : keyframes) {
            id += kf.numIDs();
        }
        return id;
    }

    void addFrame(Agent[] agents) {
        MovementKeyframe mkf;
        if (keyframes.isEmpty() || !(keyframes.get(keyframes.size() - 1) instanceof MovementKeyframe)) {
            keyframes.add(mkf = new MovementKeyframe(getUsedIDs()));
        } else {
            mkf = (MovementKeyframe) keyframes.get(keyframes.size() - 1);
        }

        mkf.addFrame(agents);
    }

    void addFrame(Selection[] selections, float seconds) {
        int id = getUsedIDs();
        keyframes.add(new SelectionKeyframe(selections, seconds, id));
    }

    static float convertDurationToSeconds(float x) {
        if (x <= 65) {
            return x * (float) Math.exp(-x / 81.25);
        }
        return 0.1f * (float) Math.pow(x - 65, 0.85) + 29.21f;
    }

    private static String getSVG(int width, int height, float seconds, String animations, String circles) {
        int actualWidth = Math.min(width * SCALE, 1500);
        int actualHeight = Math.min(height * SCALE, 750);
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
                        }
                        g > * {
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
        final float duration = getDuration();

        float time = 0f;
        for (int i = 0; i < keyframes.size(); i++) {
            AnimationKeyframe kf = keyframes.get(i);
            kf.writeElements(circles);
            if (kf instanceof MovementKeyframe mkf) {
                float stretch = 0;

                for (int j = i + 1; j < keyframes.size(); j++) {
                    if (keyframes.get(j) instanceof MovementKeyframe) break;
                    stretch += keyframes.get(j).getDuration();
                }

                mkf.writeAnimations(animations, duration, time, stretch);
            } else {
                kf.writeAnimations(animations, duration, time);
            }
            time += kf.getDuration();
        }
        assert Math.abs(duration - time) < 0.0001;

        String svg = getSVG(width, height, duration, animations.toString(), circles.toString());
        svg = compress(svg);
        writeToFile(svg, file);
    }

    private float getDuration() {
        float dur = 0f;
        for (AnimationKeyframe kf : keyframes) {
            dur += kf.getDuration();
        }
        return dur;
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
}
