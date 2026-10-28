# Зверята — Android source specification

This repository is the clean Android workspace for the native version of **«Зверята»**.

## Canonical web source

The existing PWA remains untouched in:

- repository: `miu-piu/kids-games`
- folder: `animals-click/`
- live PWA: `https://miu-piu.github.io/kids-games/animals-click/`

Use the PWA only as a behavior/asset reference. The Android app must be native Kotlin + Jetpack Compose and must not depend on the web app at runtime.

## Android identity

- App name: `Зверята`
- Suggested package: `com.miupiu.zveryata`
- Primary orientation: portrait
- Fully offline after install
- No account, analytics, ads, Gemini API, Firebase, network dependency, camera, microphone, location, contacts, notifications, or external links.

## Gallery behavior

One animal image fills the experience. No visible controls, captions, pagination, toolbar, menu, counters, or buttons.

Gestures:

- swipe left -> next
- swipe right -> previous
- swipe up -> next
- swipe down -> previous
- tap -> nothing
- tiny/ambiguous gesture -> nothing
- one confident gesture -> exactly one transition
- protect against double triggering / rapid accidental child touches

Autoplay:

- 30 seconds without a successful manual swipe -> next slide
- successful manual swipe restarts the 30 s timer
- taps do not restart the timer
- automatic transition restarts the timer
- wrap at both ends

## Image rendering

- Keep the main photo fully visible and centered.
- Do not crop the animal.
- Preserve aspect ratio.
- Fill unused screen area with a blurred/dimmed enlarged version of the same photo behind the sharp foreground image.
- Work correctly on different Android screen sizes.
- Preload only current/next/previous images or another memory-safe small window; do not decode the entire gallery at once.

## Android system behavior

Use edge-to-edge immersive fullscreen with normal Android APIs.

The app must remain compatible with Android / Samsung One UI **Screen Pinning / «Закрепить приложение»**. Do not implement a fake kiosk mode and do not programmatically force Screen Pinning.

## Canonical image order for Android

Use these slides in this order:

1. `01-arich.webp`
2. `02-kitten.webp`
3. `03-irish-setter.webp`
4. `04-rabbit.webp`
5. `05-calf.webp`
6. `06-goat.webp`
7. `07-lamb.webp`
8. `08-chick.webp`
9. `09-duckling.webp`
10. `10-hedgehog.webp`
11. `11-squirrel.webp`
12. `12-ferret.webp`
13. `13-fox.webp`
14. `14-raccoon.webp`
15. `15-fawn.webp`
16. `16-bear.webp`
17. `17-lynx.webp`
18. `18-chipmunk.webp`
19. `19-amur-leopard.webp`
20. `20-tiger.webp`
21. `21-lion.webp`
22. `22-elephant.webp`
23. `23-giraffe.webp`
24. `24-zebra.webp`
25. `25-camel.webp`
26. `26-capybara.webp`
27. `27-alpaca.webp`
28. `28-panda.webp`
29. `29-hippo.webp`
30. `30-rhino.webp`
31. `31-tit.webp`
32. `32-owl.webp`
33. `33-crow.webp`
34. `34-magpie.webp`
35. `35-pigeon.webp`
36. `36-flamingo.webp`
37. `37-hummingbird.webp`
38. `38-cuckoo.webp`
39. `39-peacock.webp`
40. `40-swan.webp`
41. `41-whale.webp`
42. `42-shark.webp`
43. `43-penguin.webp`
44. `44-seal.webp`
45. `45-dolphin.webp`
46. `46-octopus.webp`
47. `47-seahorse.webp`
48. `48-crab.webp`
49. `49-ray.webp`
50. `50-walrus.webp`
51. `51-jellyfish.webp`
52. `52-snake.webp`
53. `53-lizard.webp`
54. `54-butterfly.webp`
55. `55-bee.webp`
56. `56-snail.webp`
57. `57-lemur.webp`
58. `58-dragonfly.webp`
59. `59-ladybird.webp`

If `03-corgi.webp` still exists in the old asset directory, treat it as obsolete and do not include it unless explicitly requested later.

## Arich

`01-arich.webp` is the user's Chinese Crested Powderpuff dog and should be the first slide in the Android version.

## Current launcher icon source

The current PWA manifest uses:

- `assets/icons/zveryata-v3-192.png`
- `assets/icons/zveryata-v3-512.png`
- `assets/icons/zveryata-v3-maskable-512.png`

Also available:

- `favicon-32.png`
- `favicon-48.png`
- `apple-touch-icon.png`

For Android, create proper adaptive launcher icon resources from the current `zveryata-v3` artwork rather than referencing web manifest icons at runtime.

## Asset sync

Run `python3 scripts/sync_assets.py` from repository root to copy the current source photos and icons from the public `kids-games/animals-click` source into `reference/assets/`.

The final native Android project should package the photos locally in APK/AAB resources or assets. Runtime network access is not required.
