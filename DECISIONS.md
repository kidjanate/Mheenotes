# Decisions log

Calls made without asking, for review. Newest last.

1. **Accent color** `#3A63F0` (light) / `#7090FF` (dark): calm ink blue; keeps colorful ink as the focus.
2. **Fonts** per weight with Android `Typeface.CustomFallbackBuilder` (Inter Latin → Inter Latin Ext → IBM Plex Sans Thai), so Thai glyphs use Plex instead of the system font. Needs API 29, which matches minSdk.
3. **Strings** are typed Kotlin objects (`En`, `Th`) instead of Compose resources: no code generation, compile-time checked. Language follows the system/per-app language setting (Android 13 per-app picker via `localeConfig`).
4. **Storage** is SQLite per notebook instead of a custom journal: crash safety from WAL, random page access, one-file export.
5. **Page units** are PDF points, so imported PDF pages map 1:1.
6. **Ink rendering** draws opaque round-capped segments per sample pair; highlighters are a single translucent path under other ink. Pencil is opaque (no texture) in release 1.
7. **No motion prediction**: front-buffer rendering can't un-draw predicted points; the front buffer already removes most latency.
8. **Covers** are the first page thumbnail (no separate cover colors).
9. **Notebook title duplication**: the "(2)" suffix for duplicates is not localized.
