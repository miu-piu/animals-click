# Зверята — Android source specification

This repository is the clean Android workspace for the native version of **«Зверята»**.

## Canonical web source

The existing PWA remains untouched in `miu-piu/kids-games/animals-click/`. The Android app is native Kotlin + Jetpack Compose and does not depend on the web app at runtime.

## NON-NEGOTIABLE: use ONLY the existing approved photos

Use only the canonical animal photographs synced from `miu-piu/kids-games/animals-click/assets/images/` via `python3 scripts/sync_assets.py`. Do not generate, replace, redraw, crop, inpaint, stretch, or remotely load animal images.

## Android identity

- App name: `Зверята`
- Package: `com.miupiu.zveryata`
- Primary orientation: portrait
- Fully offline after install
- No account, analytics, ads, Firebase, network dependency, camera, microphone, location, contacts, notifications, or external links.

## Toddler-first gallery behavior

One animal image fills as much of the experience as possible. There are no visible controls, pagination, toolbar, menu, counters, or buttons.

The child often rests several fingers on the display while tapping or swiping with another finger. Gesture handling therefore tracks each pointer independently.

- tap with any pointer -> next
- swipe / clear movement in any direction -> next
- diagonal movement -> next
- existing resting pointers must not block a new pointer
- small resting-finger jitter -> nothing
- one pointer gesture -> at most one transition until that pointer is lifted
- a different pointer may trigger another transition while previous fingers remain down
- direction never means previous; child interaction always advances
- successful manual interaction restarts autoplay

Autoplay: 30 seconds after the last transition -> next slide, wrapping at the end.

## Image rendering and label

1. Full app canvas is pure black `#000000`.
2. Preserve the entire original photo with FIT/CONTAIN behavior; never crop, stretch, regenerate, extend, or inpaint it.
3. Align the photo to the bottom center so unused vertical space is concentrated above it.
4. No blurred background, frames, gradients, cards, or decorative UI.
5. In the free black space above the photo, center the current animal name horizontally and vertically.
6. Label color: `#F2F2F2`.
7. Typography: system sans-serif, Medium/500, one line, no shadow/outline/animation/letter-spacing decoration.
8. Base phone size: about 28sp; moderately increase for tablet-size widths using Android density-independent sizing, never raw pixel resolution.
9. Common animal names use normal Russian lowercase (`котёнок`, `жираф`, `божья коровка`). Proper names keep correct capitalization: the first slide is `Ари`.

## Android system behavior

Use edge-to-edge immersive fullscreen with normal Android APIs. Remain compatible with Android / Samsung One UI Screen Pinning; do not implement fake kiosk mode or force pinning programmatically.

## Canonical image order

The canonical set is the 59 numbered WebP files from `01-arich.webp` through `59-ladybird.webp`, sorted by numeric filename. `03-corgi.webp`, if present in the old source, is obsolete and must not be included.

`01-arich.webp` is the user's Chinese Crested Powderpuff dog and must remain the first slide. Its visible label is `Ари`.

## Current launcher icon source

Use the existing `zveryata-v3` artwork from the PWA to create Android adaptive launcher icon resources; do not reference web manifest icons at runtime.
