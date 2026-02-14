package com.litehed.hytalemodels.blockymodel.animations;

import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import com.litehed.hytalemodels.blockymodel.TransformCalculator;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class AnimationTransformCalculator {
    private static final float POSITION_SCALE = 32.0f;      // Convert from model units to block units
    private static final float POSITION_OFFSET_Y = 16.0f;   // Y-axis offset

    public static Transformation applyAnimationRotation(
            BlockyModelGeometry.BlockyNode node,
            Transformation baseTransform,
            Quaternionf animationRotation) {

        Vector3f pivotWorld = TransformCalculator.calculateWorldPosition(node);

        float pivotX = pivotWorld.x / POSITION_SCALE;
        float pivotY = (pivotWorld.y - POSITION_OFFSET_Y) / POSITION_SCALE;
        float pivotZ = pivotWorld.z / POSITION_SCALE;
        Vector3f pivotBlock = new Vector3f(pivotX, pivotY, pivotZ);

        Quaternionf worldOrientation = TransformCalculator.calculateWorldOrientation(node);

        Quaternionf combinedRotation = new Quaternionf(worldOrientation).mul(animationRotation);

        Transformation translateToPivot = new Transformation(
                pivotBlock,
                null,
                null,
                null
        );

        Transformation applyRotation = new Transformation(
                null,
                combinedRotation,
                null,
                null
        );

        Transformation translateBack = new Transformation(
                new Vector3f(-pivotBlock.x, -pivotBlock.y, -pivotBlock.z),
                null,
                null,
                null
        );

        // translateBack * applyRotation * translateToPivot * baseTransform
        return translateBack.compose(applyRotation).compose(translateToPivot).compose(baseTransform);
    }

    public static Transformation createPivotedAnimationTransform(
            BlockyModelGeometry.BlockyNode node,
            Quaternionf animationRotation,
            Vector3f animationTranslation) {

        Vector3f pivotWorld = node.getPosition();

        Vector3f pivotBlock = new Vector3f(
                pivotWorld.x / POSITION_SCALE,
                (pivotWorld.y - POSITION_OFFSET_Y) / POSITION_SCALE,
                pivotWorld.z / POSITION_SCALE
        );

        Vector3f finalTranslation = pivotBlock;
        if (animationTranslation != null) {
            finalTranslation = new Vector3f(pivotBlock).add(animationTranslation);
        }

        return new Transformation(
                finalTranslation,
                animationRotation,
                null,
                null
        );
    }

    public static Vector3f getPivotInBlockCoords(BlockyModelGeometry.BlockyNode node) {
        Vector3f pivotWorld = node.getPosition();
        return new Vector3f(
                pivotWorld.x / POSITION_SCALE,
                (pivotWorld.y - POSITION_OFFSET_Y) / POSITION_SCALE,
                pivotWorld.z / POSITION_SCALE
        );
    }
}
