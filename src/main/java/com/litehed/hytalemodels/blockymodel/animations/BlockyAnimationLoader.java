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

public class BlockyAnimationLoader implements ResourceManagerReloadListener {

    public static final BlockyAnimationLoader INSTANCE = new BlockyAnimationLoader();

    private final Map<Identifier, BlockyAnimationDefinition> animationCache = Maps.newConcurrentMap();

    public BlockyAnimationDefinition getAnimation(Identifier animationId) {
        return animationCache.get(animationId);
    }

    public BlockyAnimationDefinition loadAnimation(Identifier animationId) {
        return animationCache.computeIfAbsent(animationId, (id) -> {
            try {
                ResourceManager manager = Minecraft.getInstance().getResourceManager();
                Resource resource = manager.getResource(id).orElse(null);

                if (resource == null) {
                    HytaleModelLoader.LOGGER.warn("Could not find animation file: {}", id);
                    return null;
                }

                try (BlockyTokenizer tokenizer = new BlockyTokenizer(resource.open())) {
                    return BlockyAnimParser.parse(tokenizer.getRoot());
                }
            } catch (Exception e) {
                HytaleModelLoader.LOGGER.error("Failed to load animation: {}", animationId, e);
                return null;
            }
        });
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        animationCache.clear();
    }

    public void clearAnimation(Identifier animationId) {
        animationCache.remove(animationId);
    }
}
