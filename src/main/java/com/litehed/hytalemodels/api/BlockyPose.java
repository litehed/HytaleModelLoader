package com.litehed.hytalemodels.api;

import com.litehed.hytalemodels.blockymodel.TransformCalculator;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationPlayer;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Transforms layered on a model's rest pose for one frame
 * Everything added stacks, so animation files and code can be mixed
 */
public final class BlockyPose {

    private final Map<String, NodeTransform> transforms = new HashMap<>();

    /**
     * Adds a rotation around all three axes, applied in the same order as vanilla's ModelPart (Z, then Y, then X).
     *
     * @param node the node name or id
     * @param x    rotation around X in radians
     * @param y    rotation around Y in radians
     * @param z    rotation around Z in radians
     * @return this pose
     */
    public BlockyPose rotate(String node, float x, float y, float z) {
        return add(node, NodeTransform.rotation(new Quaternionf().rotationZYX(z, y, x)));
    }

    public BlockyPose rotateX(String node, float radians) {
        return rotate(node, radians, 0f, 0f);
    }

    public BlockyPose rotateY(String node, float radians) {
        return rotate(node, 0f, radians, 0f);
    }

    public BlockyPose rotateZ(String node, float radians) {
        return rotate(node, 0f, 0f, radians);
    }

    /**
     * Adds a translation. The node's children move with it.
     *
     * @param node the node name or id
     * @param x    offset in model units
     * @param y    offset in model units
     * @param z    offset in model units
     * @return this pose
     */
    public BlockyPose translate(String node, float x, float y, float z) {
        return add(node, NodeTransform.translation(new Vector3f(x, y, z).div(TransformCalculator.UNITS_PER_BLOCK)));
    }

    /**
     * Multiplies the node's shape size. Unlike rotation and translation this does not affect children.
     *
     * @param node the node name or id
     * @param x    scale on X
     * @param y    scale on Y
     * @param z    scale on Z
     * @return this pose
     */
    public BlockyPose scale(String node, float x, float y, float z) {
        return add(node, NodeTransform.scale(new Vector3f(x, y, z)));
    }

    /**
     * Hides the node's shape. Children are not hidden.
     *
     * @param node the node name or id
     * @return this pose
     */
    public BlockyPose hide(String node) {
        return add(node, NodeTransform.visibility(false));
    }

    /**
     * Adds a transform to a node, combining it with anything already set for that node.
     *
     * @param node      the node name or id
     * @param transform the transform to add
     * @return this pose
     */
    public BlockyPose add(String node, NodeTransform transform) {
        transforms.merge(node, transform, NodeTransform::merge);
        return this;
    }

    /**
     * Plays a keyframe animation at full strength.
     *
     * @param player the animation, or null to do nothing
     * @param time   time in animation units, wrapped or clamped by the animation itself
     * @return this pose
     */
    public BlockyPose play(@Nullable BlockyAnimationPlayer player, float time) {
        return play(player, time, 1.0f);
    }

    /**
     * Plays a keyframe animation scaled by a weight
     *
     * @param player the animation, or null to do nothing
     * @param time   time in animation units
     * @param weight 0 for no effect up to 1 for full strength
     * @return this pose
     */
    public BlockyPose play(@Nullable BlockyAnimationPlayer player, float time, float weight) {
        if (player == null || weight <= 0.0f) {
            return this;
        }
        float w = Math.min(weight, 1.0f);
        player.calculateTransforms(time).forEach((node, transform) -> add(node, weighted(transform, w)));
        return this;
    }

    /**
     * Plays a keyframe animation that replaces whatever is already set for the nodes it animates, instead of
     * adding to it
     *
     * @param player the animation, or null to do nothing
     * @param time   time in animation units
     * @return this pose
     */
    public BlockyPose playOverride(@Nullable BlockyAnimationPlayer player, float time) {
        if (player != null) {
            transforms.putAll(player.calculateTransforms(time));
        }
        return this;
    }

    /**
     * Looks up the combined transform for a node. Used by the renderer.
     *
     * @param id   the node's id
     * @param name the node's name
     * @return the transform, or null if the node has none
     */
    public @Nullable NodeTransform resolve(String id, String name) {
        NodeTransform byName = transforms.get(name);
        if (id.equals(name)) {
            return byName;
        }
        NodeTransform byId = transforms.get(id);
        if (byName == null) {
            return byId;
        }
        return byId == null ? byName : byName.merge(byId);
    }

    private static NodeTransform weighted(NodeTransform t, float weight) {
        if (weight >= 1.0f) {
            return t;
        }
        Vector3f position = t.position().mul(weight);
        Quaternionf rotation = new Quaternionf().slerp(t.rotation(), weight);
        Vector3f scale = new Vector3f(1, 1, 1).lerp(t.scale(), weight);
        return new NodeTransform(position, rotation, scale, t.visible());
    }
}