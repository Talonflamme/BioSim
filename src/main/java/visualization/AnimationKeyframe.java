package visualization;

import org.apache.commons.text.TextStringBuilder;

public interface AnimationKeyframe {

    /**
     * Adds this keyframe to the svg.
     * @param animations the part where to define keyframes.
     * @param totalDuration the total duration of the svg in seconds.
     * @param secondsOffset the amount of time that has already passed. If for example, the 1st keyframe
     *                   took 2 seconds to animate, the next took 1.5 seconds, then for the 3rd keyframe
     *                   {@code secondsOffset} will be equal to {@code 3.5}.
     */
    void writeAnimations(TextStringBuilder animations, float totalDuration, float secondsOffset);
    void writeElements(TextStringBuilder elements);

    /**
     * Return the amount of seconds this keyframe lasts.
     */
    float getDuration();

    /**
     * The amount of IDs this keyframe uses.
     */
    int numIDs();
}
