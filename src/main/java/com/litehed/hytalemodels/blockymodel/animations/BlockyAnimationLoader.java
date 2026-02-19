package com.litehed.hytalemodels.blockymodel.animations;

import com.google.common.collect.Maps;
import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.blockymodel.BlockyTokenizer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.util.Map;
import java.util.Optional;

public final class BlockyAnimationLoader implements ResourceManagerReloadListener {

    public static final BlockyAnimationLoader INSTANCE = new BlockyAnimationLoader();

    private final Map<Identifier, BlockyAnimationDefinition> animationCache = Maps.newConcurrentMap();

    public BlockyAnimationDefinition loadAnimation(Identifier animationId) {
        return animationCache.computeIfAbsent(animationId, this::parseAnimation);
    }

    public BlockyAnimationDefinition getAnimation(Identifier animationId) {
        return animationCache.get(animationId);
    }

    public void clearAnimation(Identifier animationId) {
        animationCache.remove(animationId);
    }

    private BlockyAnimationDefinition parseAnimation(Identifier id) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        Optional<Resource> resource = manager.getResource(id);

        if (resource.isEmpty()) {
            HytaleModelLoader.LOGGER.warn("Animation resource not found: {}", id);
            return null;
        }

        try (BlockyTokenizer tokenizer = new BlockyTokenizer(resource.get().open())) {
            return BlockyAnimParser.parse(tokenizer.getRoot());
        } catch (Exception e) {
            HytaleModelLoader.LOGGER.error("Failed to parse animation '{}': {}", id, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        animationCache.clear();
    }
}
