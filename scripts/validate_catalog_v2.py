#!/usr/bin/env python3
"""Validate production catalog and temporary prototype asset manifest.

This script is independent of the Android build and never downloads images.
"""
from __future__ import annotations

import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def validate_manifest(path: Path, *, expected_count: int | None = None) -> list[dict]:
    document = json.loads(path.read_text(encoding="utf-8"))
    cards = document["cards"]
    if expected_count is not None:
        assert len(cards) == expected_count, f"{path}: {len(cards)} != {expected_count}"
    assert cards, f"{path}: empty catalog"
    for field in ("id", "label", "asset"):
        values = [str(card.get(field, "")).strip() for card in cards]
        assert all(values), f"{path}: missing {field}"
        assert not [value for value, n in Counter(values).items() if n > 1], (
            f"{path}: duplicate {field}"
        )
    for card in cards:
        assert card["asset"].endswith(".webp"), f"Not a WebP: {card['asset']}"
        assert ".." not in card["asset"] and "/" not in card["asset"], (
            f"Unsafe filename: {card['asset']}"
        )
    return cards


def main() -> None:
    approved = json.loads((ROOT / "catalog/catalog-v2.json").read_text(encoding="utf-8"))
    count = approved["catalog_count"]
    assert count == 142, f"Unexpected release catalog count: {count}"
    cards = validate_manifest(ROOT / "catalog/catalog-v2.json", expected_count=count)
    for item in cards:
        for name in ("reference", "scene", "stage", "section"):
            assert isinstance(item.get(name), str) and item[name].strip(), (
                f"Missing {name}: {item['id']}"
            )

    preview = validate_manifest(ROOT / "app/src/main/assets/catalog-preview.json")
    image_dir = ROOT / "app/src/main/assets/images"
    expected = {item["asset"] for item in preview}
    actual = {path.name for path in image_dir.glob("*.webp")}
    assert expected == actual, (
        f"Preview image mismatch: missing={sorted(expected-actual)}, "
        f"unexpected={sorted(actual-expected)}"
    )

    print(f"Validated {len(cards)} approved production entries and "
          f"{len(preview)} temporary preview images.")


if __name__ == "__main__":
    main()
