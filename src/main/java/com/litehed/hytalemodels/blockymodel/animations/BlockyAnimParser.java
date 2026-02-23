package com.litehed.hytalemodels.blockymodel.animations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.litehed.hytalemodels.HytaleModelLoader;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.litehed.hytalemodels.blockymodel.ParserUtil.getBooleanOrDefault;
import static com.litehed.hytalemodels.blockymodel.ParserUtil.getFloatOrDefault;

public final class BlockyAnimParser {

    public static BlockyAnimationDefinition parse(JsonObject root) throws JsonParseException {
        validateRequiredFields(root);

//        int formatVersion = getIntOrDefault(root, "formatVersion", 1); idk if this will have a use, commented for now
        float duration = root.get("duration").getAsFloat();
        boolean holdLastKeyframe = getBooleanOrDefault(root, "holdLastKeyframe", false);

        return new BlockyAnimationDefinition.Builder()
                .duration(duration)
                .holdLastKeyframe(holdLastKeyframe)
                .addNodeAnimations(parseNodeAnimations(root))
                .build();
    }

    private static void validateRequiredFields(JsonObject root) throws JsonParseException {
        if (!root.has("duration")) {
            throw new JsonParseException("Animation file missing required field: 'duration'");
        }
        if (!root.has("nodeAnimations")) {
            throw new JsonParseException("Animation file missing required field: 'nodeAnimations'");
        }
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
            if (keyframesArray.isEmpty()) continue; // Skip empty tracks

            List<BlockyKeyframe> keyframes = parseKeyframes(trackType, keyframesArray);
            if (!keyframes.isEmpty()) {
                tracks.add(new NodeAnimationTrack(nodeId, trackType, keyframes));
            }
        }

        return tracks;
    }

    private static List<BlockyKeyframe> parseKeyframes(
            NodeAnimationTrack.AnimationTrackType trackType,
            JsonArray keyframesArray) {

        List<BlockyKeyframe> keyframes = new ArrayList<>();

        for (JsonElement element : keyframesArray) {
            JsonObject keyframeObj = element.getAsJsonObject();
            float time = keyframeObj.get("time").getAsFloat();
            BlockyKeyframe.InterpolationType interpolationType = parseInterpolationType(keyframeObj);

            BlockyKeyframe keyframe = switch (trackType) {
                case POSITION -> parsePositionKeyframe(time, keyframeObj, interpolationType);
                case ORIENTATION -> parseOrientationKeyframe(time, keyframeObj, interpolationType);
                case SHAPE_STRETCH -> parseScaleKeyframe(time, keyframeObj, interpolationType);
                case SHAPE_VISIBLE -> parseVisibilityKeyframe(time, keyframeObj, interpolationType);
                case SHAPE_UV_OFFSET -> null;
            };

            if (keyframe != null) {
                keyframes.add(keyframe);
            }
        }

        return keyframes;
    }

    private static BlockyKeyframe.PositionKeyframe parsePositionKeyframe(
            float time, JsonObject kfObj, BlockyKeyframe.InterpolationType interp) {

        Vector3f delta = parseVector3f(kfObj.getAsJsonObject("delta"));
        return new BlockyKeyframe.PositionKeyframe(time, delta, interp);
    }

    private static BlockyKeyframe.OrientationKeyframe parseOrientationKeyframe(
            float time,
            JsonObject keyframeObj,
            BlockyKeyframe.InterpolationType interpolationType) {

        JsonObject deltaObj = keyframeObj.getAsJsonObject("delta");
        Quaternionf delta = new Quaternionf(
                getFloatOrDefault(deltaObj, "x", 0),
                getFloatOrDefault(deltaObj, "y", 0),
                getFloatOrDefault(deltaObj, "z", 0),
                getFloatOrDefault(deltaObj, "w", 1)
        );

        return new BlockyKeyframe.OrientationKeyframe(time, delta, interpolationType);
    }

    private static BlockyKeyframe.ScaleKeyframe parseScaleKeyframe(
            float time, JsonObject kfObj, BlockyKeyframe.InterpolationType interp) {

        Vector3f delta = parseVector3f(kfObj.getAsJsonObject("delta"));
        return new BlockyKeyframe.ScaleKeyframe(time, delta, interp);
    }

    private static BlockyKeyframe.VisibilityKeyframe parseVisibilityKeyframe(
            float time, JsonObject kfObj, BlockyKeyframe.InterpolationType interp) {

        boolean visible = kfObj.get("delta").getAsBoolean();
        return new BlockyKeyframe.VisibilityKeyframe(time, visible, interp);
    }

    private static Vector3f parseVector3f(JsonObject obj) {
        return new Vector3f(
                getFloatOrDefault(obj, "x", 0f),
                getFloatOrDefault(obj, "y", 0f),
                getFloatOrDefault(obj, "z", 0f)
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
