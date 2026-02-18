package com.litehed.hytalemodels.blockymodel;

import com.google.common.collect.Lists;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
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
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

import static com.litehed.hytalemodels.blockymodel.QuadBuilder.DEBUG_BORDERS;

public class BlockyModelGeometry implements ExtendedUnbakedGeometry {

    private final List<BlockyNode> nodes;
    private final Identifier modelLocation;

    public BlockyModelGeometry(Settings settings) {
        this.nodes = Lists.newArrayList();
        this.modelLocation = settings.modelLocation();
    }

    public BlockyModelGeometry(List<BlockyNode> nodes, Identifier modelLocation) {
        this.nodes = nodes;
        this.modelLocation = modelLocation;
    }

    /**
     * Parses a BlockyModelGeometry from the given tokenizer and settings
     *
     * @param tokenizer the tokenizer to use for parsing
     * @param settings  the settings to use for parsing
     * @return a new BlockyModelGeometry instance
     */
    public static BlockyModelGeometry parse(BlockyTokenizer tokenizer, Settings settings) {
        List<BlockyNode> nodes = BlockyModelParser.parseNodes(tokenizer.getRoot());
        return new BlockyModelGeometry(nodes, settings.modelLocation());
    }


    /**
     * Bakes the model into a QuadCollection for rendering
     *
     * @param textureSlots   the texture slots for this model
     * @param modelBaker     the model baker instance
     * @param modelState     the model state (transformations)
     * @param modelDebugName the debug name for this model
     * @param contextMap     the context map for additional properties
     * @return the baked QuadCollection
     */
    @Override
    public QuadCollection bake(TextureSlots textureSlots, ModelBaker modelBaker, ModelState modelState, ModelDebugName modelDebugName, ContextMap contextMap) {
        QuadCollection.Builder builder = new QuadCollection.Builder();
        Transformation rootTransform = contextMap.getOrDefault(
                NeoForgeModelProperties.TRANSFORM,
                Transformation.identity()
        );

        Transformation finalTransform = rootTransform.isIdentity()
                ? modelState.transformation()
                : modelState.transformation().compose(rootTransform);

        for (BlockyNode node : nodes) {
            if (node.hasShape()) {
                bakeNode(builder, node, textureSlots, modelBaker, finalTransform, modelDebugName);
            }
        }

        return builder.build();
    }

    /**
     * Bakes a single BlockyNode into the QuadCollection builder
     *
     * @param builder        the QuadCollection.Builder to add quads to
     * @param node           the BlockyNode to bake
     * @param textureSlots   the texture slots for this model
     * @param modelBaker     the model baker instance
     * @param modelTransform the combined transformation for this model
     * @param modelDebugName the debug name for this model
     */
    private void bakeNode(QuadCollection.Builder builder, BlockyNode node,
                          TextureSlots textureSlots, ModelBaker modelBaker,
                          Transformation modelTransform, ModelDebugName modelDebugName) {

        BlockyShape shape = node.getShape();
        TextureAtlasSprite sprite = modelBaker.sprites()
                .resolveSlot(textureSlots, "texture", modelDebugName);

        Vector3f worldPos = TransformCalculator.calculateWorldPosition(node);
        Quaternionf worldRot = TransformCalculator.calculateWorldOrientation(node);
        Transformation nodeTransform = TransformCalculator.createNodeTransform(
                worldPos, shape.getOffset(), worldRot
        );

        // Translate after rotation
        Transformation centerTranslate = new Transformation(
                new Vector3f(0.5f, 0.5f, 0.5f), null, null, null
        );

        Transformation finalTransform;
        if (modelTransform.isIdentity()) {
            finalTransform = centerTranslate.compose(nodeTransform);
        } else {
            finalTransform = centerTranslate.compose(modelTransform).compose(nodeTransform);
        }

        // Bounds
        Vector3f halfSizes = TransformCalculator.calculateHalfSizes(shape.getSize());
        Vector3f min = new Vector3f(-halfSizes.x, -halfSizes.y, -halfSizes.z);
        Vector3f max = new Vector3f(halfSizes.x, halfSizes.y, halfSizes.z);

        // Generate quads for each face
        for (Direction direction : Direction.values()) {
            if (!shape.hasTextureLayout(direction)) {
                continue;
            }

            FaceTextureLayout texLayout = shape.getTextureLayout(direction);

            boolean shouldReverse = shape.needsWindingReversal();

            Pair<BakedQuad, Direction> quad;
            if (shouldReverse) {
                quad = QuadBuilder.createReversedQuad(
                        direction, min, max, sprite, texLayout, shape.getOriginalSize(), finalTransform);
            } else {
                quad = QuadBuilder.createQuad(
                        direction, min, max, sprite, texLayout, shape.getOriginalSize(), finalTransform);
            }

            addQuadToBuilder(builder, quad);

            // Debug quads
            if (DEBUG_BORDERS) {
                List<BakedQuad> borderQuads = QuadBuilder.createBorderQuads(
                        direction, min, max, sprite, finalTransform
                );
                for (BakedQuad borderQuad : borderQuads) {
                    builder.addUnculledFace(borderQuad);
                }
            }

            // Backface if double-sided
            if (shape.isDoubleSided()) {
                Pair<BakedQuad, Direction> backQuad;
                if (shouldReverse) {
                    // If winding is reversed, backface should be normal
                    backQuad = QuadBuilder.createQuad(
                            direction, min, max, sprite, texLayout, shape.getOriginalSize(), finalTransform);
                } else {
                    backQuad = QuadBuilder.createReversedQuad(
                            direction, min, max, sprite, texLayout, shape.getOriginalSize(), finalTransform);
                }
                builder.addUnculledFace(backQuad.getLeft());
            }
        }
    }

    /**
     * Adds a quad to the QuadCollection builder, handling culling
     *
     * @param builder the QuadCollection.Builder to add to
     * @param quad    the quad and its culling direction
     */
    private void addQuadToBuilder(QuadCollection.Builder builder, Pair<BakedQuad, Direction> quad) {
        if (quad.getRight() == null) {
            builder.addUnculledFace(quad.getLeft());
        } else {
            builder.addCulledFace(quad.getRight(), quad.getLeft());
        }
    }

    public List<BlockyNode> getNodes() {
        return nodes;
    }

    public record Settings(Identifier modelLocation) {
        public Identifier modelLocation() {
            return this.modelLocation;
        }
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
            // Defensive copies for mutable objects
            this.position = new Vector3f(position);
            this.orientation = new Quaternionf(orientation);
            this.shape = shape;
            this.parent = parent;
            this.children = new ArrayList<>();
        }

        // Getters only - no setters (immutable)
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
        private final Vector3f stretch;
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

            this.stretch = new Vector3f(stretch);
            this.originalSize = new Vector3f(size);
            this.size = new Vector3f(
                    size.x * stretch.x,
                    size.y * stretch.y,
                    size.z * stretch.z
            );

            int negativeCount = 0;
            if (stretch.x < 0) negativeCount++;
            if (stretch.y < 0) negativeCount++;
            if (stretch.z < 0) negativeCount++;
            this.needsWindingReversal = (negativeCount % 2) == 1;

            // Immutable map
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

        // Getters
        public boolean isVisible() {
            return visible;
        }

        public boolean isDoubleSided() {
            return doubleSided;
        }

        public Vector3f getOffset() {
            return new Vector3f(offset);
        }

        public Vector3f getStretch() {
            return stretch;
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

        public static FaceTextureLayout defaultLayout() {
            return new FaceTextureLayout(0, 0, false, false, 0);
        }
    }
}
