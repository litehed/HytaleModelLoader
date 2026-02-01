package com.litehed.hytalemodels.modelstuff;

import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TransformCalculator {

    private static final float POSITION_SCALE = 32.0f;      // Convert from model units to block units
    private static final float POSITION_OFFSET_Y = 16.0f;   // Y-axis offset

    /**
     * Calculate the world-space position of a node
     * Recursively adds parent positions and applies parent orientations
     *
     * @param node the node to calculate the world position for
     * @return the world-space position of the node
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
     * Calculate the world-space orientation of a node
     * Recursively combines parent orientations
     *
     * @param node the node to calculate the world orientation for
     * @return the world-space orientation of the node
     */
    public static Quaternionf calculateWorldOrientation(BlockyModelGeometry.BlockyNode node) {
        Quaternionf worldRot = new Quaternionf(node.getOrientation());

        BlockyModelGeometry.BlockyNode current = node.getParent();
        while (current != null) {
            // Compose rotations: parent * child
            worldRot = new Quaternionf(current.getOrientation()).mul(worldRot);
            current = current.getParent();
        }

        return worldRot;
    }

    /**
     * Create a Transformation for a node given its world position, shape offset, and world orientation
     *
     * @param worldPosition    the world-space position of the node
     * @param shapeOffset      the offset of the shape relative to the node's position
     * @param worldOrientation the world-space orientation of the node
     * @return a Transformation object representing the node's transform in world space
     */
    public static Transformation createNodeTransform(Vector3f worldPosition, Vector3f shapeOffset,
                                                     Quaternionf worldOrientation) {

        Vector3f rotatedOffset = new Vector3f(shapeOffset);
        worldOrientation.transform(rotatedOffset);

        // Calculate center position in block coordinates
        float centerX = (worldPosition.x + rotatedOffset.x) / POSITION_SCALE;
        float centerY = (worldPosition.y + rotatedOffset.y - POSITION_OFFSET_Y) / POSITION_SCALE;
        float centerZ = (worldPosition.z + rotatedOffset.z) / POSITION_SCALE;

        return new Transformation(
                new Vector3f(centerX, centerY, centerZ),     // Translation
                worldOrientation,                            // Rotation
                new Vector3f(1),                          // Scale (no scaling)
                null                                         // No additional rotation
        );
    }

    /**
     * Calculate half-sizes for a shape given its full size
     *
     * @param size the full size of the shape
     * @return the half-sizes of the shape
     */
    public static Vector3f calculateHalfSizes(Vector3f size) {
        return new Vector3f(
                (size.x / 2) / POSITION_SCALE,
                (size.y / 2) / POSITION_SCALE,
                (size.z / 2) / POSITION_SCALE
        );
    }

}