package com.litehed.hytalemodels.blocks.entity;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import static com.mojang.math.Constants.EPSILON;

public record NodeTransform(Vector3f position, Quaternionf rotation, Vector3f scale) {

    public NodeTransform(Vector3f position, Quaternionf rotation, Vector3f scale) {
        this.position = new Vector3f(position);
        this.rotation = new Quaternionf(rotation);
        this.scale = new Vector3f(scale);
    }

    public static NodeTransform translation(Vector3f position) {
        return new NodeTransform(position, new Quaternionf(), new Vector3f(1, 1, 1));
    }

    public static NodeTransform rotation(Quaternionf rotation) {
        return new NodeTransform(new Vector3f(), rotation, new Vector3f(1, 1, 1));
    }

    public static NodeTransform scale(Vector3f scale) {
        return new NodeTransform(new Vector3f(), new Quaternionf(), scale);
    }

    public static NodeTransform identity() {
        return new NodeTransform(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1));
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

    public boolean isIdentity() {
        return position.lengthSquared() < EPSILON &&
                rotation.equals(new Quaternionf(), EPSILON) &&
                Math.abs(scale.x - 1.0f) < EPSILON &&
                Math.abs(scale.y - 1.0f) < EPSILON &&
                Math.abs(scale.z - 1.0f) < EPSILON;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NodeTransform other)) return false;

        return position.equals(other.position, EPSILON) &&
                rotation.equals(other.rotation, EPSILON) &&
                scale.equals(other.scale, EPSILON);
    }
}