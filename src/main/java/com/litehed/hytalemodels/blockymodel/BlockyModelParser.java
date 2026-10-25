package com.litehed.hytalemodels.blockymodel;

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

import static com.litehed.hytalemodels.blockymodel.ParserUtil.*;

public final class BlockyModelParser {

    private static final float DEFAULT_SIZE = 16.0f;

    /**
     * Parses every node of a model file, children included
     *
     * @param root the root of the model file
     * @return all nodes, with each parent before its children
     * @throws JsonParseException if the file has no nodes or a node is missing a name or id
     */
    public static List<BlockyModelGeometry.BlockyNode> parseNodes(JsonObject root) throws JsonParseException {
        if (!root.has("nodes")) {
            throw new JsonParseException("BlockyModel file must contain a 'nodes' array");
        }

        List<BlockyModelGeometry.BlockyNode> nodes = new ArrayList<>();
        for (JsonElement nodeElement : root.getAsJsonArray("nodes")) {
            parseNode(nodeElement.getAsJsonObject(), null, nodes);
        }
        return nodes;
    }

    private static void parseNode(JsonObject nodeObj, BlockyModelGeometry.BlockyNode parent,
                                  List<BlockyModelGeometry.BlockyNode> allNodes) {
        if (!nodeObj.has("name")) {
            throw new JsonParseException("Node missing required field: 'name'");
        }
        if (!nodeObj.has("id")) {
            throw new JsonParseException("Node missing required field: 'id'");
        }

        BlockyModelGeometry.BlockyNode node = new BlockyModelGeometry.BlockyNode(
                nodeObj.get("id").getAsString(),
                nodeObj.get("name").getAsString(),
                parseVector(nodeObj, "position", 0, 0, 0),
                parseOrientation(nodeObj),
                parseShape(nodeObj),
                parent
        );

        allNodes.add(node);
        if (parent != null) {
            parent.addChild(node);
        }

        if (nodeObj.has("children")) {
            for (JsonElement childElement : nodeObj.getAsJsonArray("children")) {
                parseNode(childElement.getAsJsonObject(), node, allNodes);
            }
        }
    }

    // Reads an object with x, y and z, using the given values for any that are missing
    private static Vector3f parseVector(JsonObject parent, String key, float defaultX, float defaultY, float defaultZ) {
        if (!parent.has(key)) {
            return new Vector3f(defaultX, defaultY, defaultZ);
        }

        JsonObject obj = parent.getAsJsonObject(key);
        return new Vector3f(
                getFloatOrDefault(obj, "x", defaultX),
                getFloatOrDefault(obj, "y", defaultY),
                getFloatOrDefault(obj, "z", defaultZ)
        );
    }

    private static Quaternionf parseOrientation(JsonObject nodeObj) {
        if (!nodeObj.has("orientation")) {
            return new Quaternionf();
        }

        JsonObject orient = nodeObj.getAsJsonObject("orientation");
        return new Quaternionf(
                getFloatOrDefault(orient, "x", 0),
                getFloatOrDefault(orient, "y", 0),
                getFloatOrDefault(orient, "z", 0),
                getFloatOrDefault(orient, "w", 1)
        );
    }

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
                parseVector(shapeObj, "offset", 0, 0, 0),
                parseVector(shapeObj, "stretch", 1, 1, 1),
                parseSize(shapeObj),
                parseTextureLayout(shapeObj, parseFrontFace(shapeObj))
        );
    }

    // Boxes have their front on south, quads on the side their normal points to
    private static Direction parseFrontFace(JsonObject shapeObj) {
        String type = shapeObj.has("type") ? shapeObj.get("type").getAsString() : "box";
        if (!type.equalsIgnoreCase("quad")) {
            return Direction.SOUTH;
        }

        String normal = "+Z";
        if (shapeObj.has("settings")) {
            JsonObject settings = shapeObj.getAsJsonObject("settings");
            if (settings.has("normal")) {
                normal = settings.get("normal").getAsString();
            }
        }

        return switch (normal.toUpperCase()) {
            case "+Z" -> Direction.SOUTH;
            case "-Z" -> Direction.NORTH;
            default -> {
                HytaleModelLoader.LOGGER.warn("Unsupported quad normal '{}', assuming +Z", normal);
                yield Direction.SOUTH;
            }
        };
    }

    // Quads have no depth, so a size that is given without z gets 0
    private static Vector3f parseSize(JsonObject shapeObj) {
        JsonObject settings = shapeObj.has("settings") ? shapeObj.getAsJsonObject("settings") : null;
        if (settings == null || !settings.has("size")) {
            return new Vector3f(DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE);
        }
        return parseVector(settings, "size", DEFAULT_SIZE, DEFAULT_SIZE, 0);
    }

    private static Map<Direction, BlockyModelGeometry.FaceTextureLayout> parseTextureLayout(JsonObject shapeObj,
                                                                                            Direction frontFace) {
        Map<Direction, BlockyModelGeometry.FaceTextureLayout> layoutMap = new EnumMap<>(Direction.class);

        if (!shapeObj.has("textureLayout")) {
            return layoutMap;
        }

        JsonObject texLayout = shapeObj.getAsJsonObject("textureLayout");
        for (String dirName : texLayout.keySet()) {
            Direction dir = parseDirectionName(dirName, frontFace);
            if (dir != null) {
                layoutMap.put(dir, parseFaceLayout(texLayout.getAsJsonObject(dirName)));
            }
        }

        return layoutMap;
    }

    private static BlockyModelGeometry.FaceTextureLayout parseFaceLayout(JsonObject faceLayout) {
        int offsetX = 0, offsetY = 0;
        boolean mirrorX = false, mirrorY = false;

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

        int angle = getIntOrDefault(faceLayout, "angle", 0);
        return new BlockyModelGeometry.FaceTextureLayout(offsetX, offsetY, mirrorX, mirrorY, angle);
    }

    private static Direction parseDirectionName(String name, Direction frontFace) {
        return switch (name.toLowerCase()) {
            case "front" -> frontFace;
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
}