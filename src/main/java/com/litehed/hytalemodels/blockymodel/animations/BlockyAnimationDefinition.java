package com.litehed.hytalemodels.blockymodel.animations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockyAnimationDefinition {

    private final float duration; // in ticks/frames
    private final boolean holdLastKeyframe;
    private final Map<String, List<NodeAnimationTrack>> nodeAnimations;

    public BlockyAnimationDefinition(
            float duration,
            boolean holdLastKeyframe,
            Map<String, List<NodeAnimationTrack>> nodeAnimations) {
        this.duration = duration;
        this.holdLastKeyframe = holdLastKeyframe;
        this.nodeAnimations = Map.copyOf(nodeAnimations);
    }

    public float getDuration() {
        return duration;
    }

    public boolean isHoldLastKeyframe() {
        return holdLastKeyframe;
    }

    public Map<String, List<NodeAnimationTrack>> getNodeAnimations() {
        return nodeAnimations;
    }

    public List<NodeAnimationTrack> getNodeTracks(String nodeId) {
        return nodeAnimations.getOrDefault(nodeId, List.of());
    }

    public NodeAnimationTrack getNodeTrack(String nodeId, NodeAnimationTrack.AnimationTrackType trackType) {
        return getNodeTracks(nodeId).stream()
                .filter(track -> track.getTrackType() == trackType)
                .findFirst()
                .orElse(null);
    }

    public static final class Builder {
        private float duration = 0;
        private boolean holdLastKeyframe = false;
        private final Map<String, List<NodeAnimationTrack>> nodeAnimations = new HashMap<>();

        public Builder duration(float duration) {
            this.duration = duration;
            return this;
        }

        public Builder holdLastKeyframe(boolean hold) {
            this.holdLastKeyframe = hold;
            return this;
        }

        public Builder addNodeAnimations(String nodeId, List<NodeAnimationTrack> tracks) {
            this.nodeAnimations.put(nodeId, List.copyOf(tracks));
            return this;
        }

        public Builder addNodeAnimations(Map<String, List<NodeAnimationTrack>> animations) {
            this.nodeAnimations.putAll(animations);
            return this;
        }

        public BlockyAnimationDefinition build() {
            return new BlockyAnimationDefinition(duration, holdLastKeyframe, nodeAnimations);
        }
    }
}
