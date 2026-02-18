package com.litehed.hytalemodels.blockymodel.animations;

import com.litehed.hytalemodels.blocks.entity.NodeTransform;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockyAnimationPlayer {
    private final BlockyAnimationDefinition definition;
    private final Map<String, PositionInterpolator> positionInterpolators = new HashMap<>();
    private final Map<String, OrientationInterpolator> orientationInterpolators = new HashMap<>();
    private final Map<String, VisibilityInterpolator> visibilityInterpolators = new HashMap<>();
    private final Map<String, ScaleInterpolator> scaleInterpolators = new HashMap<>();

    public BlockyAnimationPlayer(BlockyAnimationDefinition definition) {
        this.definition = definition;
        initializeInterpolators();
    }

    private void initializeInterpolators() {
        for (Map.Entry<String, List<NodeAnimationTrack>> entry : definition.getNodeAnimations().entrySet()) {
            String nodeId = entry.getKey();
            for (NodeAnimationTrack track : entry.getValue()) {
                switch (track.getTrackType()) {
                    case POSITION -> {
                        List<BlockyKeyframe.PositionKeyframe> keyframes = track.getKeyframes().stream()
                                .filter(kf -> kf instanceof BlockyKeyframe.PositionKeyframe)
                                .map(kf -> (BlockyKeyframe.PositionKeyframe) kf)
                                .toList();
                        if (!keyframes.isEmpty()) {
                            positionInterpolators.put(nodeId, new PositionInterpolator(keyframes));
                        }
                    }
                    case ORIENTATION -> {
                        List<BlockyKeyframe.OrientationKeyframe> keyframes = track.getKeyframes().stream()
                                .filter(kf -> kf instanceof BlockyKeyframe.OrientationKeyframe)
                                .map(kf -> (BlockyKeyframe.OrientationKeyframe) kf)
                                .toList();
                        if (!keyframes.isEmpty()) {
                            orientationInterpolators.put(nodeId, new OrientationInterpolator(keyframes));
                        }
                    }
                    case SHAPE_VISIBLE -> {
                        List<BlockyKeyframe.VisibilityKeyframe> keyframes = track.getKeyframes().stream()
                                .filter(kf -> kf instanceof BlockyKeyframe.VisibilityKeyframe)
                                .map(kf -> (BlockyKeyframe.VisibilityKeyframe) kf)
                                .toList();
                        if (!keyframes.isEmpty()) {
                            visibilityInterpolators.put(nodeId, new VisibilityInterpolator(keyframes));
                        }
                    }
                    case SHAPE_STRETCH -> {
                        List<BlockyKeyframe.ScaleKeyframe> keyframes = track.getKeyframes().stream()
                                .filter(kf -> kf instanceof BlockyKeyframe.ScaleKeyframe)
                                .map(kf -> (BlockyKeyframe.ScaleKeyframe) kf)
                                .toList();
                        if (!keyframes.isEmpty()) {
                            scaleInterpolators.put(nodeId, new ScaleInterpolator(keyframes));
                        }
                    }
                    default -> {
                    } // Other track types ignored for now
                }
            }
        }
    }

    public Map<String, NodeTransform> calculateTransforms(float timeInTicks) {
        Map<String, NodeTransform> transforms = new HashMap<>();
        float elapsedTime = calculateElapsedTime(timeInTicks);

        // Position updates
        for (Map.Entry<String, PositionInterpolator> entry : positionInterpolators.entrySet()) {
            Vector3f position = entry.getValue().interpolate(elapsedTime);
            if (position != null) {
                transforms.put(entry.getKey(), NodeTransform.translation(position));
            }
        }

        // Orientation updates - need to merge with position updates
        for (Map.Entry<String, OrientationInterpolator> entry : orientationInterpolators.entrySet()) {
            Quaternionf rotation = entry.getValue().interpolate(elapsedTime);
            if (rotation != null) {
                String nodeId = entry.getKey();
                NodeTransform existing = transforms.get(nodeId);
                if (existing != null && !existing.position().equals(new Vector3f())) {
                    // Merge position and rotation
                    transforms.put(nodeId, new NodeTransform(existing.position(), rotation, existing.scale()));
                } else {
                    transforms.put(nodeId, NodeTransform.rotation(rotation));
                }
            }
        }
        return transforms;
    }

    private float calculateElapsedTime(float timeInTicks) {
        if (definition.isHoldLastKeyframe()) {
            return Math.min(timeInTicks, definition.getDuration());
        } else {
            return timeInTicks % definition.getDuration();
        }
    }

    public float getAnimationDuration() {
        return definition.getDuration();
    }

    public boolean isHoldLastKeyframe() {
        return definition.isHoldLastKeyframe();
    }

    private static final class PositionInterpolator {
        private final List<BlockyKeyframe.PositionKeyframe> keyframes;

        PositionInterpolator(List<BlockyKeyframe.PositionKeyframe> keyframes) {
            this.keyframes = new ArrayList<>(keyframes);
        }

        Vector3f interpolate(float elapsedTime) {
            if (keyframes.isEmpty()) return null;

            // Find surrounding keyframes
            int nextIdx = 0;
            while (nextIdx < keyframes.size() && keyframes.get(nextIdx).getTime() <= elapsedTime) {
                nextIdx++;
            }

            int prevIdx = Math.max(0, nextIdx - 1);
            if (nextIdx >= keyframes.size()) {
                nextIdx = keyframes.size() - 1;
            }

            if (prevIdx == nextIdx) {
                return new Vector3f(keyframes.get(prevIdx).getDelta());
            }

            BlockyKeyframe.PositionKeyframe prevKeyframe = keyframes.get(prevIdx);
            BlockyKeyframe.PositionKeyframe nextKeyframe = keyframes.get(nextIdx);

            float timeDiff = nextKeyframe.getTime() - prevKeyframe.getTime();
            float alpha = (elapsedTime - prevKeyframe.getTime()) / timeDiff;
            alpha = Math.max(0, Math.min(1, alpha)); // Clamp to [0, 1]

            // Apply interpolation based on type
            Vector3f result = interpolatePosition(
                    prevKeyframe, nextKeyframe, alpha, prevKeyframe.getInterpolationType());

            return result;
        }

        private Vector3f interpolatePosition(
                BlockyKeyframe.PositionKeyframe prev,
                BlockyKeyframe.PositionKeyframe next,
                float alpha,
                BlockyKeyframe.InterpolationType interpolationType) {

            Vector3f prevDelta = prev.getDelta();
            Vector3f nextDelta = next.getDelta();

            return switch (interpolationType) {
                case LINEAR -> new Vector3f(prevDelta).lerp(nextDelta, alpha);
                case SMOOTH -> smoothstep(prevDelta, nextDelta, alpha);
                case CATMULLROM -> catmullromInterpolate(prevDelta, nextDelta, alpha);
            };
        }

        private Vector3f smoothstep(Vector3f from, Vector3f to, float t) {
            // Smoothstep: 3t^2 - 2t^3
            float smoothT = t * t * (3 - 2 * t);
            return new Vector3f(from).lerp(to, smoothT);
        }

        private Vector3f catmullromInterpolate(Vector3f from, Vector3f to, float t) {
            // Simple Catmull-Rom approximation (simplified version)
            // For full implementation, would need access to adjacent keyframes
            float t2 = t * t;
            float t3 = t2 * t;

            float mt = 1.0f - t;
            float mt2 = mt * mt;
            float mt3 = mt2 * mt;

            float coeff0 = -0.5f * mt3 + mt2 - 0.5f * mt;
            float coeff1 = 1.5f * t3 - 2.5f * t2 + 1.0f;
            float coeff2 = -1.5f * t3 + 2.0f * t2 + 0.5f * t;
            float coeff3 = 0.5f * t3 - 0.5f * t2;

            return new Vector3f(
                    from.x * coeff1 + to.x * coeff2,
                    from.y * coeff1 + to.y * coeff2,
                    from.z * coeff1 + to.z * coeff2
            );
        }
    }

    /**
     * Interpolates orientation keyframes over time
     */
    private static final class OrientationInterpolator {
        private final List<BlockyKeyframe.OrientationKeyframe> keyframes;

        OrientationInterpolator(List<BlockyKeyframe.OrientationKeyframe> keyframes) {
            this.keyframes = new ArrayList<>(keyframes);
        }

        Quaternionf interpolate(float elapsedTime) {
            if (keyframes.isEmpty()) return null;

            // Find surrounding keyframes
            int nextIdx = 0;
            while (nextIdx < keyframes.size() && keyframes.get(nextIdx).getTime() <= elapsedTime) {
                nextIdx++;
            }

            int prevIdx = Math.max(0, nextIdx - 1);
            if (nextIdx >= keyframes.size()) {
                nextIdx = keyframes.size() - 1;
            }

            if (prevIdx == nextIdx) {
                return new Quaternionf(keyframes.get(prevIdx).getDelta());
            }

            BlockyKeyframe.OrientationKeyframe prevKeyframe = keyframes.get(prevIdx);
            BlockyKeyframe.OrientationKeyframe nextKeyframe = keyframes.get(nextIdx);

            float timeDiff = nextKeyframe.getTime() - prevKeyframe.getTime();
            float alpha = (elapsedTime - prevKeyframe.getTime()) / timeDiff;
            alpha = Math.max(0, Math.min(1, alpha)); // Clamp to [0, 1]

            return new Quaternionf(prevKeyframe.getDelta())
                    .slerp(nextKeyframe.getDelta(), alpha);
        }
    }

    /**
     * Interpolates visibility keyframes over time
     */
    private static final class VisibilityInterpolator {
        private final List<BlockyKeyframe.VisibilityKeyframe> keyframes;

        VisibilityInterpolator(List<BlockyKeyframe.VisibilityKeyframe> keyframes) {
            this.keyframes = new ArrayList<>(keyframes);
        }

        boolean isVisible(float elapsedTime) {
            if (keyframes.isEmpty()) return true;

            // Find the most recent keyframe at or before elapsedTime
            BlockyKeyframe.VisibilityKeyframe lastKeyframe = keyframes.getFirst();
            for (BlockyKeyframe.VisibilityKeyframe kf : keyframes) {
                if (kf.getTime() <= elapsedTime) {
                    lastKeyframe = kf;
                } else {
                    break;
                }
            }

            return lastKeyframe.isVisible();
        }
    }

    private static final class ScaleInterpolator {
        private final List<BlockyKeyframe.ScaleKeyframe> keyframes;

        ScaleInterpolator(List<BlockyKeyframe.ScaleKeyframe> keyframes) {
            this.keyframes = new ArrayList<>(keyframes);
        }

        private Vector3f interpolateScale(
                BlockyKeyframe.PositionKeyframe prev,
                BlockyKeyframe.PositionKeyframe next,
                float alpha,
                BlockyKeyframe.InterpolationType interpolationType) {
            Vector3f prevDelta = prev.getDelta();
            Vector3f nextDelta = next.getDelta();

            return switch (interpolationType) {
                case LINEAR -> new Vector3f(prevDelta).lerp(nextDelta, alpha);
                case SMOOTH -> smoothstep(prevDelta, nextDelta, alpha);
                case CATMULLROM -> catmullromInterpolate(prevDelta, nextDelta, alpha);
            };
        }

        private Vector3f smoothstep(Vector3f from, Vector3f to, float t) {
            // Smoothstep: 3t^2 - 2t^3
            float smoothT = t * t * (3 - 2 * t);
            return new Vector3f(from).lerp(to, smoothT);
        }

        private Vector3f catmullromInterpolate(Vector3f from, Vector3f to, float t) {
            // Simple Catmull-Rom approximation (simplified version)
            // For full implementation, would need access to adjacent keyframes
            float t2 = t * t;
            float t3 = t2 * t;

            float mt = 1.0f - t;
            float mt2 = mt * mt;
            float mt3 = mt2 * mt;

            float coeff0 = -0.5f * mt3 + mt2 - 0.5f * mt;
            float coeff1 = 1.5f * t3 - 2.5f * t2 + 1.0f;
            float coeff2 = -1.5f * t3 + 2.0f * t2 + 0.5f * t;
            float coeff3 = 0.5f * t3 - 0.5f * t2;

            return new Vector3f(
                    from.x * coeff1 + to.x * coeff2,
                    from.y * coeff1 + to.y * coeff2,
                    from.z * coeff1 + to.z * coeff2
            );
        }

    }
}