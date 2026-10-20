package com.litehed.hytalemodels.blockymodel.animations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.litehed.hytalemodels.HytaleModelLoader;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

import static com.litehed.hytalemodels.blockymodel.ParserUtil.getBooleanOrDefault;
import static com.litehed.hytalemodels.blockymodel.ParserUtil.getFloatOrDefault;

public final class BlockyAnimParser {

    /**
     * Parses an animation file
     *
     * @param root the root of the animation file
     * @return the animation
     * @throws JsonParseException if the file is missing its duration or node animations, or a keyframe is incomplete
     */
    public static BlockyAnimationDefinition parse(JsonObject root) throws JsonParseException {
        if (!root.has("duration")) {
            throw new JsonParseException("Animation file missing required field: 'duration'");
        }
        if (!root.has("nodeAnimations")) {
            throw new JsonParseException("Animation file missing required field: 'nodeAnimations'");
        }

        return new BlockyAnimationDefinition.Builder()
                .duration(root.get("duration").getAsFloat())
                .holdLastKeyframe(getBooleanOrDefault(root, "holdLastKeyframe", false))
                .addNodeAnimations(parseNodeAnimations(root))
                .build();
    }

    private static Map<String, List<NodeAnimationTrack>> parseNodeAnimations(JsonObject root) {
        Map<String, List<NodeAnimationTrack>> result = new HashMap<>();

        JsonObject nodeAnimationsObj = root.getAsJsonObject("nodeAnimations");
        for (String nodeId : nodeAnimationsObj.keySet()) {
            List<NodeAnimationTrack> tracks = parseNodeAnimationTracks(nodeId, nodeAnimationsObj.getAsJsonObject(nodeId));
            if (!tracks.isEmpty()) {
                result.put(nodeId, tracks);
            }
        }

        return result;
    }

    private static List<NodeAnimationTrack> parseNodeAnimationTracks(String nodeId, JsonObject nodeObj) {
        List<NodeAnimationTrack> tracks = new ArrayList<>();

        for (String trackKey : nodeObj.keySet()) {
            NodeAnimationTrack.AnimationTrackType trackType = NodeAnimationTrack.AnimationTrackType.fromJsonKey(trackKey);
            if (trackType == null) {
                HytaleModelLoader.LOGGER.warn("Unknown animation track type: {}, skipping", trackKey);
                continue;
            }

            JsonArray keyframesArray = nodeObj.getAsJsonArray(trackKey);
            if (keyframesArray.isEmpty()) continue;

            List<BlockyKeyframe> keyframes = parseKeyframes(trackType, keyframesArray);
            if (!keyframes.isEmpty()) {
                tracks.add(new NodeAnimationTrack(nodeId, trackType, keyframes));
            }
        }

        return tracks;
    }

    private static List<BlockyKeyframe> parseKeyframes(NodeAnimationTrack.AnimationTrackType trackType,
                                                       JsonArray keyframesArray) {
        List<BlockyKeyframe> keyframes = new ArrayList<>();

        for (JsonElement element : keyframesArray) {
            JsonObject keyframeObj = element.getAsJsonObject();
            if (!keyframeObj.has("time") || !keyframeObj.has("delta")) {
                throw new JsonParseException("Keyframe missing required field: 'time' or 'delta'");
            }

            float time = keyframeObj.get("time").getAsFloat();
            JsonElement delta = keyframeObj.get("delta");
            BlockyKeyframe.InterpolationType interpolation = parseInterpolationType(keyframeObj);

            BlockyKeyframe keyframe = switch (trackType) {
                case POSITION ->
                        new BlockyKeyframe.PositionKeyframe(time, parseVector3f(delta.getAsJsonObject()), interpolation);
                case ORIENTATION ->
                        new BlockyKeyframe.OrientationKeyframe(time, parseQuaternion(delta.getAsJsonObject()), interpolation);
                case SHAPE_STRETCH ->
                        new BlockyKeyframe.ScaleKeyframe(time, parseVector3f(delta.getAsJsonObject()), interpolation);
                case SHAPE_VISIBLE -> new BlockyKeyframe.VisibilityKeyframe(time, delta.getAsBoolean(), interpolation);
                case SHAPE_UV_OFFSET -> null; // Not supported yet
            };

            if (keyframe != null) {
                keyframes.add(keyframe);
            }
        }

        // The player expects keyframes in time order
        keyframes.sort(Comparator.comparingDouble(BlockyKeyframe::getTime));
        return keyframes;
    }

    private static Vector3f parseVector3f(JsonObject obj) {
        return new Vector3f(
                getFloatOrDefault(obj, "x", 0f),
                getFloatOrDefault(obj, "y", 0f),
                getFloatOrDefault(obj, "z", 0f)
        );
    }

    private static Quaternionf parseQuaternion(JsonObject obj) {
        return new Quaternionf(
                getFloatOrDefault(obj, "x", 0),
                getFloatOrDefault(obj, "y", 0),
                getFloatOrDefault(obj, "z", 0),
                getFloatOrDefault(obj, "w", 1)
        );
    }

    private static BlockyKeyframe.InterpolationType parseInterpolationType(JsonObject keyframeObj) {
        if (!keyframeObj.has("interpolationType")) {
            return BlockyKeyframe.InterpolationType.SMOOTH;
        }

        String typeStr = keyframeObj.get("interpolationType").getAsString().toLowerCase();
        return switch (typeStr) {
            case "linear" -> BlockyKeyframe.InterpolationType.LINEAR;
            case "smooth" -> BlockyKeyframe.InterpolationType.SMOOTH;
            default -> {
                HytaleModelLoader.LOGGER.warn("Unknown interpolation type: '{}', defaulting to SMOOTH", typeStr);
                yield BlockyKeyframe.InterpolationType.SMOOTH;
            }
        };
    }
}