package com.litehed.hytalemodels.blockymodel;

import com.google.common.collect.Maps;
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

public class BlockyModelLoader implements UnbakedModelLoader<BlockyModel>, ResourceManagerReloadListener {

    public static final BlockyModelLoader INSTANCE = new BlockyModelLoader();
    public static final Identifier ID = Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID, "blockymodel_loader");
    private final Map<BlockyModelGeometry.Settings, BlockyModelGeometry> geometryCache = Maps.newConcurrentMap();

    // Important for cleaning up during resource reloads
    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        geometryCache.clear();
    }

    /**
     * Reads a BlockyModel file and returns a BlockyModel instance
     *
     * @param jsonObject                 the JsonObject representing the BlockyModel
     * @param jsonDeserializationContext the context for deserializing JSON
     * @return a BlockyModel instance
     * @throws JsonParseException if the model is malformed or missing required fields
     */
    @Override
    public BlockyModel read(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        HytaleModelLoader.LOGGER.debug("[BlockyModelLoader] Reading model from file: {}", jsonObject);

        if (!jsonObject.has("model")) {
            throw new JsonParseException("BlockyModel Loader requires a 'model' key that points to a valid BlockyModel file.");
        }

        String modelLocation = jsonObject.get("model").getAsString();
        StandardModelParameters parameters = StandardModelParameters.parse(jsonObject, jsonDeserializationContext);

        var geometry = loadGeometry(new BlockyModelGeometry.Settings(Identifier.parse(modelLocation)));
        return new BlockyModel(parameters, geometry);
    }

    /**
     * Loads and parses a BlockyModel file from the given location
     *
     * @param settings the settings containing the model location
     * @return the parsed BlockyModelGeometry
     */
    public BlockyModelGeometry loadGeometry(BlockyModelGeometry.Settings settings) {
        return geometryCache.computeIfAbsent(settings, (data) -> {
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            Resource resource = manager.getResource(settings.modelLocation()).orElseThrow();
            try (BlockyTokenizer tokenizer = new BlockyTokenizer(resource.open())) {
                return BlockyModelGeometry.parse(tokenizer, data);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Could not find BlockyModel file", e);
            } catch (Exception e) {
                throw new RuntimeException("Could not read BlockyModel file", e);
            }
        });
    }
}
