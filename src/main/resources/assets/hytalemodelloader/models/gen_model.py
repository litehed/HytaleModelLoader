import os
import json

def ensure_folder(path):
    if not os.path.exists(path):
        os.makedirs(path)

def write_json(path, data):
    with open(path, "w") as f:
        json.dump(data, f, indent=2)

def generate_from_blockymodel(filepath, model_type):
    filename = os.path.basename(filepath)
    name = filename.replace(".blockymodel", "")

    # Folder structure
    block_folder = "./block"
    item_folder = "./item"
    items_folder = "../items"
    blockstates_folder = "../blockstates"

    ensure_folder(block_folder)
    ensure_folder(item_folder)
    ensure_folder(items_folder)

    if model_type == "block":
        ensure_folder(blockstates_folder)

    # BLOCK MODEL
    if model_type == "block":
        block_json = {
            "loader": "hytalemodelloader:blockymodel_loader",
            "model": f"hytalemodelloader:models/{name}.blockymodel",
            "render_type": "minecraft:cutout",
            "textures": {
                "texture": f"hytalemodelloader:block/{name}",
                "particle": f"hytalemodelloader:block/{name}"
            }
        }
        write_json(f"{block_folder}/{name}.json", block_json)

        # Item JSON (block item)
        item_json = {
            "parent": f"hytalemodelloader:block/{name}"
        }
        write_json(f"{item_folder}/{name}.json", item_json)

        # Items folder JSON
        items_json = {
            "model": {
                "type": "minecraft:model",
                "model": f"hytalemodelloader:item/{name}"
            }
        }
        write_json(f"{items_folder}/{name}.json", items_json)

        # Blockstates JSON
        blockstates_json = {
            "variants": {
                "facing=north": {"model": f"hytalemodelloader:block/{name}", "y": 180},
                "facing=east":  {"model": f"hytalemodelloader:block/{name}", "y": 270},
                "facing=south": {"model": f"hytalemodelloader:block/{name}"},
                "facing=west":  {"model": f"hytalemodelloader:block/{name}", "y": 90}
            }
        }
        write_json(f"{blockstates_folder}/{name}.json", blockstates_json)

        print(f"Block model generated for {name}")

    # ITEM MODEL
    elif model_type == "item":
        item_json = {
            "parent": "neoforge:item/default",
            "loader": "hytalemodelloader:blockymodel_loader",
            "model": f"hytalemodelloader:models/{name}.blockymodel",
            "textures": {
                "texture": f"hytalemodelloader:item/{name}_texture",
                "particle": f"hytalemodelloader:item/{name}_texture"
            }
        }
        write_json(f"{item_folder}/{name}.json", item_json)

        items_json = {
            "model": {
                "type": "minecraft:model",
                "model": f"hytalemodelloader:item/{name}"
            }
        }
        write_json(f"{items_folder}/{name}.json", items_json)

        print(f"Item model generated for {name}")

    else:
        print("Invalid type. Must be 'block' or 'item'.")


if __name__ == "__main__":
    filepath = input("Enter the .blockymodel file path: ").strip()
    model_type = input("Is this a block or item? ").strip().lower()

    generate_from_blockymodel(filepath, model_type)