#!/usr/bin/env python3
"""Stage Zveryata 2.0 offline content with explicit preview/release modes.

Preview reuses the old approved photographs. Release is fully offline and
requires every one of 142 individually approved images plus locked SHA-256s.
The script must fail closed: incomplete production art is never published.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import re

ROOT = Path(__file__).resolve().parents[1]
APP_ASSETS = ROOT / "app/src/main/assets"
APP_IMAGES = APP_ASSETS / "images"
PREVIEW = APP_ASSETS / "catalog-preview.json"
APP_CATALOG = APP_ASSETS / "catalog.json"
PRODUCTION = ROOT / "catalog/catalog-v2.json"
APP_MANIFEST = APP_ASSETS / "image-hashes.json"
APPROVED_IMAGES = ROOT / "assets-v2/approved/webp"
APPROVED_HASHES = ROOT / "assets-v2/approved/sha256.json"


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        while piece := stream.read(1024 * 1024):
            digest.update(piece)
    return digest.hexdigest()


def validate_cards(cards: list[dict]) -> None:
    if not cards:
        raise ValueError("Empty catalog")
    for key in ("id", "label", "asset"):
        values = [card.get(key) for card in cards]
        if any(not isinstance(v, str) or not v.strip() for v in values):
            raise ValueError(f"Missing/blank {key}")
        if len(set(values)) != len(values):
            raise ValueError(f"Duplicate {key}")
    for card in cards:
        filename = card["asset"]
        if filename != Path(filename).name or not filename.endswith(".webp"):
            raise ValueError(f"Unsafe/non-WebP filename {filename}")


def validate_image(path: Path, strict_dimensions: bool) -> None:
    with path.open("rb") as stream:
        header = stream.read(12)
    if not (header.startswith(b"RIFF") and header[8:12] == b"WEBP"):
        raise ValueError(f"Not a WebP: {path}")
    if strict_dimensions:
        try:
            from PIL import Image
        except ImportError as error:
            raise RuntimeError("Pillow is required for release image validation") from error
        with Image.open(path) as image:
            image.load()
            if image.format != "WEBP" or image.size != (1440, 2560):
                raise ValueError(f"Bad WebP dimensions/format: {path} {image.format} {image.size}")
            if image.mode not in ("RGB", "RGBA"):
                raise ValueError(f"Unsupported colour mode: {path}: {image.mode}")


def validate_approvals(records: dict, expected: set[str]) -> dict[str, str]:
    """A release cannot be made from files with no individual visual review."""
    if set(records) != expected:
        raise ValueError("Approved image record list does not match the 142-card catalog")
    approved_hashes = {}
    for filename, record in records.items():
        if not isinstance(record, dict) or record.get("status") != "approved":
            raise ValueError(f"Image is not individually approved: {filename}")
        for field in ("sha256", "original_sha256"):
            value = record.get(field)
            if not isinstance(value, str) or re.fullmatch("[a-f0-9]{64}", value) is None:
                raise ValueError(f"Invalid {field}: {filename}")
        if not isinstance(record.get("review_notes"), str) or not record["review_notes"].strip():
            raise ValueError(f"Missing visual review notes: {filename}")
        approved_hashes[filename] = record["sha256"]
    return approved_hashes


def package(mode: str) -> None:
    production = load(PRODUCTION)
    full = production["cards"]
    validate_cards(full)
    if len(full) != 142 or production["catalog_count"] != 142:
        raise ValueError("Release catalog must have exactly 142 entries")

    if mode == "preview":
        cards = load(PREVIEW)["cards"]
        source = APP_IMAGES
        hashes = None
    else:
        cards = full
        source = APPROVED_IMAGES
        if not APPROVED_HASHES.is_file():
            raise FileNotFoundError("Missing locked production sha256.json")
        hashes = validate_approvals(
            load(APPROVED_HASHES), {c["asset"] for c in cards}
        )
    validate_cards(cards)

    expected = {c["asset"] for c in cards}
    if not source.is_dir():
        raise FileNotFoundError(f"Missing image folder {source}")
    actual = {p.name for p in source.glob("*.webp")}
    if actual != expected:
        raise ValueError(
            f"{mode}: missing {sorted(expected - actual)}; unexpected {sorted(actual - expected)}"
        )

    calculated = {}
    for card in cards:
        filename = card["asset"]
        path = source / filename
        validate_image(path, strict_dimensions=(mode == "release"))
        calculated[filename] = sha256(path)
        if hashes is not None and hashes[filename] != calculated[filename]:
            raise ValueError(f"SHA-256 mismatch: {filename}")

    if mode == "release":
        APP_IMAGES.mkdir(parents=True, exist_ok=True)
        for existing in APP_IMAGES.glob("*.webp"):
            existing.unlink()
        for name in sorted(expected):
            shutil.copy2(source / name, APP_IMAGES / name)

    # All checks completed successfully before staging the runtime index.
    runtime = {
        "schema_version": "2.0",
        "content_mode": mode,
        "cards": [{"id": c["id"], "label": c["label"], "asset": c["asset"]}
                  for c in cards],
    }
    APP_CATALOG.write_text(json.dumps(runtime, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8")
    APP_MANIFEST.write_text(
        json.dumps({"content_mode": mode, "sha256": calculated},
                   ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Packaged {len(cards)} validated offline cards ({mode})")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", required=True, choices=("preview", "release"))
    args = parser.parse_args()
    package(args.mode)


if __name__ == "__main__":
    main()
