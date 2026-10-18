package com.litehed.hytalemodels.blockymodel.animations;

import com.litehed.hytalemodels.api.NodeTransform;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockyAnimationPlayer {

    private static final float POSITION_SCALE = 1f / 32f;

    private final BlockyAnimationDefinition definition;
    private final Map<String, PositionInterpolator> positionInterpolators = new HashMap<>();
    private final Map<String, OrientationInterpolator> orientationInterpolators = new HashMap<>();
    private final Map<String, ScaleInterpolator> scaleInterpolators = new HashMap<>();
    private final Map<String, VisibilityInterpolator> visibilityInterpolators = new HashMap<>();

    public BlockyAnimationPlayer(BlockyAnimationDefinition definition) {
        this.definition = definition;
        initializeInterpolators();
    }

    public float getAnimationDuration() {
        return definition.getDuration();
    }

    public boolean isHoldLastKeyframe() {
        return definition.isHoldLastKeyframe();
    }

    /**
     * Resolves the input time in ticks to a valid time within the animation's duration
     *
     * @param timeInTicks The input time in ticks
     * @return A time value that is either clamped to the animation's duration (if holdLastKeyframe is true) else wrapped
     */
    private float resolveTime(float timeInTicks) {
        float duration = definition.getDuration();
        return definition.isHoldLastKeyframe()
                ? Math.min(timeInTicks, duration)
                : timeInTicks % duration;
    }

    /**
     * Merges a new NodeTransform into the map for a given nodeId
     *
     * @param map      The map to merge into
     * @param nodeId   The ID of the node whose transform is being merged
     * @param incoming The new NodeTransform to merge
     */
    private static void mergeIntoMap(Map<String, NodeTransform> map,
                                     String nodeId,
                                     NodeTransform incoming) {
        map.merge(nodeId, incoming, NodeTransform::merge);
    }

    /**
     * Calculates the current transforms for all nodes based on the input time in ticks
     *
     * @param timeInTicks The current time in ticks for which to calculate the transforms
     * @return A map of node IDs to their corresponding NodeTransforms at the given time
     */
    public Map<String, NodeTransform> calculateTransforms(float timeInTicks) {
        float t = resolveTime(timeInTicks);
        float duration = definition.getDuration();
        // A looping animation interpolates from its last keyframe back round to its first
        boolean cyclic = !definition.isHoldLastKeyframe();
        Map<String, NodeTransform> transforms = new HashMap<>();

        positionInterpolators.forEach((nodeId, interp) -> {
            Vector3f pos = interp.interpolate(t, duration, cyclic);
            if (pos != null) {
                Vector3f scaled = pos.mul(POSITION_SCALE, new Vector3f());
                mergeIntoMap(transforms, nodeId, NodeTransform.translation(scaled));
            }
        });

        orientationInterpolators.forEach((nodeId, interp) -> {
            Quaternionf rot = interp.interpolate(t, duration, cyclic);
            if (rot != null) {
                mergeIntoMap(transforms, nodeId, NodeTransform.rotation(rot));
            }
        });

        scaleInterpolators.forEach((nodeId, interp) -> {
            Vector3f scale = interp.interpolate(t, duration, cyclic);
            if (scale != null) {
                mergeIntoMap(transforms, nodeId, NodeTransform.scale(scale));
            }
        });

        visibilityInterpolators.forEach((nodeId, interp) -> {
            boolean visible = interp.isVisible(t);
            mergeIntoMap(transforms, nodeId, NodeTransform.visibility(visible));
        });

        return transforms;
    }

    /**
     * Initializes the interpolators for each node and track type based on the animation definition
     */
    private void initializeInterpolators() {
        for (Map.Entry<String, List<NodeAnimationTrack>> entry : definition.getNodeAnimations().entrySet()) {
            String nodeId = entry.getKey();
            for (NodeAnimationTrack track : entry.getValue()) {
                switch (track.getTrackType()) {
                    case POSITION -> {
                        List<BlockyKeyframe.PositionKeyframe> kfs = castKeyframes(
                                track.getKeyframes(), BlockyKeyframe.PositionKeyframe.class);
                        if (!kfs.isEmpty()) positionInterpolators.put(nodeId, new PositionInterpolator(kfs));
                    }
                    case ORIENTATION -> {
                        List<BlockyKeyframe.OrientationKeyframe> kfs = castKeyframes(
                                track.getKeyframes(), BlockyKeyframe.OrientationKeyframe.class);
                        if (!kfs.isEmpty()) orientationInterpolators.put(nodeId, new OrientationInterpolator(kfs));
                    }
                    case SHAPE_STRETCH -> {
                        List<BlockyKeyframe.ScaleKeyframe> kfs = castKeyframes(
                                track.getKeyframes(), BlockyKeyframe.ScaleKeyframe.class);
                        if (!kfs.isEmpty()) scaleInterpolators.put(nodeId, new ScaleInterpolator(kfs));
                    }
                    case SHAPE_VISIBLE -> {
                        List<BlockyKeyframe.VisibilityKeyframe> kfs = castKeyframes(
                                track.getKeyframes(), BlockyKeyframe.VisibilityKeyframe.class);
                        if (!kfs.isEmpty()) visibilityInterpolators.put(nodeId, new VisibilityInterpolator(kfs));
                    }
                    default -> {
                    } // Other track types ignored for now
                }
            }
        }
    }

    /**
     * Utility method to filter and cast a list of BlockyKeyframes to a specific subtype, based on the provided class type
     *
     * @param <K>  The specific subtype of BlockyKeyframe to filter and cast to
     * @param raw  The original list of BlockyKeyframes to filter and cast
     * @param type The Class object representing the specific subtype of BlockyKeyframe to filter and cast to
     * @return A new list containing only the keyframes from the original list that are instances of the specified type, cast to that type
     */
    private static <K extends BlockyKeyframe> List<K> castKeyframes(
            List<BlockyKeyframe> raw, Class<K> type) {
        List<K> result = new ArrayList<>(raw.size());
        for (BlockyKeyframe kf : raw) {
            if (type.isInstance(kf)) result.add((K) kf);
        }
        return result;
    }

    private static final class PositionInterpolator {
        private final List<BlockyKeyframe.PositionKeyframe> keyframes;

        PositionInterpolator(List<BlockyKeyframe.PositionKeyframe> keyframes) {
            this.keyframes = new ArrayList<>(keyframes);
        }

        Vector3f interpolate(float t, float duration, boolean cyclic) {
            if (keyframes.isEmpty()) return null;
            if (keyframes.size() == 1) return keyframes.getFirst().getDelta();

            Segment seg = findSegment(keyframes, t, duration, cyclic);
            int prev = seg.from();
            int next = seg.to();

            if (prev == next) return new Vector3f(keyframes.get(prev).getDelta());

            BlockyKeyframe.PositionKeyframe a = keyframes.get(prev);
            BlockyKeyframe.PositionKeyframe b = keyframes.get(next);
            float alpha = seg.alpha();

            return interpolateVec(a.getDelta(), b.getDelta(), alpha, a.getInterpolationType());
        }
    }


    private static final class OrientationInterpolator {
        private final List<BlockyKeyframe.OrientationKeyframe> keyframes;

        OrientationInterpolator(List<BlockyKeyframe.OrientationKeyframe> keyframes) {
            this.keyframes = new ArrayList<>(keyframes);
        }

        Quaternionf interpolate(float t, float duration, boolean cyclic) {
            if (keyframes.isEmpty()) return null;
            if (keyframes.size() == 1) return keyframes.getFirst().getDelta();

            Segment seg = findSegment(keyframes, t, duration, cyclic);
            int prev = seg.from();
            int next = seg.to();

            if (prev == next) return new Quaternionf(keyframes.get(prev).getDelta());

            BlockyKeyframe.OrientationKeyframe a = keyframes.get(prev);
            BlockyKeyframe.OrientationKeyframe b = keyframes.get(next);
            float alpha = seg.alpha();

            float slerpAlpha = (a.getInterpolationType() == BlockyKeyframe.InterpolationType.LINEAR)
                    ? alpha
                    : smoothstep(alpha);

            return new Quaternionf(a.getDelta()).slerp(b.getDelta(), slerpAlpha);
        }
    }

    private static final class ScaleInterpolator {
        private final List<BlockyKeyframe.ScaleKeyframe> keyframes;

        ScaleInterpolator(List<BlockyKeyframe.ScaleKeyframe> keyframes) {
            this.keyframes = new ArrayList<>(keyframes);
        }

        Vector3f interpolate(float t, float duration, boolean cyclic) {
            if (keyframes.isEmpty()) return null;
            if (keyframes.size() == 1) return keyframes.getFirst().getDelta();

            Segment seg = findSegment(keyframes, t, duration, cyclic);
            int prev = seg.from();
            int next = seg.to();

            if (prev == next) return new Vector3f(keyframes.get(prev).getDelta());

            BlockyKeyframe.ScaleKeyframe a = keyframes.get(prev);
            BlockyKeyframe.ScaleKeyframe b = keyframes.get(next);
            float alpha = seg.alpha();

            return interpolateVec(a.getDelta(), b.getDelta(), alpha, a.getInterpolationType());
        }
    }


    private static final class VisibilityInterpolator {
        private final List<BlockyKeyframe.VisibilityKeyframe> keyframes;

        VisibilityInterpolator(List<BlockyKeyframe.VisibilityKeyframe> keyframes) {
            this.keyframes = new ArrayList<>(keyframes);
        }

        boolean isVisible(float t) {
            if (keyframes.isEmpty()) return true;
            return keyframes.get(prevIndex(keyframes, t)).isVisible();
        }
    }


    /**
     * Utility method to clamp the interpolation alpha value between 0 and 1 based on the input time and the keyframe times
     *
     * @param t      The current time for which to calculate the alpha
     * @param tStart The time of the previous keyframe
     * @param tEnd   The time of the next keyframe
     * @return The clamped alpha value between 0 and 1
     */
    private static float clampAlpha(float t, float tStart, float tEnd) {
        float span = tEnd - tStart;
        if (span <= 0f) return 0f;
        return Math.clamp((t - tStart) / span, 0f, 1f);
    }

    /**
     * Utility method to apply a smoothstep function to the input alpha value for smoother interpolation
     *
     * @param t The input alpha value between 0 and 1
     * @return The output alpha value after applying the smoothstep function, also between 0 and 1
     */
    private static float smoothstep(float t) {
        return t * t * (3 - 2 * t);
    }

    /**
     * Utility method to linearly interpolate between two Vector3f values based on the input alpha
     *
     * @param from The starting Vector3f value
     * @param to   The ending Vector3f value
     * @param t    The interpolation alpha value between 0 and 1
     * @return A new Vector3f that is the result of linearly interpolating between 'from' and 'to' based on 't'
     */
    private static Vector3f lerpVec(Vector3f from, Vector3f to, float t) {
        return new Vector3f(from).lerp(to, t);
    }

    /**
     * Utility method to interpolate between two Vector3f values using either linear or smooth interpolation based on the specified type
     *
     * @param from The starting Vector3f value
     * @param to   The ending Vector3f value
     * @param t    The interpolation alpha value between 0 and 1
     * @return A new Vector3f that is the result of interpolating between 'from' and 'to' based on 't'
     */
    private static Vector3f smoothstepVec(Vector3f from, Vector3f to, float t) {
        return lerpVec(from, to, smoothstep(t));
    }

    /**
     * Utility method to interpolate between two Vector3f values based on the input alpha and interpolation type (linear or smooth)
     *
     * @param from  The starting Vector3f value
     * @param to    The ending Vector3f value
     * @param alpha The interpolation alpha value between 0 and 1
     * @param type  The interpolation type (LINEAR or SMOOTH)
     * @return A new Vector3f that is the result of interpolating between 'from' and 'to' based on 'alpha'
     */
    private static Vector3f interpolateVec(Vector3f from, Vector3f to, float alpha,
                                           BlockyKeyframe.InterpolationType type) {
        return switch (type) {
            case LINEAR -> lerpVec(from, to, alpha);
            case SMOOTH -> smoothstepVec(from, to, alpha);
        };
    }

    /**
     * The pair of keyframes a time falls between, and how far along that pair it is
     *
     * @param from  index of the keyframe the segment starts at
     * @param to    index of the keyframe the segment ends at
     * @param alpha progress from 0 to 1 along the segment
     */
    private record Segment(int from, int to, float alpha) {
    }

    /**
     * Finds the keyframe segment containing the given time. For a looping animation, times after the last
     * keyframe interpolate towards the first keyframe one loop later, and times before the first keyframe interpolate
     * from the last keyframe one loop earlier. Otherwise, the first and last keyframes are simply held.
     *
     * @param keyframes keyframes sorted by time
     * @param t         the time, already wrapped into the animation's duration
     * @param duration  the length of one loop
     * @param cyclic    whether the animation loops
     * @return the segment containing the time
     */
    private static <K extends BlockyKeyframe> Segment findSegment(List<K> keyframes, float t,
                                                                  float duration, boolean cyclic) {
        int last = keyframes.size() - 1;
        int prev = prevIndex(keyframes, t);
        float firstTime = keyframes.getFirst().getTime();
        float lastTime = keyframes.get(last).getTime();

        if (cyclic && last > 0 && duration > 0f) {
            if (t < firstTime) {
                return new Segment(last, 0, clampAlpha(t, lastTime - duration, firstTime));
            }
            if (prev == last) {
                return new Segment(last, 0, clampAlpha(t, lastTime, firstTime + duration));
            }
        }

        int next = Math.min(prev + 1, last);
        float alpha = prev == next ? 0f : clampAlpha(t, keyframes.get(prev).getTime(), keyframes.get(next).getTime());
        return new Segment(prev, next, alpha);
    }

    /**
     * Utility method to find the index of the previous keyframe in a sorted list of keyframes based on the input time
     *
     * @param <K>       The specific subtype of BlockyKeyframe contained in the list
     * @param keyframes A list of keyframes sorted by their time value in ascending order
     * @param time      The input time for which to find the previous keyframe index
     * @return The index of the keyframe in the list that is the greatest keyframe time less than or equal to the input time
     */
    private static <K extends BlockyKeyframe> int prevIndex(List<K> keyframes, float time) {
        int lo = 0, hi = keyframes.size() - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (keyframes.get(mid).getTime() <= time) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

}