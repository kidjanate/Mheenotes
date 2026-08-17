# Mheenotes

Mheenotes is an Android-first, tablet-focused handwriting and document editor. The 0.9 release candidate implements the nine milestones in `prompt.md`: low-latency AndroidX Ink authoring, native editable notebooks, continuous pages, PDF annotation/export, mixed lasso selection, editable images/text/shapes, Action Maps, scribble erase, library organization, themes, recovery and bounded rendering caches.

## Open and run

1. Open this repository folder in Android Studio.
2. Allow the Gradle sync to finish.
3. Enable USB debugging on the Xiaomi Pad 5 and connect it.
4. Select the tablet as the target and run the `app` configuration.

The current build uses AndroidX Ink for active-stylus authoring, supports finger scrolling plus two-finger pan/zoom, and incrementally autosaves completed strokes and undo/redo operations to a checksummed recovery journal. `.mynote` files embed their PDF and image assets.

The latest locally built APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Command-line verification

```sh
./gradlew test assembleDebug lintDebug
```

The complete user-facing feature inventory is in [docs/FEATURES.md](docs/FEATURES.md). The architecture baseline is in [docs/TECHNICAL_DESIGN.md](docs/TECHNICAL_DESIGN.md), and the implementation/release-gate status is in [docs/MILESTONE_STATUS.md](docs/MILESTONE_STATUS.md).

The real-device acceptance checklist is in [docs/MILESTONE_1_DEVICE_TEST.md](docs/MILESTONE_1_DEVICE_TEST.md).
