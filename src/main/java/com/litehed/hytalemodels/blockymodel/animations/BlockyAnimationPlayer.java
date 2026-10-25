package com.litehed.hytalemodels.blockymodel.animations;

import com.litehed.hytalemodels.api.NodeTransform;
import com.litehed.hytalemodels.blockymodel.TransformCalculator;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockyAnimationPlayer {

    private static final float POSITION_SCALE = 1f / TransformCalculator.UNITS_PER_BLOCK;

    private final BlockyAnimationDefinition definition;
    private final Map<String, VectorInterpolator> positionInterpolators = new HashMap<>();
    private final Map<String, OrientationInterpolator> orientationInterpolators = new HashMap<>();
    private final Map<String, VectorInterpolator> scaleInterpolators = new HashMap<>();
    private final Map<String, VisibilityInterpolator> visibilityInterpolators = new HashMap<>();

    public BlockyAnimationPlayer(BlockyAnimationDefinition definition) {
        this.definition = definition;
        initializeInterpolators();
    }

    public float getAnimationDuration() {
        return definition.getDuration();
    }

    /**
     * Works out the transform of every animated node at a time
     * Looping animations wrap around, animations that hold their last keyframe stop at the end
     *
     * @param timeInTicks the time in animation units
     * @return the transforms keyed by node id or name
     */
    public Map<String, NodeTransform> calculateTransforms(float timeInTicks) {
        float duration = definition.getDuration();
        // A looping animation blends from its last keyframe back to its first
        boolean cyclic = !definition.isHoldLastKeyframe();
        float t = resolveTime(timeInTicks, duration, cyclic);
        Map<String, NodeTransform> transforms = new HashMap<>();

        positionInterpolators.forEach((nodeId, interp) -> {
            Vector3f pos = interp.interpolate(t, duration, cyclic);
            transforms.merge(nodeId, NodeTransform.translation(pos.mul(POSITION_SCALE)), NodeTransform::merge);
        });

        orientationInterpolators.forEach((nodeId, interp) -> {
            Quaternionf rot = interp.interpolate(t, duration, cyclic);
            transforms.merge(nodeId, NodeTransform.rotation(rot), NodeTransform::merge);
        });

        scaleInterpolators.forEach((nodeId, interp) -> {
            Vector3f scale = interp.interpolate(t, duration, cyclic);
            transforms.merge(nodeId, NodeTransform.scale(scale), NodeTransform::merge);
        });

        visibilityInterpolators.forEach((nodeId, interp) ->
                transforms.merge(nodeId, NodeTransform.visibility(interp.isVisible(t)), NodeTransform::merge));

        return transforms;
    }

    private static float resolveTime(float time, float duration, boolean cyclic) {
        if (duration <= 0f) {
            return 0f;
        }
        return cyclic ? time % duration : Math.min(time, duration);
    }

    private void initializeInterpolators() {
        for (Map.Entry<String, List<NodeAnimationTrack>> entry : definition.getNodeAnimations().entrySet()) {
            String nodeId = entry.getKey();
            for (NodeAnimationTrack track : entry.getValue()) {
                switch (track.getTrackType()) {
                    case POSITION -> {
                        List<BlockyKeyframe.PositionKeyframe> kfs = castKeyframes(
                                track.getKeyframes(), BlockyKeyframe.PositionKeyframe.class);
                        if (!kfs.isEmpty()) positionInterpolators.put(nodeId, new VectorInterpolator(kfs));
                    }
                    case ORIENTATION -> {
                        List<BlockyKeyframe.OrientationKeyframe> kfs = castKeyframes(
                                track.getKeyframes(), BlockyKeyframe.OrientationKeyframe.class);
                        if (!kfs.isEmpty()) orientationInterpolators.put(nodeId, new OrientationInterpolator(kfs));
                    }
                    case SHAPE_STRETCH -> {
                        List<BlockyKeyframe.ScaleKeyframe> kfs = castKeyframes(
                                track.getKeyframes(), BlockyKeyframe.ScaleKeyframe.class);
                        if (!kfs.isEmpty()) scaleInterpolators.put(nodeId, new VectorInterpolator(kfs));
                    }
                    case SHAPE_VISIBLE -> {
                        List<BlockyKeyframe.VisibilityKeyframe> kfs = castKeyframes(
                                track.getKeyframes(), BlockyKeyframe.VisibilityKeyframe.class);
                        if (!kfs.isEmpty()) visibilityInterpolators.put(nodeId, new VisibilityInterpolator(kfs));
                    }
                    default -> {
                    }
                }
            }
        }
    }

    private static <K extends BlockyKeyframe> List<K> castKeyframes(List<BlockyKeyframe> raw, Class<K> type) {
        List<K> result = new ArrayList<>(raw.size());
        for (BlockyKeyframe kf : raw) {
            if (type.isInstance(kf)) result.add(type.cast(kf));
        }
        return result;
    }

    // Position and scale both blend a vector
    private static final class VectorInterpolator {
        private final List<BlockyKeyframe.VectorKeyframe> keyframes;

        VectorInterpolator(List<? extends BlockyKeyframe.VectorKeyframe> keyframes) {
            this.keyframes = new ArrayList<>(keyframes);
        }

        Vector3f interpolate(float t, float duration, boolean cyclic) {
            if (keyframes.size() == 1) return keyframes.getFirst().getDelta();

            Segment seg = findSegment(keyframes, t, duration, cyclic);
            BlockyKeyframe.VectorKeyframe a = keyframes.get(seg.from());
            if (seg.from() == seg.to()) return a.getDelta();

            BlockyKeyframe.VectorKeyframe b = keyframes.get(seg.to());
            float alpha = ease(seg.alpha(), a.getInterpolationType());
            return a.getDelta().lerp(b.getDelta(), alpha);
        }
    }

    private static final class OrientationInterpolator {
        private final List<BlockyKeyframe.OrientationKeyframe> keyframes;

        OrientationInterpolator(List<BlockyKeyframe.OrientationKeyframe> keyframes) {
            this.keyframes = new ArrayList<>(keyframes);
        }

        Quaternionf interpolate(float t, float duration, boolean cyclic) {
            if (keyframes.size() == 1) return keyframes.getFirst().getDelta();

            Segment seg = findSegment(keyframes, t, duration, cyclic);
            BlockyKeyframe.OrientationKeyframe a = keyframes.get(seg.from());
            if (seg.from() == seg.to()) return a.getDelta();

            BlockyKeyframe.OrientationKeyframe b = keyframes.get(seg.to());
            float alpha = ease(seg.alpha(), a.getInterpolationType());
            return a.getDelta().slerp(b.getDelta(), alpha);
        }
    }

    private static final class VisibilityInterpolator {
        private final List<BlockyKeyframe.VisibilityKeyframe> keyframes;

        VisibilityInterpolator(List<BlockyKeyframe.VisibilityKeyframe> keyframes) {
            this.keyframes = new ArrayList<>(keyframes);
        }

        boolean isVisible(float t) {
            return keyframes.get(prevIndex(keyframes, t)).isVisible();
        }
    }

    private static float ease(float alpha, BlockyKeyframe.InterpolationType type) {
        return type == BlockyKeyframe.InterpolationType.LINEAR ? alpha : alpha * alpha * (3 - 2 * alpha);
    }

    private static float clampAlpha(float t, float tStart, float tEnd) {
        float span = tEnd - tStart;
        if (span <= 0f) return 0f;
        return Math.clamp((t - tStart) / span, 0f, 1f);
    }

    // The two keyframes a time falls between, and how far along them it is
    private record Segment(int from, int to, float alpha) {
    }

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

    // Index of the last keyframe at or before the time, the keyframes must be sorted
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