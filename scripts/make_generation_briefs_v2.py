#!/usr/bin/env python3
"""Create an individually grounded scene brief for each approved catalog card.

No image generation is performed here. Every animal gets a species/age/scene
description and habitat-sensitive pose rules. A separate human visual review
is required before artwork can be marked approved.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "catalog/catalog-v2.json"

CAMERA = {
    "cat": "One adult grey-brown tabby domestic cat with white chest, calmly standing on all four paws on pale wood in a quiet home, entire tail visible. Do not depict a kitten.",
    "kitten": "One orange-and-white domestic kitten around two to three months old with credible juvenile body and paws, standing fully visible on a plain light rug in a calm home.",
    "dog": "One ADULT Irish RED SETTER dog with distinctive long silky chestnut coat, feathered ears and tail, standing fully visible on flat lawn; no other breed.",
    "puppy": "One Irish RED SETTER PUPPY around three months old, red-chestnut young proportions, all paws and full tail visible on quiet flat garden lawn.",
    "rabbit": "One adult domestic brown-and-white rabbit with both ears, paws and small tail visible on simple home enclosure flooring, no front bars.",
    "hamster": "One adult golden Syrian hamster, realistic compact body, feet, ears and whiskers on simple clean enclosure surface, no toys.",
    "guinea_pig": "One adult tricolor domestic guinea pig, natural rounded proportions and short visible legs on clean simple enclosure floor, no front bars.",
    "pet_rat": "One adult grey-and-white domestic fancy rat with full thin long tail and all four pink feet visible on plain clean indoor shelf.",
    "cow": "One adult black-and-white Holstein dairy cow, full legs and udder clearly visible on flat farm pasture.",
    "bull": "One adult powerful brown domestic bull with clear male body proportions and no udder, peaceful stance on flat pasture.",
    "calf": "One young black-and-white Holstein calf, clearly juvenile proportions, all four slender legs visible on smooth pasture.",
    "squirrel": "Entire tail arched visibly away from torso, paws on a simple branch; three-quarter lateral view.",
    "deer": "Adult stag (DEER, not leopard or any feline), standing on FOUR visible, naturally straight legs, antlers entirely inside the frame. Hooves rest on a simple smooth woodland clearing with no detailed leaf litter, gravel, stones or visible moss texture.",
    "fawn": "Young deer STANDING with all FOUR legs extended and separated, not lying or tucked.",
    "moose": "Adult moose standing in clear profile, entire high legs, muzzle and antlers distinct.",
    "horse": "Full adult horse in calm side three-quarter standing pose, long legs and hooves visible.",
    "foal": "Young horse standing on all four long legs, realistic foal body proportions.",
    "amur_tiger": "ADULT Amur/Siberian tiger, heavy winter coat, natural black stripes, complete tail and all paws. Temperate Russian Far Eastern mixed forest, NOT tropical jungle.",
    "amur_leopard": "ADULT Far Eastern/Amur leopard, pale thick coat and LEOPARD rosettes (not jaguar center-spotted rosettes). Far Eastern temperate forest expressed through soft out-of-focus broad woodland tones, not a studio backdrop. Complete long tail and all paws. Stand on quiet smooth soil or a nearly textureless natural rock: no busy moss, layered leaves, pebbles or twigs.",
    "snow_leopard": "ADULT snow leopard/irbis with smoky-grey thick spotted fur and an exceptionally long heavy tail, standing on four paws against simple high mountain rock.",
    "peacock": "Adult male displaying one COMPLETE open tail-fan, every edge and feather tip inside the frame, feet visible; camera pulled back as needed.",
    "ray": "ONE adult stingray swimming at a believable frontal and slightly side-on three-quarter perspective, NEVER directly from overhead. Show three-dimensional body, both wing-like pectoral fins, full long tail and anatomically accurate dorsal eyes/spiracles versus ventral mouth/gill slits. No human-like smile. Calm blue-grey or turquoise water, smooth quiet seafloor, no glare, caustic light bands or patterned reflections.",
    "octopus": "Exactly eight anatomically plausible arms separately readable across quiet seafloor, complete mantle.",
    "jellyfish": "Natural translucent bell and every tentacle distinctly visible against uncluttered blue water, no human-like eyes.",
    "snake": "Entire real grass snake in one gently extended S-curve across pale smooth sand or rock, never tightly coiled.",
    "chameleon": "Complete side profile on a single clean branch, visible grasping feet and naturally curled tail, non-camouflage background.",
    "budgerigar": "A real adult budgerigar perched naturally inside a spacious domestic aviary, no visible front cage bars or glass, whole tail and feet.",
    "goldfish": "Single adult goldfish with natural fin anatomy, complete fins and tail, clean aquarium water and no front glass reflections.",
    "giraffe": "Entire adult giraffe including all four exceptionally long legs, hooves, head and neck; true body ratios.",
    "ostrich": "Adult ostrich with whole long neck and legs, both feet fully visible; clean savanna ground.",
    "flamingo": "Adult flamingo with entire neck and both long legs visible; no artificial water reflections hiding legs.",
    "frog": "Adult green frog with clear hind legs and webbed feet on simple stone at a greenish freshwater pond.",
    "starfish": "Natural five-armed sea star from overhead, each arm complete on uniform sandy shallow-sea floor.",
    "butterfly": "Real swallowtail butterfly photographed from above, both open wings and antennae fully inside the frame.",
    "dragonfly": "True dragonfly with exactly four slender wings on simple branch, side/dorsal view, unbroken tail.",
    "ladybird": "Adult seven-spotted ladybird on broad smooth green leaf, dorsal view, true wing cases and legs.",
}

GROUP_RULES = {
    "Домашние питомцы": "Home or pet-habitat context; no opaque foreground cage bars or glass. Show all anatomical landmarks against quiet surroundings.",
    "Ферма и деревня": "Familiar quiet farm setting; body at a natural standing side or three-quarter angle; all feet and tail visible.",
    "Лес и поля": "Quiet woodland habitat in true daylight, soft distant tree-color masses. Under the animal use smooth uncomplicated forest ground, no sharp leaf litter, twigs, speckled moss, pebbles or textured stones; no foliage covering paws or tail.",
    "Дальний Восток и горы": "Grounded geographic habitat shown through naturally soft distant forest or mountain color masses. The standing surface is nearly textureless natural soil or smooth rock; avoid patterned moss, small stones, branches and distracting detail. Accurate species-specific fur and complete silhouette.",
    "Африка и саванна": "Authentic open African grassland, safe peaceful pose, no grass concealing legs or hooves.",
    "Австралия": "Authentic Australian open woodland or freshwater habitat, species-specific posture and full body.",
    "Америка": "Authentic South American/natural habitat; full species-specific body and tail.",
    "Арктика и север": "Natural Arctic tundra, snow or sea ice; animal separated tonally from pale background.",
    "Пустыня": "Muted smooth sand or rock substrate to separate complicated form and limbs.",
    "Реки, озёра и болота": "Freshwater green-grey or earthy palette; body contour visible through water or against simple shore.",
    "Море и океан": "Sea blue/turquoise or deep ocean blue appropriate to depth, with gentle low-contrast illumination. No high-frequency wave caustics, white sparkles, sun streaks, rippled-floor reflections or glare on the subject; water and seabed form calm simple masses and fins/tails are clear.",
    "Птицы": "Whole bird, beak, feet, wings and tail distinguishable; one simple natural perch or ground.",
    "Насекомые и другие мелкие животные": "Photorealistic macro on smooth leaf, rock, branch or soil; no bright flowers or distracting detailed textures, correct legs and antennae.",
    "Пресмыкающиеся": "True reptile anatomy shown on quiet simple natural rock, sand or branch; no confusing camouflage.",
}

COMMON = (
    "One separately generated vertical portrait photograph, approximate aspect ratio 9:16. "
    "Exactly ONE lifelike animal, entire body including limbs, tail, ears and distinctive parts "
    "inside the image with generous natural environment margin. Adult form by default; if explicitly "
    "a baby animal, use species-correct juvenile proportions. "
    "Premium realistic wildlife/pet photography, natural light, correct anatomy, "
    "calm expression and accurate pigmentation. The animal must be the unmistakable "
    "visual focal point, sharp and high-contrast against a believable but deliberately "
    "quiet habitat: one or two large soft background color masses, no small competing details. "
    "The surface directly beneath paws, hooves or fins must be natural, visually simple, "
    "smooth and low-contrast, not a decorated pedestal. In water, no caustic light ribbons "
    "or bright specular highlights on sand or animal. Keep real habitat context, not a "
    "uniform studio backdrop. Do not add the black caption header; "
    "the Android app places its own text outside the image. "
    "No people, extra animals, toy look, illustration, CGI, Disney/anime, unnatural baby eyes, "
    "human smiles, wrong limbs or fingers, clipped tails or paws, injury, aggression, logos, "
    "letters, borders, unnatural HDR, decorative sparkles or colored studio background."
)


def briefs() -> list[dict]:
    data = json.loads(CATALOG.read_text(encoding="utf-8"))
    cards = data["cards"]
    if len(cards) != 142 or len({c["id"] for c in cards}) != 142:
        raise ValueError("Expected 142 unique cards")
    output = []
    for card in cards:
        if card["id"] == "ari":
            output.append({
                "id": "ari", "label": "Ари", "asset": card["asset"],
                "generation": "PRESERVE_REAL_PHOTO",
                "instructions": "Never generate, replace or stylize Ari; optimize the original photo only.",
                "review": "PENDING",
            })
            continue
        specific = CAMERA.get(card["id"],
            "Use the most recognizable natural full-body angle for this exact animal; "
            "keep species-defining features, legs, tail and characteristic appendages visible.")
        group = GROUP_RULES.get(card["section"])
        if group is None:
            raise ValueError(f"Missing environment rules: {card['section']}")
        prompt = (
            f"SUBJECT: {card['reference']}. Russian screen label (not embedded in pixels): "
            f"{card['label']}. Age: {card['stage']}. LOCATION AND SCENE: {card['scene']}. "
            f"COMPOSITION AND POSE: {specific} ENVIRONMENT: {group} {COMMON}"
        )
        output.append({
            "id": card["id"], "label": card["label"], "asset": card["asset"],
            "reference": card["reference"], "stage": card["stage"],
            "scene": card["scene"], "section": card["section"],
            "generation": "GENERATE_SEPARATELY",
            "prompt": prompt,
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
    print(f"Prepared {len(result['cards'])} image-specific briefs "
          f"({sum(c['generation']=='GENERATE_SEPARATELY' for c in result['cards'])} to generate).")


if __name__ == "__main__":
    main()
