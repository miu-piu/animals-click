# Prompt for Google AI Studio (Android)

Use this repository as the Android workspace for the native app **«Зверята»**.

Do not create a web app. Do not replace this with React/Next.js. Build a native Android app using **Kotlin + Jetpack Compose**.

Before coding, read `SOURCE-SPEC.md` and follow it as the product specification.

## ABSOLUTE ASSET RULE

Use ONLY the existing approved animal photographs from `miu-piu/kids-games/animals-click/assets/images/`.

Run `python3 scripts/sync_assets.py` first and use exactly those synced files.

Do NOT generate new animal images, AI replacements, stock images, redesigned scenes, or substitutes. If any expected source file is missing, report the missing filename and stop rather than generating a replacement.

The phone screen and the source photographs have different aspect ratios. Do NOT solve that by changing the images. Solve it only in layout:

- full-screen background = pure black `#000000`;
- show exactly one sharp canonical photo centered on that black background;
- use `ContentScale.Fit` or equivalent;
- the full original photo must remain visible and preserve aspect ratio;
- the animal must never be cropped or stretched;
- unused screen area stays pure black;
- do NOT use a blurred duplicate, crop/fill background, gradients, cards, borders, captions, or other UI.

Then:

1. Run `python3 scripts/sync_assets.py` to download the canonical photos, current v3 launcher artwork, and PWA reference files from `miu-piu/kids-games/animals-click` and package the exact 59 photos into the Android app.
2. Use the existing native Android project in this repository; do not create a competing replacement project.
3. Use package name and namespace `com.miupiu.zveryata`. Do not leave production code under `com.example`.
4. Set the application label to `Зверята`.
5. Package all synced canonical gallery photos locally so the installed app works completely offline.
6. Implement the gallery behavior exactly as described in `SOURCE-SPEC.md`: one photo, no visible controls, four-direction swipe navigation, taps do nothing, 30-second automatic next slide, circular navigation, protection against accidental/double gestures.
7. First slide must be `01-arich.webp`.
8. Render each slide on a pure black fullscreen canvas with the source photo centered using `ContentScale.Fit`.
9. Use edge-to-edge immersive fullscreen Android APIs.
10. Keep compatibility with normal Android/Samsung Screen Pinning. Do not fake kiosk mode and do not force pinning programmatically.
11. Use the current `zveryata-v3` artwork to create/use the Android launcher icon. Do not invent a new puppy icon.
12. Avoid runtime network permissions and dependencies. No ads, analytics, Firebase, Gemini API, login, tracking, external links, push notifications, camera, microphone, contacts, or location.
13. Use memory-safe image loading; do not decode all 59 full-size bitmaps simultaneously.
14. Build the project and fix build errors before stopping.
15. Run or preview it on an Android emulator/device if available and verify gestures, 30-second auto-advance, wraparound, fullscreen black-background rendering, offline design, app label, first slide, exact source images, and launcher icon.

Important: the original PWA in `miu-piu/kids-games/animals-click` must remain untouched. This repository is the separate native Android implementation.

When finished, report:

- project structure
- package name and namespace
- min/target SDK
- number of bundled canonical animal images
- exact first slide
- where the local image assets live
- build result
- what behaviors were actually verified
- confirmation that NO animal image was newly generated or substituted
- how to install it on a physical Android device / use AI Studio's Install on Device flow
