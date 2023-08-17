package visualization;

import org.apache.commons.text.TextStringBuilder;
import simulation.Agent;
import simulation.config.SimulationConfig;

import java.awt.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static utility.Utility.*;

class AliveKeyframe implements AnimationKeyframe {
    public static final Color ALIVE_COLOR = Color.GREEN;
    public static final Color DEAD_COLOR = Color.RED;

    private final int idOffset;
    private final Hashtable<Agent, Boolean> alive;
    private final float transitionDuration;

    /**
     *
     * @param idOffset the offset of IDs which will be used for creating animation identifiers.
     * @param transitionDuration the transition in seconds of the agents fading away.
     */
    AliveKeyframe(Agent[] agents, int idOffset, SimulationConfig config, float transitionDuration) {
        this.idOffset = idOffset;
        this.transitionDuration = transitionDuration;

        this.alive = new Hashtable<>(agents.length);
        for (Agent agent : agents) {
            this.alive.put(agent, agent.isAlive());

            if (!config.isUniqueColors()) {
                agent.getBrain().setColor(agent.isAlive() ? ALIVE_COLOR : DEAD_COLOR);
            }
        }
    }

    @Override
    public void writeAnimations(TextStringBuilder animations, float totalDuration, float secondsOffset) {
        float per = secondsOffset / totalDuration * 100;
        animations.append("@keyframes kf").append(idOffset).append("{")
                .append(format((secondsOffset - transitionDuration) / totalDuration * 100)).append("%{opacity:1}")
                .append(format(per)).append("%{opacity:0}")
                .appendln("}");
    }

    /**
     * Searches for the agent which started at the specified location and returns whether it is alive.
     */
    private boolean getAliveForStartingPos(int x, int y, Set<Map.Entry<Agent, ArrayList<MovementKeyframe.Movement>>> entries) {
        for (var entry : entries) {
            Agent key = entry.getKey();
            var value = entry.getValue();
            var firstMove = value.get(0);
            if (firstMove.fromX == x && firstMove.fromY == y) {
                entries.remove(entry);
                return alive.get(key);
            }
        }
        throw new RuntimeException("Position not found (%d, %d)".formatted(x, y));
    }

    public void writeElements(TextStringBuilder elements, MovementKeyframe mkf) {
        final Pattern pattern = Pattern.compile("(<circle\\scx=\"(\\d+)\"\\scy=\"(\\d+)\"\\sfill=\"#[A-Fa-f0-9]+\")(\\sstyle=\"animation-name: (kf\\d+)\")?\\s/>");
        Matcher matcher = pattern.matcher(elements);
        var entries = mkf.keyframes.entrySet();

        int offset = 0;
        // we need to compute all matches before inserting anything into the StringBuilder
        for (var match : matcher.results().toArray(MatchResult[]::new)) {
            int x = Integer.parseInt(match.group(2));
            int y = Integer.parseInt(match.group(3));

            if (getAliveForStartingPos(x, y, entries)) continue;

            String anim = match.group(4); // everything inside " style=..."

            String s;
            int insertAt;
            if (anim == null) {
                s = " style=\"animation-name: kf%d\" ".formatted(idOffset);
                insertAt = match.end(1);
            } else {
                s = ",kf%d ".formatted(idOffset);
                insertAt = match.end(5);
            }
            elements.insert(insertAt + offset, s);
            offset += s.length();
        }
    }

    /**
     * @deprecated use {@link #writeElements(TextStringBuilder, MovementKeyframe)} instead.
     */
    @Override
    @Deprecated
    public void writeElements(TextStringBuilder elements) {
        throw new RuntimeException("Illegal method call, call writeElements(elements, mkf) instead");
    }

    @Override
    public float getDuration() {
        return 0;
    }

    @Override
    public int numIDs() {
        return 1;
    }
}
