#!/usr/bin/env python3
"""Stage the pinned 59-image legacy set for Android preview builds only.

The 142-image release is packaged separately via package_content_v2.py.
Never download PWA code or replace the repository's existing launcher icon.
"""
from __future__ import annotations

import pathlib
import urllib.request

OWNER = "miu-piu"
REPO = "kids-games"
COMMIT = "176e1393e171510849e8cb7d11b22c022e8dd473"
BASE_RAW = f"https://raw.githubusercontent.com/{OWNER}/{REPO}/{COMMIT}/animals-click"
ROOT = pathlib.Path(__file__).resolve().parents[1]
APP_IMAGES = ROOT / "app/src/main/assets/images"
APP_ICON = ROOT / "app/src/main/res/drawable-nodpi/zveryata_icon.png"

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

def download(url: str, destination: pathlib.Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "zveryata-preview-sync"})
    with urllib.request.urlopen(request, timeout=60) as response:
        destination.write_bytes(response.read())


def main() -> None:
    if not APP_ICON.is_file():
        raise FileNotFoundError("The approved launcher icon must be present in the repository")
    APP_IMAGES.mkdir(parents=True, exist_ok=True)

    for existing in APP_IMAGES.glob("*.webp"):
        existing.unlink()

    for filename in CANONICAL_IMAGES:
        download(f"{BASE_RAW}/assets/images/{filename}", APP_IMAGES / filename)

    actual = sorted(p.name for p in APP_IMAGES.glob("*.webp"))
    if actual != sorted(CANONICAL_IMAGES):
        raise RuntimeError("Preview assets must match the pinned 59-image list")

    print("Staged exactly 59 pinned legacy photographs for temporary offline Android preview.")


if __name__ == "__main__":
    main()
