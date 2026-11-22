#!/usr/bin/env python3
"""Prepare one generated image for review without altering the meaning of its pixels.

Preserve all original revisions. Conversion is technical only (orientation, resize,
RGB colour mode, WebP encoding): no generative enlargement or subject cropping.
Release approval is a separate manual step.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil

from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "catalog/catalog-v2.json"
DEST_SIZE = (1440, 2560)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def prepare_photo(
    card_id: str,
    input_file: Path,
    destination: Path = ROOT / "assets-v2",
    quality: int = 90,
    catalog_path: Path = CATALOG,
    replace_candidate: bool = False,
) -> dict:
    cards = json.loads(catalog_path.read_text(encoding="utf-8"))["cards"]
    items = [item for item in cards if item["id"] == card_id]
    if len(items) != 1:
        raise ValueError(f"Unknown/nonunique catalog id {card_id!r}")
    card = items[0]
    if card_id == "ari":
        raise ValueError("Never generate or replace Ari from a synthetic photograph")
    if not (70 <= quality <= 95):
        raise ValueError("WebP quality must be between 70 and 95")
    if not input_file.is_file() or input_file.suffix.lower() not in (".png", ".jpg", ".jpeg", ".webp"):
        raise ValueError("Expected an existing photographic PNG, JPEG or WebP original")

    source_hash = sha256(input_file)
    original_name = f"{card_id}-{source_hash[:12]}{input_file.suffix.lower()}"
    original_path = destination / "originals" / original_name
    candidate_path = destination / "candidates" / card["asset"]
    metadata_path = destination / "candidates" / "metadata" / (card_id + ".json")
    history = destination / "candidates" / "history"
    if candidate_path.is_file() and not replace_candidate:
        raise FileExistsError("Existing candidate must not be overwritten without --replace-candidate")

    with Image.open(input_file) as source:
        source.load()
        oriented = ImageOps.exif_transpose(source)
        width, height = oriented.size
        if width < 1 or height < 1:
            raise ValueError("Invalid source dimensions")
        expected = DEST_SIZE[0] / DEST_SIZE[1]
        if abs(width / height - expected) / expected > 0.025:
            raise ValueError("Source is not close enough to 9:16; do not crop an animal to make it fit")
        if "A" in oriented.getbands() and oriented.getchannel("A").getextrema()[0] < 255:
            raise ValueError("Transparent pixels are forbidden in edge-to-edge photographs")
        converted = oriented.convert("RGB").resize(DEST_SIZE, Image.Resampling.LANCZOS)

    original_path.parent.mkdir(parents=True, exist_ok=True)
    candidate_path.parent.mkdir(parents=True, exist_ok=True)
    metadata_path.parent.mkdir(parents=True, exist_ok=True)
    if original_path.exists():
        if sha256(original_path) != source_hash:
            raise ValueError("Original image hash collision or modified source")
    elif original_path.resolve() != input_file.resolve():
        shutil.copy2(input_file, original_path)

    # Stage to a temporary file. A failed encode/check must never destroy a prior candidate.
    temporary = candidate_path.with_name(f".{card_id}-{source_hash[:12]}.tmp.webp")
    try:
        converted.save(temporary, "WEBP", quality=quality, method=6)
        with Image.open(temporary) as check:
            check.load()
            if check.format != "WEBP" or check.size != DEST_SIZE:
                raise ValueError("WebP round-trip validation failed")
        if temporary.stat().st_size > 2_500_000:
            raise ValueError("WebP exceeds the release size limit of 2.5 MB")
        if candidate_path.is_file():
            history.mkdir(parents=True, exist_ok=True)
            previous_hash = sha256(candidate_path)
            archived = history / f"{card_id}-{previous_hash[:12]}.webp"
            if not archived.exists():
                shutil.copy2(candidate_path, archived)
            if metadata_path.is_file():
                old_meta = json.loads(metadata_path.read_text(encoding="utf-8"))
                previous_meta = history / f"{card_id}-{previous_hash[:12]}.json"
                if not previous_meta.exists():
                    previous_meta.write_text(json.dumps(old_meta, ensure_ascii=False, indent=2) + "\n",
                                             encoding="utf-8")
        os.replace(temporary, candidate_path)
    finally:
        temporary.unlink(missing_ok=True)

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
    parser.add_argument("--id", required=True, help="Card ID from catalog/catalog-v2.json")
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--quality", type=int, default=90)
    parser.add_argument("--destination", type=Path, default=ROOT / "assets-v2")
    parser.add_argument("--replace-candidate", action="store_true")
    args = parser.parse_args()
    record = prepare_photo(
        args.id, args.input, args.destination, args.quality,
        replace_candidate=args.replace_candidate,
    )
    print(json.dumps(record, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
