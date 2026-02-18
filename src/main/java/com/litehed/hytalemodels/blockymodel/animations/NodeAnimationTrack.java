package com.litehed.hytalemodels.blockymodel.animations;

import java.util.List;

public final class NodeAnimationTrack {

    private final String nodeId;
    private final AnimationTrackType trackType;
    private final List<BlockyKeyframe> keyframes;

    public NodeAnimationTrack(String nodeId, AnimationTrackType trackType, List<BlockyKeyframe> keyframes) {
        this.nodeId = nodeId;
        this.trackType = trackType;
        this.keyframes = List.copyOf(keyframes); // Immutable copy
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

    /**
     * Enum representing different types of animation tracks
     */
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

        public String getJsonKey() {
            return jsonKey;
        }

        public static AnimationTrackType fromJsonKey(String key) {
            return switch (key) {
                case "position" -> POSITION;
                case "orientation" -> ORIENTATION;
                case "shapeStretch" -> SHAPE_STRETCH;
                case "shapeVisible" -> SHAPE_VISIBLE;
                case "shapeUvOffset" -> SHAPE_UV_OFFSET;
                default -> null;
            };
        }
    }
}






