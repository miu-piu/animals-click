#!/usr/bin/env python3
"""Sync canonical Зверята PWA assets into this Android workspace.

This is a build-time helper only. The final Android app must package assets locally
and must not require network access at runtime.
"""

from __future__ import annotations

import json
import pathlib
import urllib.request

OWNER = "miu-piu"
REPO = "kids-games"
BRANCH = "main"
BASE_RAW = f"https://raw.githubusercontent.com/{OWNER}/{REPO}/{BRANCH}/animals-click"
BASE_API = f"https://api.github.com/repos/{OWNER}/{REPO}/contents/animals-click"
ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "reference" / "assets"
IMAGES = OUT / "images"
ICONS = OUT / "icons"
PWA = ROOT / "reference" / "pwa"

CANONICAL_IMAGES = [
    "01-arich.webp", "02-kitten.webp", "03-irish-setter.webp", "04-rabbit.webp",
    "05-calf.webp", "06-goat.webp", "07-lamb.webp", "08-chick.webp",
    "09-duckling.webp", "10-hedgehog.webp", "11-squirrel.webp", "12-ferret.webp",
    "13-fox.webp", "14-raccoon.webp", "15-fawn.webp", "16-bear.webp",
    "17-lynx.webp", "18-chipmunk.webp", "19-amur-leopard.webp", "20-tiger.webp",
    "21-lion.webp", "22-elephant.webp", "23-giraffe.webp", "24-zebra.webp",
    "25-camel.webp", "26-capybara.webp", "27-alpaca.webp", "28-panda.webp",
    "29-hippo.webp", "30-rhino.webp", "31-tit.webp", "32-owl.webp",
    "33-crow.webp", "34-magpie.webp", "35-pigeon.webp", "36-flamingo.webp",
    "37-hummingbird.webp", "38-cuckoo.webp", "39-peacock.webp", "40-swan.webp",
    "41-whale.webp", "42-shark.webp", "43-penguin.webp", "44-seal.webp",
    "45-dolphin.webp", "46-octopus.webp", "47-seahorse.webp", "48-crab.webp",
    "49-ray.webp", "50-walrus.webp", "51-jellyfish.webp", "52-snake.webp",
    "53-lizard.webp", "54-butterfly.webp", "55-bee.webp", "56-snail.webp",
    "57-lemur.webp", "58-dragonfly.webp", "59-ladybird.webp",
]

ICON_FILES = [
    "zveryata-v3-192.png",
    "zveryata-v3-512.png",
    "zveryata-v3-maskable-512.png",
    "favicon-32.png",
    "favicon-48.png",
    "apple-touch-icon.png",
]

PWA_FILES = [
    "index.html",
    "styles.css",
    "app.js",
    "manifest.webmanifest",
    "service-worker.js",
]


def download(url: str, destination: pathlib.Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "zveryata-android-asset-sync"})
    with urllib.request.urlopen(request, timeout=60) as response:
        destination.write_bytes(response.read())
    print(f"synced {destination.relative_to(ROOT)}")


def main() -> None:
    for name in CANONICAL_IMAGES:
        download(f"{BASE_RAW}/assets/images/{name}", IMAGES / name)

    for name in ICON_FILES:
        download(f"{BASE_RAW}/assets/icons/{name}", ICONS / name)

    for name in PWA_FILES:
        download(f"{BASE_RAW}/{name}", PWA / name)

    manifest = {
        "source_repository": f"https://github.com/{OWNER}/{REPO}",
        "source_path": "animals-click",
        "images": CANONICAL_IMAGES,
        "icons": ICON_FILES,
        "pwa_reference": PWA_FILES,
    }
    (OUT / "sync-manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"\nSynced {len(CANONICAL_IMAGES)} images, {len(ICON_FILES)} icons and {len(PWA_FILES)} PWA reference files.")
    print("These files are for build/reference use. Package required assets inside the Android app for offline runtime.")


if __name__ == "__main__":
    main()
