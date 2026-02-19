package com.litehed.hytalemodels.blockymodel.animations;

import com.litehed.hytalemodels.blocks.entity.NodeTransform;
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

    private float resolveTime(float timeInTicks) {
        float duration = definition.getDuration();
        return definition.isHoldLastKeyframe()
                ? Math.min(timeInTicks, duration)
                : timeInTicks % duration;
    }

    private static void mergeIntoMap(Map<String, NodeTransform> map,
                                     String nodeId,
                                     NodeTransform incoming) {
        map.merge(nodeId, incoming, NodeTransform::merge);
    }

    public Map<String, NodeTransform> calculateTransforms(float timeInTicks) {
        float t = resolveTime(timeInTicks);
        Map<String, NodeTransform> transforms = new HashMap<>();

        positionInterpolators.forEach((nodeId, interp) -> {
            Vector3f pos = interp.interpolate(t);
            if (pos != null) {
                Vector3f scaled = pos.mul(POSITION_SCALE, new Vector3f());
                mergeIntoMap(transforms, nodeId, NodeTransform.translation(scaled));
            }
        });

        orientationInterpolators.forEach((nodeId, interp) -> {
            Quaternionf rot = interp.interpolate(t);
            if (rot != null) {
                mergeIntoMap(transforms, nodeId, NodeTransform.rotation(rot));
            }
        });

        scaleInterpolators.forEach((nodeId, interp) -> {
            Vector3f scale = interp.interpolate(t);
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

        Vector3f interpolate(float t) {
            if (keyframes.isEmpty()) return null;
            if (keyframes.size() == 1) return keyframes.getFirst().getDelta();

            int prev = prevIndex(keyframes, t);
            int next = Math.min(prev + 1, keyframes.size() - 1);

            if (prev == next) return new Vector3f(keyframes.get(prev).getDelta());

            BlockyKeyframe.PositionKeyframe a = keyframes.get(prev);
            BlockyKeyframe.PositionKeyframe b = keyframes.get(next);
            float alpha = clampAlpha(t, a.getTime(), b.getTime());

            return interpolateVec(a.getDelta(), b.getDelta(), alpha, a.getInterpolationType());
        }
    }


    private static final class OrientationInterpolator {
        private final List<BlockyKeyframe.OrientationKeyframe> keyframes;

        OrientationInterpolator(List<BlockyKeyframe.OrientationKeyframe> keyframes) {
            this.keyframes = new ArrayList<>(keyframes);
        }

        Quaternionf interpolate(float t) {
            if (keyframes.isEmpty()) return null;
            if (keyframes.size() == 1) return keyframes.getFirst().getDelta();

            int prev = prevIndex(keyframes, t);
            int next = Math.min(prev + 1, keyframes.size() - 1);

            if (prev == next) return new Quaternionf(keyframes.get(prev).getDelta());

            BlockyKeyframe.OrientationKeyframe a = keyframes.get(prev);
            BlockyKeyframe.OrientationKeyframe b = keyframes.get(next);
            float alpha = clampAlpha(t, a.getTime(), b.getTime());

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

        Vector3f interpolate(float t) {
            if (keyframes.isEmpty()) return null;
            if (keyframes.size() == 1) return keyframes.getFirst().getDelta();

            int prev = prevIndex(keyframes, t);
            int next = Math.min(prev + 1, keyframes.size() - 1);

            if (prev == next) return new Vector3f(keyframes.get(prev).getDelta());

            BlockyKeyframe.ScaleKeyframe a = keyframes.get(prev);
            BlockyKeyframe.ScaleKeyframe b = keyframes.get(next);
            float alpha = clampAlpha(t, a.getTime(), b.getTime());

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


    private static float clampAlpha(float t, float tStart, float tEnd) {
        float span = tEnd - tStart;
        if (span <= 0f) return 0f;
        return Math.clamp((t - tStart) / span, 0f, 1f);
    }

    private static float smoothstep(float t) {
        return t * t * (3 - 2 * t);
    }

    private static Vector3f lerpVec(Vector3f from, Vector3f to, float t) {
        return new Vector3f(from).lerp(to, t);
    }

    private static Vector3f smoothstepVec(Vector3f from, Vector3f to, float t) {
        return lerpVec(from, to, smoothstep(t));
    }

    private static Vector3f interpolateVec(Vector3f from, Vector3f to, float alpha,
                                           BlockyKeyframe.InterpolationType type) {
        return switch (type) {
            case LINEAR -> lerpVec(from, to, alpha);
            case SMOOTH -> smoothstepVec(from, to, alpha);
        };
    }

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