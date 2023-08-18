package visualization;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.TextStringBuilder;
import simulation.config.Selection;

class SelectionKeyframe implements AnimationKeyframe {
    private final float durationSeconds;
    private final Selection[] selections;
    private final int idOffset;
    private final int generation;

    @Override
    public void writeAnimations(TextStringBuilder animations, float totalDuration, float secondsOffset) {
        float percentOffset = secondsOffset / totalDuration;
        float thisDuration = getDuration();
        int id = idOffset;
        for (Selection s : selections) {
            s.writeAnimation(animations, percentOffset, percentOffset + thisDuration / totalDuration, id, generation);
            if (s.isActive(generation)) {
                id++;
            }
        }
    }

    @Override
    public void writeElements(TextStringBuilder elements) {
        int id = idOffset;
        for (Selection s : selections) {
            s.writeElements(elements, id, generation);
            if (s.isActive(generation)) {
                id++;
            }
        }
    }

    @Override
    public float getDuration() {
        return durationSeconds;
    }

    @Override
    public int numIDs() {
        int count = 0;
        for (Selection s : selections) {
            count += s.numAnimationIDs(generation);
        }
        return count;
    }

    /**
     * @param selections      the selections to draw
     * @param durationSeconds to duration this keyframe should last
     * @param idOffset        idOffset that is used to generate a unique identifier. Should be unique.
     * @param generation      the generation of this keyframe
     */
    SelectionKeyframe(Selection[] selections, float durationSeconds, int idOffset, int generation) {
        this.durationSeconds = durationSeconds;
        this.selections = ArrayUtils.clone(selections);
        this.idOffset = idOffset;
        this.generation = generation;
    }
}
