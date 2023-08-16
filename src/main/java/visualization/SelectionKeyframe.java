package visualization;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.TextStringBuilder;
import simulation.config.Selection;

class SelectionKeyframe implements AnimationKeyframe {
    private final float durationSeconds;
    private final Selection[] selections;
    private final int idOffset;

    @Override
    public void writeAnimations(TextStringBuilder animations, float totalDuration, float secondsOffset) {
        float percentOffset = secondsOffset / totalDuration;
        float thisDuration = getDuration();
        for (int i = 0; i < selections.length; i++) {
            Selection s = selections[i];
            int id = idOffset + i;
            s.writeAnimation(animations, percentOffset, percentOffset + thisDuration / totalDuration, id);
        }
    }

    @Override
    public void writeElements(TextStringBuilder elements) {
        for (int i = 0; i < selections.length; i++) {
            Selection s = selections[i];
            int id = idOffset + i;
            s.writeElements(elements, id);
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
            count += s.numAnimationIDs();
        }
        return count;
    }

    /**
     * @param selections      the selections to draw
     * @param durationSeconds to duration this keyframe should last
     * @param idOffset        idOffset that is used to generate a unique identifier. Should be unique.
     */
    SelectionKeyframe(Selection[] selections, float durationSeconds, int idOffset) {
        this.durationSeconds = durationSeconds;
        this.selections = ArrayUtils.clone(selections);
        this.idOffset = idOffset;
    }
}
