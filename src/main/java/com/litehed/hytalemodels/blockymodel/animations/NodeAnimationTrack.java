package com.litehed.hytalemodels.blockymodel.animations;

import java.util.List;

public final class NodeAnimationTrack {

    // A node name or id, animation files use either
    private final String nodeId;
    private final AnimationTrackType trackType;
    private final List<BlockyKeyframe> keyframes;

    public NodeAnimationTrack(String nodeId, AnimationTrackType trackType, List<BlockyKeyframe> keyframes) {
        this.nodeId = nodeId;
        this.trackType = trackType;
        this.keyframes = List.copyOf(keyframes);
    }

    public String getNodeId() {
        return nodeId;
    }

    public AnimationTrackType getTrackType() {
        return trackType;
    }

    public List<BlockyKeyframe> getKeyframes() {
        return keyframes;
    }

    public enum AnimationTrackType {
        POSITION("position"),
        ORIENTATION("orientation"),
        SHAPE_STRETCH("shapeStretch"),
        SHAPE_VISIBLE("shapeVisible"),
        SHAPE_UV_OFFSET("shapeUvOffset");

        private final String jsonKey;

        AnimationTrackType(String jsonKey) {
            this.jsonKey = jsonKey;
        }

        /**
         * @param key the track name used in animation files
         * @return the matching track type, or null if there is none
         */
        public static AnimationTrackType fromJsonKey(String key) {
            for (AnimationTrackType type : values()) {
                if (type.jsonKey.equals(key)) {
                    return type;
                }
            }
            return null;
        }
    }
}