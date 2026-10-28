# Prompt for Google AI Studio (Android)

Use this repository as the Android workspace for the native app **«Зверята»**.

Do not create a web app. Do not replace this with React/Next.js. Build a native Android app using **Kotlin + Jetpack Compose**.

Before coding, read `SOURCE-SPEC.md` and follow it as the product specification.

Then:

1. Run `python3 scripts/sync_assets.py` to download the canonical photos, current v3 launcher artwork, and PWA reference files from `miu-piu/kids-games/animals-click` into `reference/`.
2. Create a normal buildable Android project in this repository.
3. Use package name `com.miupiu.zveryata` unless Android tooling requires a different valid package.
4. Set the application label to `Зверята`.
5. Package all gallery photos locally so the installed app works completely offline.
6. Implement the gallery behavior exactly as described in `SOURCE-SPEC.md`: one photo, no visible controls, four-direction swipe navigation, taps do nothing, 30-second automatic next slide, circular navigation, protection against accidental/double gestures.
7. Render the full sharp photo centered without cropping the animal; use a blurred/dimmed enlarged copy of the same photo behind it to fill unused screen space.
8. Use edge-to-edge immersive fullscreen Android APIs.
9. Keep compatibility with normal Android/Samsung Screen Pinning. Do not fake kiosk mode and do not force pinning programmatically.
10. Use the current `zveryata-v3` artwork to generate proper Android adaptive launcher icons.
11. Avoid runtime network permissions and dependencies. No ads, analytics, Firebase, Gemini API, login, tracking, external links, push notifications, camera, microphone, contacts, or location.
12. Use memory-safe image loading: current + adjacent slides or another small window, not all full-size bitmaps decoded simultaneously.
13. Build the project and fix build errors before stopping.
14. Run or preview it on an Android emulator/device if available and verify gestures, 30-second auto-advance, wraparound, fullscreen rendering, offline design, app label, and launcher icon.

Important: the original PWA in `miu-piu/kids-games/animals-click` must remain untouched. This repository is the separate native Android implementation.

When finished, report:

- project structure
- package name
- min/target SDK
- where the local image assets live
- build result
- what behaviors were verified
- how to install it on a physical Android device / use AI Studio's Install on Device flow
