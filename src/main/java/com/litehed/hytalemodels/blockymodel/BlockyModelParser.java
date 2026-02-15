package com.litehed.hytalemodels.blockymodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.litehed.hytalemodels.HytaleModelLoader;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BlockyModelParser {

    private static final float DEFAULT_SIZE = 16.0f;
    private static final float DEFAULT_STRETCH = 1.0f;
    private static final Vector3f DEFAULT_POSITION = new Vector3f(0, 0, 0);
    private static final Quaternionf DEFAULT_ORIENTATION = new Quaternionf();

    /**
     * Parse the nodes from the root JsonObject
     *
     * @param root the root JsonObject of the BlockyModel
     * @return a list of parsed BlockyNodes
     * @throws JsonParseException if required fields are missing or invalid
     */
    public static List<BlockyModelGeometry.BlockyNode> parseNodes(JsonObject root) throws JsonParseException {
        if (!root.has("nodes")) {
            throw new JsonParseException("BlockyModel file must contain a 'nodes' array");
        }

        List<BlockyModelGeometry.BlockyNode> nodes = new ArrayList<>();
        JsonArray nodesArray = root.getAsJsonArray("nodes");

        for (JsonElement nodeElement : nodesArray) {
            parseNode(nodeElement.getAsJsonObject(), null, nodes);
        }

        return nodes;
    }

    /**
     * Parse a single node and its children recursively
     *
     * @param nodeObj  the JsonObject representing the node
     * @param parent   the parent BlockyNode, or null if root
     * @param allNodes the list to add parsed nodes to
     * @throws JsonParseException if required fields are missing or invalid
     */
    private static void parseNode(JsonObject nodeObj, BlockyModelGeometry.BlockyNode parent, List<BlockyModelGeometry.BlockyNode> allNodes) {
        validateRequiredFields(nodeObj);

        BlockyModelGeometry.BlockyNode node = new BlockyModelGeometry.BlockyNode(
                parseString(nodeObj, "id"),
                parseString(nodeObj, "name"),
                parsePosition(nodeObj),
                parseOrientation(nodeObj),
                parseShape(nodeObj),
                parent
        );

        allNodes.add(node);

        if (parent != null) {
            parent.addChild(node);
        }

        // Parse children recursively
        if (nodeObj.has("children")) {
            JsonArray children = nodeObj.getAsJsonArray("children");
            for (JsonElement childElement : children) {
                parseNode(childElement.getAsJsonObject(), node, allNodes);
            }
        }
    }

    /**
     * Validate that required fields are present in the node JsonObject
     *
     * @param nodeObj the JsonObject representing the node
     * @throws JsonParseException if required fields are missing
     */
    private static void validateRequiredFields(JsonObject nodeObj) throws JsonParseException {
        if (!nodeObj.has("name")) {
            throw new JsonParseException("Node missing required field: 'name'");
        }
        if (!nodeObj.has("id")) {
            throw new JsonParseException("Node missing required field: 'id'");
        }
    }

    /**
     * Parse a string field from a JsonObject
     *
     * @param obj the JsonObject
     * @param key the key of the string field
     * @return the string value
     */
    private static String parseString(JsonObject obj, String key) {
        return obj.get(key).getAsString();
    }

    /**
     * Parse the position vector from a node JsonObject
     *
     * @param nodeObj the JsonObject representing the node
     * @return the parsed position vector
     */
    private static Vector3f parsePosition(JsonObject nodeObj) {
        if (!nodeObj.has("position")) {
            return new Vector3f(DEFAULT_POSITION);
        }

        JsonObject pos = nodeObj.getAsJsonObject("position");
        return new Vector3f(
                getFloatOrDefault(pos, "x", 0),
                getFloatOrDefault(pos, "y", 0),
                getFloatOrDefault(pos, "z", 0)
        );
    }

    /**
     * Parse the orientation quaternion from a node JsonObject
     *
     * @param nodeObj the JsonObject representing the node
     * @return the parsed orientation quaternion
     */
    private static Quaternionf parseOrientation(JsonObject nodeObj) {
        if (!nodeObj.has("orientation")) {
            return new Quaternionf(DEFAULT_ORIENTATION);
        }

        JsonObject orient = nodeObj.getAsJsonObject("orientation");
        return new Quaternionf(
                getFloatOrDefault(orient, "x", 0),
                getFloatOrDefault(orient, "y", 0),
                getFloatOrDefault(orient, "z", 0),
                getFloatOrDefault(orient, "w", 1)
        );
    }

    /**
     * Parse the shape from a node
     *
     * @param nodeObj the JsonObject representing the node
     * @return the parsed BlockyShape, or null if none
     */
    private static BlockyModelGeometry.BlockyShape parseShape(JsonObject nodeObj) {
        if (!nodeObj.has("shape")) {
            return null;
        }

        JsonObject shapeObj = nodeObj.getAsJsonObject("shape");

        boolean visible = getBooleanOrDefault(shapeObj, "visible", true);
        if (!visible) {
            return BlockyModelGeometry.BlockyShape.invisible();
        }

        return new BlockyModelGeometry.BlockyShape(
                visible,
                getBooleanOrDefault(shapeObj, "doubleSided", false),
                parseOffset(shapeObj),
                parseStretch(shapeObj),
                parseSize(shapeObj),
                parseTextureLayout(shapeObj)
        );
    }

    /**
     * Parse the offset vector from a shape JsonObject
     *
     * @param shapeObj the JsonObject representing the shape
     * @return the parsed offset vector
     */
    private static Vector3f parseOffset(JsonObject shapeObj) {
        if (!shapeObj.has("offset")) {
            return new Vector3f(0, 0, 0);
        }

        JsonObject offset = shapeObj.getAsJsonObject("offset");
        return new Vector3f(
                getFloatOrDefault(offset, "x", 0),
                getFloatOrDefault(offset, "y", 0),
                getFloatOrDefault(offset, "z", 0)
        );
    }

    /**
     * Parse the stretch vector from a shape JsonObject
     *
     * @param shapeObj the JsonObject representing the shape
     * @return the parsed stretch vector
     */
    private static Vector3f parseStretch(JsonObject shapeObj) {
        if (!shapeObj.has("stretch")) {
            return new Vector3f(1, 1, 1);
        }

        JsonObject stretch = shapeObj.getAsJsonObject("stretch");
        return new Vector3f(
                getFloatOrDefault(stretch, "x", DEFAULT_STRETCH),
                getFloatOrDefault(stretch, "y", DEFAULT_STRETCH),
                getFloatOrDefault(stretch, "z", DEFAULT_STRETCH)
        );
    }

    /**
     * Parse the size vector from a shape JsonObject
     *
     * @param shapeObj the JsonObject representing the shape
     * @return the parsed size vector
     */
    private static Vector3f parseSize(JsonObject shapeObj) {
        if (!shapeObj.has("settings")) {
            return new Vector3f(DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE);
        }

        JsonObject settings = shapeObj.getAsJsonObject("settings");
        if (!settings.has("size")) {
            return new Vector3f(DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE);
        }

        JsonObject size = settings.getAsJsonObject("size");
        return new Vector3f(
                getFloatOrDefault(size, "x", DEFAULT_SIZE),
                getFloatOrDefault(size, "y", DEFAULT_SIZE),
                getFloatOrDefault(size, "z", DEFAULT_SIZE)
        );
    }

    /**
     * Parse the texture layout from a shape JsonObject
     *
     * @param shapeObj the JsonObject representing the shape
     * @return the parsed texture layout map
     */
    private static Map<Direction, BlockyModelGeometry.FaceTextureLayout> parseTextureLayout(JsonObject shapeObj) {
        Map<Direction, BlockyModelGeometry.FaceTextureLayout> layoutMap = new EnumMap<>(Direction.class);

        if (!shapeObj.has("textureLayout")) {
            return layoutMap;
        }

        JsonObject texLayout = shapeObj.getAsJsonObject("textureLayout");
        for (String dirName : texLayout.keySet()) {
            Direction dir = parseDirectionName(dirName);
            if (dir != null) {
                layoutMap.put(dir, parseFaceLayout(texLayout.getAsJsonObject(dirName)));
            }
        }

        return layoutMap;
    }

    /**
     * Parse a single face texture layout from a JsonObject
     *
     * @param faceLayout the JsonObject representing the face layout
     * @return the parsed FaceTextureLayout
     */
    private static BlockyModelGeometry.FaceTextureLayout parseFaceLayout(JsonObject faceLayout) {
        int offsetX = 0, offsetY = 0;
        boolean mirrorX = false, mirrorY = false;
        int angle = 0;

        if (faceLayout.has("offset")) {
            JsonObject offset = faceLayout.getAsJsonObject("offset");
            offsetX = getIntOrDefault(offset, "x", 0);
            offsetY = getIntOrDefault(offset, "y", 0);
        }

        if (faceLayout.has("mirror")) {
            JsonObject mirror = faceLayout.getAsJsonObject("mirror");
            mirrorX = getBooleanOrDefault(mirror, "x", false);
            mirrorY = getBooleanOrDefault(mirror, "y", false);
        }

        if (faceLayout.has("angle")) {
            angle = faceLayout.get("angle").getAsInt();
            validateAngle(angle);
        }

        return new BlockyModelGeometry.FaceTextureLayout(offsetX, offsetY, mirrorX, mirrorY, angle);
    }

    /**
     * Parse a direction name string to a Direction enum
     *
     * @param name the direction name string
     * @return the corresponding Direction enum, or null if unknown
     */
    private static Direction parseDirectionName(String name) {
        return switch (name.toLowerCase()) {
            case "front" -> Direction.SOUTH;
            case "back" -> Direction.NORTH;
            case "left" -> Direction.WEST;
            case "right" -> Direction.EAST;
            case "top" -> Direction.UP;
            case "bottom" -> Direction.DOWN;
            case "north", "south", "west", "east", "up", "down" -> Direction.valueOf(name.toUpperCase());
            default -> {
                HytaleModelLoader.LOGGER.warn("Unknown direction name: {}, skipping", name);
                yield null;
            }
        };
    }

    /**
     * Validate that the angle is one of the allowed values
     *
     * @param angle the angle to validate
     * @throws JsonParseException if the angle is invalid
     */
    private static void validateAngle(int angle) {
        if (angle != 0 && angle != 90 && angle != 180 && angle != 270) {
            throw new JsonParseException("Invalid angle: " + angle + ". Must be 0, 90, 180, or 270");
        }
    }

    // Utility methods to get values with defaults

    private static float getFloatOrDefault(JsonObject obj, String key, float defaultValue) {
        return obj.has(key) ? obj.get(key).getAsFloat() : defaultValue;
    }

    private static int getIntOrDefault(JsonObject obj, String key, int defaultValue) {
        return obj.has(key) ? obj.get(key).getAsInt() : defaultValue;
    }

    private static boolean getBooleanOrDefault(JsonObject obj, String key, boolean defaultValue) {
        return obj.has(key) ? obj.get(key).getAsBoolean() : defaultValue;
    }
}