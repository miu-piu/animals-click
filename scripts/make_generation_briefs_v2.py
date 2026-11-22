#!/usr/bin/env python3
"""Create one independent, positive-only generation brief per catalog entry.

This prepares text, not images. An image tool receives just the prompt for
the selected animal, never the complete catalog or batch.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "catalog/catalog-v2.json"

POSE = {
    "cat": "Adult grey-brown tabby cat with white chest, full tail, standing on four paws.",
    "kitten": "Orange-and-white kitten aged two to three months, clearly juvenile proportions, full tail.",
    "dog": "Adult Irish Red Setter with a long chestnut silky coat, feathered ears and tail, elegant standing pose.",
    "puppy": "Three-month-old Irish Red Setter puppy, chestnut fur, youthful proportions, all four paws and full tail.",
    "rabbit": "Adult brown-and-white domestic rabbit with full ears, four paws and small tail.",
    "hamster": "Adult golden Syrian hamster with compact body, rounded ears, paws and clear whiskers.",
    "guinea_pig": "Adult tricolor domestic guinea pig with rounded body and four short visible paws.",
    "pet_rat": "Adult grey-and-white fancy rat with four visible paws and entire long thin tail.",
    "budgerigar": "Small adult green-and-yellow budgerigar perched with feet, wings and full long tail visible.",
    "goldfish": "Adult orange goldfish swimming calmly, complete natural fins and tail.",
    "tortoise": "Adult land tortoise at a three-quarter angle, domed shell, head and all four legs visible.",
    "cow": "Adult black-and-white Holstein dairy cow, clear udder and full legs.",
    "bull": "Strong adult brown domestic bull, characteristic male proportions and full legs.",
    "calf": "Young black-and-white Holstein calf with juvenile proportions and all four slender legs.",
    "horse": "Adult horse standing in natural three-quarter side view, full legs and hooves.",
    "foal": "Young foal standing with all four long slender legs and true juvenile proportions.",
    "squirrel": "Bushy tail arched visibly away from the body on a natural branch.",
    "deer": "Adult stag standing on four long straight legs, all antlers and hooves fully inside the frame.",
    "fawn": "Young deer standing on four clearly separated legs with its head and back visible.",
    "moose": "Adult moose standing in a natural side view with full legs, muzzle and antlers.",
    "amur_tiger": "Adult Amur tiger with a strong frame, thick fur, authentic black stripes, complete tail and paws.",
    "amur_leopard": "Adult Far Eastern leopard with pale thick fur and authentic leopard rosettes with light centers, full tail and paws.",
    "snow_leopard": "Adult snow leopard with smoky silver-grey spotted fur and an exceptionally long heavy tail.",
    "giraffe": "Tall adult giraffe with true long-necked proportions, four complete legs and visible hooves.",
    "peacock": "Adult male peacock displaying one complete open fan of feathers, all fan edges and feet visible.",
    "ray": "Adult stingray swimming in a frontal and gently side-on three-quarter perspective, three-dimensional body, two broad pectoral fins, full tail, accurate dorsal eyes and spiracles.",
    "octopus": "True octopus with eight naturally articulated arms and the entire mantle visible.",
    "jellyfish": "Translucent jellyfish with one softly lit bell and complete flowing tentacles.",
    "snake": "Whole grass snake extending in a loose natural S-curve across smooth ground.",
    "chameleon": "Natural chameleon in clear side profile on a simple branch, grasping feet and curled tail.",
    "ostrich": "Adult ostrich with entire long neck, full legs and visible feet.",
    "flamingo": "Adult flamingo with full curved neck and both long legs visible.",
    "frog": "Adult green frog with long hind legs and webbed toes beside freshwater.",
    "starfish": "Natural five-armed sea star seen from above, each arm entirely visible.",
    "butterfly": "Real swallowtail butterfly with two complete open wings and delicate antennae.",
    "dragonfly": "Dragonfly with exactly four transparent wings and long full abdomen on a natural perch.",
    "ladybird": "Adult seven-spotted ladybird in a clear dorsal view with realistic wing cases and legs.",
}

HABITATS = {
    "Домашние питомцы": "A quiet real domestic setting with softly blurred furniture shapes and simple flooring or perch.",
    "Ферма и деревня": "A peaceful real farm setting with an open smooth pasture and gentle distant farm shapes.",
    "Лес и поля": "A natural woodland clearing with smooth earthy ground and soft distant tree tones.",
    "Дальний Восток и горы": "Authentic temperate forest or mountain habitat with low-detail soil or smooth natural stone.",
    "Африка и саванна": "Open African savanna in gentle daylight with simple short grass.",
    "Австралия": "Authentic Australian open woodland or water habitat with soft natural tones.",
    "Америка": "Authentic Americas habitat with a natural, low-detail ground and calm distant foliage.",
    "Арктика и север": "Authentic Arctic snow or tundra with clear tonal separation from the animal.",
    "Пустыня": "Warm muted desert sand and natural rock formations with broad quiet shapes.",
    "Реки, озёра и болота": "Natural freshwater scene with muted green-blue water and gentle background detail.",
    "Море и океан": "Natural ocean habitat with calm blue water and softly graded underwater light.",
    "Птицы": "Authentic natural perching or ground habitat, with a simple branch or perch.",
    "Насекомые и другие мелкие животные": "Natural realistic macro habitat with one broad leaf, branch or plain rock.",
    "Пресмыкающиеся": "Natural quiet sand, stone or branch setting with clear silhouette.",
}

DEFAULT_POSE = "An anatomically correct calm natural three-quarter or profile view, with all defining body parts visible."
FINISH = (
    "Composition: one animal, the entire recognizable silhouette within a single uninterrupted "
    "vertical frame, a softly blurred but believable environment and enough room around all "
    "limbs, fins, horns, wings and tails. Soft authentic daylight, sharp natural photographic "
    "textures and colors. An easy-to-recognize subject for a young child."
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
                "generation": "PRESERVE_REAL_PHOTO",
                "instructions": "Use the existing original for this catalog card.",
                "review": "PENDING",
            })
            continue
        habitat = HABITATS.get(card["section"])
        if habitat is None:
            raise ValueError(f"Missing habitat description: {card['section']}")
        pose = POSE.get(card["id"], DEFAULT_POSE)
        prompt = (
            "Create one standalone, photorealistic vertical 9:16 photograph. "
            f"Subject: one {card['reference']} ({card['stage']}). "
            f"COMPOSITION AND POSE: {pose} "
            f"Location: {card['scene']}. "
            f"Environment: {habitat} "
            f"{FINISH}"
        )
        output.append({
            "id": card["id"], "label": card["label"], "asset": card["asset"],
            "reference": card["reference"], "stage": card["stage"],
            "scene": card["scene"], "section": card["section"],
            "generation": "GENERATE_SEPARATELY", "prompt": prompt,
            "review": "PENDING",
        })
    return output


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="catalog/generation-briefs-v2.json")
    args = parser.parse_args()
    result = {"schema_version": "1.0", "count": 142, "cards": briefs()}
    path = ROOT / args.output
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n",
                    encoding="utf-8")
    print(f"Prepared {len(result['cards'])} catalog briefs with "
          f"{sum(c['generation'] == 'GENERATE_SEPARATELY' for c in result['cards'])} individual prompts.")


if __name__ == "__main__":
    main()
