
Hytale Model Loader
=======

A mod for Minecraft Neoforge 1.21.11 that allows the player to import and use Hytale models in game. It converts Hytale's BlockyModel file format into a renderable version inside of Minecraft. Currently, these can be rendered as item or block models with entity models coming in a future version. This is usable with both resource packs and mods.

### Creating a Model

Models are defined using `.blockymodel` files (custom binary/text format) and referenced in `.json` model files just like standard Minecraft models. To create the model simply download this mod and when creating a model file make sure to add the loader and model location as shown below.

#### Example: `pot.json`

```json
{
  "loader": "hytalemodelloader:blockymodel_loader",
  "model": "hytalemodelloader:models/pot.blockymodel",
  "render_type": "minecraft:cutout",
  "textures": {
    "texture": "hytalemodelloader:block/pot_texture",
    "particle": "hytalemodelloader:block/pot_texture"
  }
}
```

**Key fields:**
- `loader` – References the BlockyModelLoader **This is most important**
- `model` – Path to your `.blockymodel` file (This file is best in the models folder but put it wherever)
- `render_type` – Standard Minecraft render type this needs to be changed depending on model transparency
- `textures` – Texture references used by your model


## TODO

### v1.0.0
- [x] Add model parser `.blockymodel`
- [x] Implement custom item/block loader
- [x] Add block rotation support
- [x] Add double-sided face rendering
- [x] Add UV rotation and mirroring support

### v1.1.0
- [x] Check item and block scaling/translating using model json
- [ ] Make bounding boxes fit models
- [ ] Fix and clean up code

### v1.2.0
- [ ] Add parser for animation support `.blockyanim`
- [ ] Load animations in for blocks and items
- [ ] Create animation system to actually play and time these animations

### v2.0.0
- [ ] Implement entity model loading
- [ ] Create in-game model preview/editing tool
- [ ] Support for custom render layers and transparency blending
- [ ] Clean code and docs for v2 release

### v2.1.0
- [ ] Add animation support for entities


## Contributing

Contributions are welcome! Please feel free to submit issues and prs