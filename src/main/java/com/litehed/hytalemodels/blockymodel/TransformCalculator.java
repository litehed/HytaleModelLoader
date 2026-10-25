package com.litehed.hytalemodels.blockymodel;

import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TransformCalculator {

    // Model units in one block
    public static final float UNITS_PER_BLOCK = 32.0f;
    // Half a block in model units, taken off Y so models sit in the middle of a block
    public static final float ORIGIN_Y_OFFSET = 16.0f;

    /**
     * Position of a node in model space, including parents
     *
     * @param node the node
     * @return the position in model units
     */
    public static Vector3f calculateWorldPosition(BlockyModelGeometry.BlockyNode node) {
        Vector3f worldPos = new Vector3f(node.getPosition());

        BlockyModelGeometry.BlockyNode current = node.getParent();
        while (current != null) {
            Quaternionf parentOrientation = new Quaternionf(current.getOrientation());
            parentOrientation.transform(worldPos);

            worldPos.add(current.getPosition());
            if (current.hasShape()) {
                Vector3f parentOffset = new Vector3f(current.getShape().getOffset());
                parentOrientation.transform(parentOffset);
                worldPos.add(parentOffset);
            }

            current = current.getParent();
        }

        return worldPos;
    }

    /**
     * Orientation of a node in model space, including parents
     *
     * @param node the node to calculate the world orientation for
     * @return the world-space orientation of the node
     */
    public static Quaternionf calculateWorldOrientation(BlockyModelGeometry.BlockyNode node) {
        Quaternionf worldRot = new Quaternionf(node.getOrientation());

        BlockyModelGeometry.BlockyNode current = node.getParent();
        while (current != null) {
            worldRot = new Quaternionf(current.getOrientation()).mul(worldRot);
            current = current.getParent();
        }

        return worldRot;
    }

    /**
     * Transformation that places a node's shape in a block
     *
     * @param worldPosition    the position of the node in model units
     * @param shapeOffset      the offset of the shape from the node
     * @param worldOrientation the orientation of the node
     * @return the transformation, in blocks
     */
    public static Transformation createNodeTransform(Vector3f worldPosition, Vector3f shapeOffset,
                                                     Quaternionf worldOrientation) {

        Vector3f rotatedOffset = new Vector3f(shapeOffset);
        worldOrientation.transform(rotatedOffset);

        float centerX = (worldPosition.x + rotatedOffset.x) / UNITS_PER_BLOCK;
        float centerY = (worldPosition.y + rotatedOffset.y - ORIGIN_Y_OFFSET) / UNITS_PER_BLOCK;
        float centerZ = (worldPosition.z + rotatedOffset.z) / UNITS_PER_BLOCK;

        return new Transformation(new Vector3f(centerX, centerY, centerZ), worldOrientation, new Vector3f(1), null);
    }

    /**
     * Half of a shape's size, in blocks
     *
     * @param size the full size of the shape in model units
     * @return the half size in blocks
     */
    public static Vector3f calculateHalfSizes(Vector3f size) {
        return new Vector3f(
                (size.x / 2) / UNITS_PER_BLOCK,
                (size.y / 2) / UNITS_PER_BLOCK,
                (size.z / 2) / UNITS_PER_BLOCK
        );
    }
}