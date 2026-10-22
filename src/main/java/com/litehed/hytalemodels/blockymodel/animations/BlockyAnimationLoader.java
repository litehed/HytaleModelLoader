package com.litehed.hytalemodels.blockymodel.animations;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.blockymodel.BlockyTokenizer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockyAnimationLoader implements ResourceManagerReloadListener {

    public static final BlockyAnimationLoader INSTANCE = new BlockyAnimationLoader();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(HytaleModelLoader.MODID, "blockyanim_loader");

    // Failures are cached as empty so they only log once
    private final Map<ResourceLocation, Optional<BlockyAnimationDefinition>> animationCache = new ConcurrentHashMap<>();

    /**
     * Loads an animation. Results are cached until the next resource reload
     *
     * @param animationId the full path of the .blockyanim file
     * @return the animation, or null if it is missing or invalid
     */
    public BlockyAnimationDefinition loadAnimation(ResourceLocation animationId) {
        return animationCache.computeIfAbsent(animationId, this::parseAnimation).orElse(null);
    }

    private Optional<BlockyAnimationDefinition> parseAnimation(ResourceLocation id) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);

        if (resource.isEmpty()) {
            HytaleModelLoader.LOGGER.warn("Animation resource not found: {}", id);
            return Optional.empty();
        }

        try (BlockyTokenizer tokenizer = new BlockyTokenizer(resource.get().open())) {
            return Optional.of(BlockyAnimParser.parse(tokenizer.getRoot()));
        } catch (Exception e) {
            HytaleModelLoader.LOGGER.error("Failed to parse animation '{}': {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        animationCache.clear();
    }
}