package com.litehed.hytalemodels.blockymodel.animations;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class BlockyKeyframe {

    private final float time;
    private final InterpolationType interpolationType;

    protected BlockyKeyframe(float time, InterpolationType interpolationType) {
        this.time = time;
        this.interpolationType = interpolationType;
    }

    public float getTime() {
        return time;
    }

    public InterpolationType getInterpolationType() {
        return interpolationType;
    }

    // Keyframes that hold a vector, such as position and scale
    public abstract static class VectorKeyframe extends BlockyKeyframe {
        private final Vector3f delta;

        protected VectorKeyframe(float time, Vector3f delta, InterpolationType interpolationType) {
            super(time, interpolationType);
            this.delta = new Vector3f(delta);
        }

        public Vector3f getDelta() {
            return new Vector3f(delta);
        }
    }

    public static final class PositionKeyframe extends VectorKeyframe {
        public PositionKeyframe(float time, Vector3f delta, InterpolationType interpolationType) {
            super(time, delta, interpolationType);
        }
    }

    public static final class ScaleKeyframe extends VectorKeyframe {
        public ScaleKeyframe(float time, Vector3f delta, InterpolationType interpolationType) {
            super(time, delta, interpolationType);
        }
    }

    public static final class OrientationKeyframe extends BlockyKeyframe {
        private final Quaternionf delta;

        public OrientationKeyframe(float time, Quaternionf delta, InterpolationType interpolationType) {
            super(time, interpolationType);
            this.delta = new Quaternionf(delta);
        }

        public Quaternionf getDelta() {
            return new Quaternionf(delta);
        }
    }

    public static final class VisibilityKeyframe extends BlockyKeyframe {
        private final boolean visible;

        public VisibilityKeyframe(float time, boolean visible, InterpolationType interpolationType) {
            super(time, interpolationType);
            this.visible = visible;
        }

        public boolean isVisible() {
            return visible;
        }
    }

    // How a keyframe blends into the next one
    public enum InterpolationType {
        LINEAR,
        SMOOTH
    }
}