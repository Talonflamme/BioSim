package visualization;

import org.apache.commons.text.TextStringBuilder;
import simulation.Agent;
import simulation.Direction;

import java.util.ArrayList;
import java.util.Hashtable;

import static visualization.BoardAnimation.format;

class MovementKeyframe implements AnimationKeyframe {
    MovementKeyframe() {
        keyframes = new Hashtable<>();
    }

    private final Hashtable<Agent, ArrayList<Movement>> keyframes;

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

    public void addCircles(TextStringBuilder circles) {
        keyframes.forEach((agent, movements) -> {
            Movement first = movements.get(0);
            boolean hasAnimation = movements.size() > 1 || first.didMove();
            circles.append("""
                    <circle cx="%d" cy="%d" fill="%s"%s />
                    """, first.fromX, first.fromY, format(agent.getBrain().getUniqueColor()), hasAnimation ? " style=\"animation-name: kf" + agent.getID() + '"' : "");
        });
    }

    @Override
    public void write(TextStringBuilder animations, float totalDuration) {
        keyframes.forEach((agent, movements) -> {
            Movement first = movements.get(0);
            boolean hasAnimation = movements.size() > 1 || first.didMove();

            if (!hasAnimation) return;

            animations.append("@keyframes kf")
                    .append(agent.getID())
                    .appendln(" {");
            addToKeyframes(animations, 0, first.fromX, first.fromY);

            int currentTime = 0;
            for (Movement movement : movements) {
                currentTime += movement.duration;
                float durPercentage = currentTime / (float) getDuration();
                addToKeyframes(animations, durPercentage, movement.toX, movement.toY);
            }
            assert currentTime == getDuration();
            animations.appendln('}');
        });
    }

    private static void addToKeyframes(TextStringBuilder sb, float durPercentage, int x, int y) {
        sb.appendPadding(4, ' ')
                .append(format(durPercentage * 100, 2)).appendln("%{")
                .append("cx:").append(x).append(";")
                .append("cy:").append(y)
                .append('}');
    }

    int getDuration() {
        return 300; // todo
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
