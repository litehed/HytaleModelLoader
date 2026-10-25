package com.litehed.hytalemodels.blockymodel;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class BlockyModelGeometry implements ExtendedUnbakedGeometry {

    private final List<BlockyNode> nodes;

    public BlockyModelGeometry(List<BlockyNode> nodes) {
        this.nodes = nodes;
    }

    /**
     * Parses a model file
     *
     * @param tokenizer the tokenizer holding the model file
     * @return the parsed geometry
     */
    public static BlockyModelGeometry parse(BlockyTokenizer tokenizer) {
        return new BlockyModelGeometry(BlockyModelParser.parseNodes(tokenizer.getRoot()));
    }

    @Override
    public QuadCollection bake(TextureSlots textureSlots, ModelBaker modelBaker, ModelState modelState,
                               ModelDebugName modelDebugName, ContextMap contextMap) {
        QuadCollection.Builder builder = new QuadCollection.Builder();
        Transformation rootTransform = contextMap.getOrDefault(
                NeoForgeModelProperties.TRANSFORM,
                Transformation.identity()
        );

        Transformation finalTransform = rootTransform.isIdentity()
                ? modelState.transformation()
                : modelState.transformation().compose(rootTransform);

        TextureAtlasSprite sprite = modelBaker.sprites().resolveSlot(textureSlots, "texture", modelDebugName);

        for (BlockyNode node : nodes) {
            if (node.hasShape()) {
                bakeNode(builder, node, sprite, finalTransform);
            }
        }

        return builder.build();
    }

    private void bakeNode(QuadCollection.Builder builder, BlockyNode node, TextureAtlasSprite sprite,
                          Transformation modelTransform) {
        BlockyShape shape = node.getShape();

        Vector3f worldPos = TransformCalculator.calculateWorldPosition(node);
        Quaternionf worldRot = TransformCalculator.calculateWorldOrientation(node);
        Transformation nodeTransform = TransformCalculator.createNodeTransform(
                worldPos, shape.getOffset(), worldRot
        );

        // Translate after rotation
        Transformation centerTranslate = new Transformation(
                new Vector3f(0.5f, 0.5f, 0.5f), null, null, null
        );

        Transformation finalTransform = modelTransform.isIdentity()
                ? centerTranslate.compose(nodeTransform)
                : centerTranslate.compose(modelTransform).compose(nodeTransform);

        Vector3f halfSizes = TransformCalculator.calculateHalfSizes(shape.getSize());
        Vector3f min = new Vector3f(-halfSizes.x, -halfSizes.y, -halfSizes.z);
        Vector3f max = new Vector3f(halfSizes.x, halfSizes.y, halfSizes.z);

        boolean reversed = shape.needsWindingReversal();

        for (Direction direction : Direction.values()) {
            if (!shape.hasTextureLayout(direction)) {
                continue;
            }

            FaceTextureLayout texLayout = shape.getTextureLayout(direction);

            builder.addUnculledFace(QuadBuilder.createQuad(
                    direction, min, max, sprite, texLayout, shape.getOriginalSize(), finalTransform, reversed));

            // Back face winds the other way
            if (shape.isDoubleSided()) {
                builder.addUnculledFace(QuadBuilder.createQuad(
                        direction, min, max, sprite, texLayout, shape.getOriginalSize(), finalTransform, !reversed));
            }
        }
    }

    public List<BlockyNode> getNodes() {
        return nodes;
    }

    public record Settings(Identifier modelLocation) {
    }

    public static final class BlockyNode {
        private final String id;
        private final String name;
        private final Vector3f position;
        private final Quaternionf orientation;
        private final BlockyShape shape;
        private final BlockyNode parent;
        private final List<BlockyNode> children;

        public BlockyNode(String id, String name, Vector3f position, Quaternionf orientation,
                          BlockyShape shape, BlockyNode parent) {
            this.id = id;
            this.name = name;
            this.position = new Vector3f(position);
            this.orientation = new Quaternionf(orientation);
            this.shape = shape;
            this.parent = parent;
            this.children = new ArrayList<>();
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Vector3f getPosition() {
            return new Vector3f(position);
        }

        public Quaternionf getOrientation() {
            return new Quaternionf(orientation);
        }

        public BlockyShape getShape() {
            return shape;
        }

        public BlockyNode getParent() {
            return parent;
        }

        public List<BlockyNode> getChildren() {
            return Collections.unmodifiableList(children);
        }

        void addChild(BlockyNode child) {
            this.children.add(child);
        }

        public boolean hasShape() {
            return shape != null && shape.isVisible();
        }

        @Override
        public String toString() {
            return "BlockyNode{id='" + id + "', name='" + name + "'}";
        }
    }

    public static final class BlockyShape {
        private final boolean visible;
        private final boolean doubleSided;
        private final Vector3f offset;
        private final boolean needsWindingReversal;
        private final Vector3f originalSize;
        private final Vector3f size;
        private final Map<Direction, FaceTextureLayout> textureLayout;

        public BlockyShape(boolean visible, boolean doubleSided, Vector3f offset,
                           Vector3f stretch, Vector3f size,
                           Map<Direction, FaceTextureLayout> textureLayout) {
            this.visible = visible;
            this.doubleSided = doubleSided;
            this.offset = new Vector3f(offset);
            this.originalSize = new Vector3f(size);
            this.size = new Vector3f(
                    size.x * stretch.x,
                    size.y * stretch.y,
                    size.z * stretch.z
            );

            // An odd number of negative stretches turns the faces inside out
            int negativeCount = 0;
            if (stretch.x < 0) negativeCount++;
            if (stretch.y < 0) negativeCount++;
            if (stretch.z < 0) negativeCount++;
            this.needsWindingReversal = (negativeCount % 2) == 1;

            this.textureLayout = Collections.unmodifiableMap(
                    new EnumMap<>(textureLayout)
            );
        }

        public static BlockyShape invisible() {
            return new BlockyShape(
                    false, false,
                    new Vector3f(0, 0, 0),
                    new Vector3f(1, 1, 1),
                    new Vector3f(16, 16, 16),
                    new EnumMap<>(Direction.class)
            );
        }

        public boolean isVisible() {
            return visible;
        }

        public boolean isDoubleSided() {
            return doubleSided;
        }

        public Vector3f getOffset() {
            return new Vector3f(offset);
        }

        public boolean needsWindingReversal() {
            return needsWindingReversal;
        }

        public Vector3f getOriginalSize() {
            return originalSize;
        }

        public Vector3f getSize() {
            return new Vector3f(size);
        }

        public FaceTextureLayout getTextureLayout(Direction face) {
            return textureLayout.get(face);
        }

        public boolean hasTextureLayout(Direction face) {
            return textureLayout.containsKey(face);
        }
    }

    public record FaceTextureLayout(int offsetX, int offsetY, boolean mirrorX, boolean mirrorY, int angle) {
        public FaceTextureLayout {
            if (angle != 0 && angle != 90 && angle != 180 && angle != 270) {
                throw new IllegalArgumentException("Angle must be 0, 90, 180, or 270, got: " + angle);
            }
        }

    }
}