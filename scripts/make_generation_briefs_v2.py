#!/usr/bin/env python3
"""Build independent minimal prompts for the 141 animal images.

The catalog supplies animal identity and life stage, not scene directions.
The image generator chooses a natural pose, background and appearance within
the common photographic requirements.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "catalog/catalog-v2.json"

PHOTO_STANDARD = (
    "Create one standalone photorealistic vertical 9:16 photograph. "
    "One recognizable animal, entirely visible with correct natural anatomy "
    "and lifelike proportions. Use a natural species-appropriate setting, "
    "a calm uncluttered background, soft daylight and a clear silhouette. "
    "The animal is the obvious main subject of a single continuous photograph."
)


def briefs() -> list[dict]:
    cards = json.loads(CATALOG.read_text(encoding="utf-8"))["cards"]
    if len(cards) != 142 or len({card["id"] for card in cards}) != 142:
        raise ValueError("Catalog must contain 142 unique cards")
    output = []
    for card in cards:
        if card["id"] == "ari":
            output.append({
                "id": card["id"], "label": card["label"], "asset": card["asset"],
                "generation": "PRESERVE_REAL_PHOTO", "review": "PENDING",
            })
            continue
        prompt = (
            f"{PHOTO_STANDARD} "
            f"Subject: {card['reference']}. "
            f"Life stage: {card['stage']}."
        )
        output.append({
            "id": card["id"], "label": card["label"], "asset": card["asset"],
            "reference": card["reference"], "stage": card["stage"],
            "section": card["section"], "generation": "GENERATE_SEPARATELY",
            "prompt": prompt, "review": "PENDING",
        })
    return output


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="catalog/generation-briefs-v2.json")
    args = parser.parse_args()
    result = {"schema_version": "2.0", "count": 142, "cards": briefs()}
    path = ROOT / args.output
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Prepared {sum(c['generation'] == 'GENERATE_SEPARATELY' for c in result['cards'])} image prompts.")


if __name__ == "__main__":
    main()
