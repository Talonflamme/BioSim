package visualization;

import org.apache.commons.text.TextStringBuilder;
import simulation.Agent;
import simulation.Simulation;
import simulation.config.Selection;
import simulation.config.SimulationConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.MatchResult;
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
            keyframes.add(mkf = new MovementKeyframe(getUsedIDs(), config));
        } else {
            mkf = (MovementKeyframe) keyframes.get(keyframes.size() - 1);
        }

        mkf.addFrame(agents);
    }

    void addAlive(Agent[] agents, float fadeAwayTransition) {
        keyframes.add(new AliveKeyframe(agents, getUsedIDs(), config, fadeAwayTransition));
    }

    void addFrame(Selection[] selections, float seconds) {
        int id = getUsedIDs();
        keyframes.add(new SelectionKeyframe(selections, seconds, id, Simulation.activeSimulation.getProgress().getGeneration()));
    }

    private static String getSVG(int width, int height, float seconds, String animations, String circles) {
        int actualWidth = Math.min(width * SCALE, 1500);
        int actualHeight = Math.min(height * SCALE, 750);
        // language=SVG
        return """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" height="%d" fill="none">
                    <defs>
                        <pattern id="_s" width="1" height="1" patternUnits="userSpaceOnUse">
                            <path d="M1 0H0v1" stroke="%s" stroke-width=".5" vector-effect="non-scaling-stroke" />
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
                        * {
                            animation: %ss linear forwards;
                        }
                %s    </style>
                    <path d="M0 0h%1$dv%2$dH0z" fill="%s" stroke="%5$s" vector-effect="non-scaling-stroke" />
                    <path d="M0 0h%1$dv%2$dH0z" fill="url(#_g)" />
                %s</svg>
                """.formatted(width, height, actualWidth, actualHeight,
                format(RECT_OUTLINE_COLOR),
                format(seconds),
                animations.indent(8),
                format(BACKGROUND_COLOR),
                circles.indent(8));
    }


    private static String uniqueIdentifier(String s) {
        return uniqueIdentifier(Integer.parseInt(s));
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

    private static final Pattern uselessWhitespace = Pattern.compile(";[\r\n\\s]*(?=})|([>{};:])\\s+|\r?\n|\\s+(?=[\\s<;>{}@]|$|/>)");
    private static final Pattern compressKeyframeNamesRegex = Pattern.compile("(?<=@keyframes\\s|animation-name:)kf(\\d+)(?:,kf(\\d+))?"); // currently only 2 animations
    private static final Pattern keyframesRegex = Pattern.compile("@keyframes\\s+([A-z0-9_-]+)\\{(?:[\\d.]+%\\{[^}]*})*\\s*}");
    private static final Pattern oneHundredPercentPortion = Pattern.compile("(?:(?<=[{]|\\d%,)100%|(\\d%\\{cx:\\d+;cy:\\d+})100%)(?=\\{cx)");
    private static final Pattern unnecessaryCxCyDefinition = Pattern.compile("(<circle[^/]*)\\s*c[xy]=\"0\"\\s*([^/]*/)");

    private static final Pattern percentagesRegex = Pattern.compile("(?<=[^.\\d])(?=[.\\d]+%\\{)");

    private static String keyframesReplacer(MatchResult match) {
        String whole = match.group();
        String[] percentages = percentagesRegex.split(whole);

        HashMap<String, ArrayList<Float>> counter = new HashMap<>();
        // start at 1: first element is '@keyframes kf'
        for (int i = 1; i < percentages.length; i++) {
            String s = percentages[i];
            float percent = Float.parseFloat(s.substring(0, s.indexOf('{') - 1));
            s = s.substring(s.indexOf('{'));

            if (i == percentages.length - 1) {
                s = s.substring(0, s.length() - 1); // end of @keyframes has "}" at end
            }

            ArrayList<Float> list = counter.computeIfAbsent(s, k -> new ArrayList<>());
            list.add(percent);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(percentages[0]);
        for (var entry : counter.entrySet()) {
            for (float f : entry.getValue()) {
                if (f == 100) {
                    sb.append("to");
                } else if (f == 0) {
                    sb.append("0%");
                } else if (0 == (int) f) {
                    sb.append(format(f).substring(1)).append('%'); // remove 0 of for example '0.12'
                } else {
                    sb.append(format(f)).append('%');
                }
                sb.append(",");
            }
            sb.setLength(sb.length() - 1); // remove trailing ','
            sb.append(entry.getKey());
        }

        sb.append('}');
        return sb.toString();
    }

    /**
     * Compresses the svg to be as small as possible
     */
    private static String compress(String svg) {
        int beforeLength = svg.getBytes().length;
        // white spaces removed
        String str = uselessWhitespace.matcher(svg).replaceAll("$1");

        str = keyframesRegex.matcher(str).replaceAll(BoardAnimation::keyframesReplacer);
        str = oneHundredPercentPortion.matcher(str).replaceAll("$1to");
        str = unnecessaryCxCyDefinition.matcher(str).replaceAll("$1$2");

        Matcher matcher = compressKeyframeNamesRegex.matcher(str);
        str = matcher.replaceAll(matchResult -> {
            String g1 = matchResult.group(1);
            String g2 = matchResult.group(2);
            if (g2 == null) {
                return uniqueIdentifier(g1);
            } else {
                return uniqueIdentifier(g1) + "," + uniqueIdentifier(g2);
            }
        });

        int afterLength = str.getBytes().length;
        Simulation.activeSimulation.getReport().compressed(beforeLength, afterLength);
        return str;
    }

    void save(File file) {
        int width = config.getBoardWidth();
        int height = config.getBoardHeight();

        TextStringBuilder circles = new TextStringBuilder();
        TextStringBuilder animations = new TextStringBuilder();
        final float duration = getDuration();

        float time = 0f;
        MovementKeyframe lastMkf = null;
        for (int i = 0; i < keyframes.size(); i++) {
            AnimationKeyframe kf = keyframes.get(i);
            if (kf instanceof MovementKeyframe mkf) {
                lastMkf = mkf;
            }

            if (kf instanceof AliveKeyframe akf) {
                assert lastMkf != null;
                akf.writeElements(circles, lastMkf);
            } else {
                kf.writeElements(circles);
            }

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

    public void clear() {
        keyframes.clear();
    }
}
