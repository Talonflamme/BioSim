package visualization;

import org.apache.commons.text.TextStringBuilder;

public interface AnimationKeyframe {

    /**
     * Adds this keyframe to the svg.
     * @param animations the part where to define keyframes.
     * @param totalDuration the total duration of the svg in seconds.
     */
    void write(TextStringBuilder animations, float totalDuration);
}
