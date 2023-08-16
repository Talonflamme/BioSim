package visualization;

import org.apache.commons.text.TextStringBuilder;
import simulation.Agent;
import simulation.Direction;

import java.util.ArrayList;
import java.util.Hashtable;

import static visualization.BoardAnimation.convertDurationToSeconds;
import static visualization.BoardAnimation.format;

class MovementKeyframe implements AnimationKeyframe {
    private final Hashtable<Agent, ArrayList<Movement>> keyframes;
    private final int idOffset;

    MovementKeyframe(int idOffset) {
        keyframes = new Hashtable<>();
        this.idOffset = idOffset;
    }

    public void addFrame(Agent[] agents) {
        for (Agent agent : agents) {
            int x = agent.getX();
            int y = agent.getY();
            int lx = agent.getLastX();
            int ly = agent.getLastY();

            ArrayList<Movement> movements;
            if (keyframes.containsKey(agent)) {
                movements = keyframes.get(agent);
            } else {
                movements = new ArrayList<>();
                keyframes.put(agent, movements);
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

    @Override
    public void writeElements(TextStringBuilder circles) {
        keyframes.forEach((agent, movements) -> {
            Movement first = movements.get(0);
            boolean hasAnimation = movements.size() > 1 || first.didMove();
            int id = idOffset + agent.getID();
            circles.append("""
                    <circle cx="%d" cy="%d" fill="%s"%s />
                    """, first.fromX, first.fromY, format(agent.getBrain().getUniqueColor()), hasAnimation ? " style=\"animation-name: kf" + id + '"' : "");
        });
    }

    /**
     * @param stretchFor the seconds to stretch the last frame.
     */
    public void writeAnimations(TextStringBuilder animations, float totalDuration, float secondsOffset, float stretchFor) {
        float thisDuration = getDuration();
        keyframes.forEach((agent, movements) -> {
            Movement first = movements.get(0);
            boolean hasAnimation = movements.size() > 1 || first.didMove();

            if (!hasAnimation) return;

            animations.append("@keyframes kf")
                    .append(idOffset + agent.getID())
                    .appendln(" {");
            addToKeyframes(animations, 0, first.fromX, first.fromY);

            int currentTime = 0;
            for (Movement movement : movements) {
                currentTime += movement.duration;
                float durPercentage = (currentTime * thisDuration + getSteps() * secondsOffset)
                                      / (getSteps() * totalDuration);
                addToKeyframes(animations, durPercentage, movement.toX, movement.toY);
            }

            if (stretchFor > 0 && !movements.isEmpty()) {
                Movement last = movements.get(movements.size() - 1);
                float time = (getDuration() + stretchFor) / totalDuration;
                addToKeyframes(animations, time, last.toX, last.toY);
            }

            assert currentTime == getSteps();
            animations.appendln('}');
        });
    }

    @Override
    public void writeAnimations(TextStringBuilder animations, float totalDuration, float secondsOffset) {
        writeAnimations(animations, totalDuration, secondsOffset, 0);
    }

    private static void addToKeyframes(TextStringBuilder sb, float durPercentage, int x, int y) {
        sb.append(format(durPercentage * 100, 2)).append("%{")
                .append("cx:").append(x).append(";")
                .append("cy:").append(y)
                .appendln('}');
    }

    int getSteps() {
        ArrayList<Movement> moves = keyframes.get(keyframes.keys().nextElement());
        int dur = 0;
        for (Movement m : moves) {
            dur += m.duration;
        }
        return dur;
    }

    @Override
    public float getDuration() {
        return convertDurationToSeconds(getSteps());
    }

    @Override
    public int numIDs() {
        return keyframes.size();
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
