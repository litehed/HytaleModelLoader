package com.litehed.hytalemodels.blockymodel;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.litehed.hytalemodels.HytaleModelLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockyModelLoader implements UnbakedModelLoader<BlockyModel>, ResourceManagerReloadListener {

    public static final BlockyModelLoader INSTANCE = new BlockyModelLoader();
    public static final Identifier ID = Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID, "blockymodel_loader");

    private final Map<BlockyModelGeometry.Settings, BlockyModelGeometry> geometryCache = new ConcurrentHashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        geometryCache.clear();
    }

    @Override
    public BlockyModel read(JsonObject jsonObject, JsonDeserializationContext context) throws JsonParseException {
        if (!jsonObject.has("model")) {
            throw new JsonParseException("BlockyModel Loader requires a 'model' key that points to a valid BlockyModel file.");
        }

        String modelLocation = jsonObject.get("model").getAsString();
        StandardModelParameters parameters = StandardModelParameters.parse(jsonObject, context);

        BlockyModelGeometry geometry = loadGeometry(new BlockyModelGeometry.Settings(Identifier.parse(modelLocation)));
        return new BlockyModel(parameters, geometry);
    }

    /**
     * Loads a model file. Results are cached until the next resource reload
     *
     * @param settings the settings holding the model location
     * @return the parsed geometry
     */
    public BlockyModelGeometry loadGeometry(BlockyModelGeometry.Settings settings) {
        return geometryCache.computeIfAbsent(settings, this::readGeometry);
    }

    private BlockyModelGeometry readGeometry(BlockyModelGeometry.Settings settings) {
        Identifier location = settings.modelLocation();
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(location)
                    .orElseThrow(() -> new FileNotFoundException("File not found"));
            try (BlockyTokenizer tokenizer = new BlockyTokenizer(resource.open())) {
                return BlockyModelGeometry.parse(tokenizer);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not read BlockyModel " + location + ": " + e.getMessage(), e);
        }
    }
}