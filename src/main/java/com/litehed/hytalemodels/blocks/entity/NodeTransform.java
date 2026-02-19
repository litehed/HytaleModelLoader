package com.litehed.hytalemodels.blocks.entity;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import static com.mojang.math.Constants.EPSILON;

public record NodeTransform(Vector3f position, Quaternionf rotation, Vector3f scale, boolean visible) {

    public NodeTransform(Vector3f position, Quaternionf rotation, Vector3f scale, boolean visible) {
        this.position = new Vector3f(position);
        this.rotation = new Quaternionf(rotation);
        this.scale = new Vector3f(scale);
        this.visible = visible;
    }

    public static NodeTransform translation(Vector3f position) {
        return new NodeTransform(position, new Quaternionf(), new Vector3f(1, 1, 1), true);
    }

    public static NodeTransform rotation(Quaternionf rotation) {
        return new NodeTransform(new Vector3f(), rotation, new Vector3f(1, 1, 1), true);
    }

    public static NodeTransform scale(Vector3f scale) {
        return new NodeTransform(new Vector3f(), new Quaternionf(), scale, true);
    }

    public static NodeTransform visibility(boolean visible) {
        return new NodeTransform(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1), visible);
    }

    public static NodeTransform identity() {
        return new NodeTransform(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1), true);
    }

    @Override
    public Vector3f position() {
        return new Vector3f(position);
    }

    @Override
    public Quaternionf rotation() {
        return new Quaternionf(rotation);
    }

    @Override
    public Vector3f scale() {
        return new Vector3f(scale);
    }

    /** 
     * Merges this NodeTransform with another NodeTransform
     * 
     * @param other The other NodeTransform to merge with
     * @return The combined node transform
     */
    public NodeTransform merge(NodeTransform other) {
        Vector3f mergedPos = new Vector3f(this.position).add(other.position);
        Quaternionf mergedRot = new Quaternionf(this.rotation).mul(other.rotation);
        Vector3f mergedScale = new Vector3f(this.scale).mul(other.scale);
        boolean mergedVis = this.visible && other.visible;
        return new NodeTransform(mergedPos, mergedRot, mergedScale, mergedVis);
    }

    /**
     * Checks if this NodeTransform is the identity transform
     * @return true if this NodeTransform is the identity transform, false otherwise
     */
    public boolean isIdentity() {
        return position.lengthSquared() < EPSILON &&
                rotation.equals(new Quaternionf(), EPSILON) &&
                Math.abs(scale.x - 1.0f) < EPSILON &&
                Math.abs(scale.y - 1.0f) < EPSILON &&
                Math.abs(scale.z - 1.0f) < EPSILON &&
                visible;
    }
}