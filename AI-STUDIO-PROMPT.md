# Prompt for Google AI Studio (Android)

Use this repository as the Android workspace for the native app **«Зверята»**.

Do not create a web app. Do not replace this with React/Next.js. Build a native Android app using **Kotlin + Jetpack Compose**.

Before coding, read `SOURCE-SPEC.md` and follow it as the product specification.

## ABSOLUTE ASSET RULE

Use ONLY the existing approved animal photographs from `miu-piu/kids-games/animals-click/assets/images/`.

Run `python3 scripts/sync_assets.py` first and use exactly those synced files.

Do NOT generate new animal images, AI replacements, stock images, redesigned scenes, or substitutes. If any expected source file is missing, report the missing filename and stop rather than generating a replacement.

The phone screen and the source photographs have different aspect ratios. Do NOT solve that by changing the images. Solve it only in layout:

- full-screen blurred/dimmed background = same canonical photo with crop/fill behavior;
- sharp foreground = exact same canonical photo centered with `ContentScale.Fit` or equivalent;
- the full sharp photo must remain visible and preserve aspect ratio;
- the animal must never be cropped;
- the decorative blurred background may crop;
- no black/white bars, cards, borders, captions, or UI.

Then:

1. Run `python3 scripts/sync_assets.py` to download the canonical photos, current v3 launcher artwork, and PWA reference files from `miu-piu/kids-games/animals-click` into `reference/`.
2. Create a normal buildable Android project in this repository.
3. Use package name and namespace `com.miupiu.zveryata`. Do not leave production code under `com.example`.
4. Set the application label to `Зверята`.
5. Package all synced canonical gallery photos locally so the installed app works completely offline.
6. Implement the gallery behavior exactly as described in `SOURCE-SPEC.md`: one photo, no visible controls, four-direction swipe navigation, taps do nothing, 30-second automatic next slide, circular navigation, protection against accidental/double gestures.
7. First slide must be `01-arich.webp`.
8. Render each slide using the fit-plus-blurred-background layout described above.
9. Use edge-to-edge immersive fullscreen Android APIs.
10. Keep compatibility with normal Android/Samsung Screen Pinning. Do not fake kiosk mode and do not force pinning programmatically.
11. Use the current `zveryata-v3` artwork to generate proper Android adaptive launcher icons. Do not invent a new puppy icon.
12. Avoid runtime network permissions and dependencies. No ads, analytics, Firebase, Gemini API, login, tracking, external links, push notifications, camera, microphone, contacts, or location.
13. Use memory-safe image loading: current + adjacent slides or another small window, not all full-size bitmaps decoded simultaneously.
14. Build the project and fix build errors before stopping.
15. Run or preview it on an Android emulator/device if available and verify gestures, 30-second auto-advance, wraparound, fullscreen rendering, offline design, app label, first slide, exact source images, and launcher icon.

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
