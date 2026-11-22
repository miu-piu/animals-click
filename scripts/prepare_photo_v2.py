#!/usr/bin/env python3
"""Convert ONE generated 9:16 source into an unapproved photo candidate.

No generative enhancement, automatic cropping, or silent visual acceptance.
Source images are copied byte-for-byte into assets-v2/originals/.
Converted WebP is staged in assets-v2/candidates/, never into approved/.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import shutil

from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "catalog/catalog-v2.json"
DEST_SIZE = (1440, 2560)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def prepare_photo(
    card_id: str, input_file: Path, destination: Path = ROOT / "assets-v2",
    quality: int = 90, catalog_path: Path = CATALOG
) -> dict:
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))["cards"]
    entries = [row for row in catalog if row["id"] == card_id]
    if len(entries) != 1:
        raise ValueError(f"No unique catalog card with id {card_id!r}")
    card = entries[0]
    if card_id == "ari":
        raise ValueError("Ari must be preserved from a real original, never generated")
    if quality < 70 or quality > 95:
        raise ValueError("WebP quality must be between 70 and 95")
    if not input_file.is_file() or input_file.suffix.lower() not in (".png", ".jpg", ".jpeg", ".webp"):
        raise ValueError("Input must be an existing PNG, JPEG or WebP photograph")

    original_name = card_id + input_file.suffix.lower()
    original_path = destination / "originals" / original_name
    candidate_path = destination / "candidates" / card["asset"]
    metadata_path = destination / "candidates" / "metadata" / (card_id + ".json")

    with Image.open(input_file) as loaded:
        loaded.load()
        oriented = ImageOps.exif_transpose(loaded)
        width, height = oriented.size
        if width <= 0 or height <= 0:
            raise ValueError("Invalid source dimensions")
        ratio = width / height
        expected = DEST_SIZE[0] / DEST_SIZE[1]
        if abs(ratio - expected) / expected > 0.025:
            raise ValueError("Source must be close to 9:16; do not crop the animal to make it fit")
        if "A" in oriented.getbands() and oriented.getchannel("A").getextrema()[0] < 255:
            raise ValueError("Transparent image is not a suitable edge-to-edge photograph")
        converted = oriented.convert("RGB").resize(DEST_SIZE, Image.Resampling.LANCZOS)

    original_path.parent.mkdir(parents=True, exist_ok=True)
    candidate_path.parent.mkdir(parents=True, exist_ok=True)
    metadata_path.parent.mkdir(parents=True, exist_ok=True)

    source_hash = sha256(input_file)
    if original_path.exists() and sha256(original_path) != source_hash:
        raise FileExistsError(f"Refusing to replace a preserved original: {original_path}")
    if candidate_path.exists():
        raise FileExistsError(f"An image candidate already exists: {candidate_path}")
    shutil.copy2(input_file, original_path)
    converted.save(candidate_path, "WEBP", quality=quality, method=6)
    with Image.open(candidate_path) as check:
        check.load()
        if check.format != "WEBP" or check.size != DEST_SIZE:
            raise ValueError("Post-conversion image validation failed")
    if candidate_path.stat().st_size > 2_500_000:
        candidate_path.unlink(missing_ok=True)
        raise ValueError("Resulting WebP exceeds the 2.5 MB release per-image limit")

    record = {
        "id": card_id,
        "label": card["label"],
        "asset": card["asset"],
        "original": original_name,
        "original_sha256": source_hash,
        "sha256": sha256(candidate_path),
        "source_dimensions": {"width": width, "height": height},
        "output_dimensions": {"width": 1440, "height": 2560},
        "upscaled": width < 1440 or height < 2560,
        "generation": "NEW_IMAGE",
        "status": "pending_visual_review",
        "review_notes": "",
    }
    metadata_path.write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n",
                             encoding="utf-8")
    return record


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--id", required=True, help="Exact card id from catalog/catalog-v2.json")
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--quality", type=int, default=90)
    parser.add_argument("--destination", type=Path, default=ROOT / "assets-v2")
    args = parser.parse_args()
    record = prepare_photo(args.id, args.input, args.destination, args.quality)
    print(json.dumps(record, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
