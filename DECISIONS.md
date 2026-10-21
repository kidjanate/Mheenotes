# Decisions log

Calls made without asking, for review. Newest last.

1. **Accent color** `#3A63F0` (light) / `#7090FF` (dark): calm ink blue; keeps colorful ink as the focus.
2. **Fonts** per weight with Android `Typeface.CustomFallbackBuilder` (Inter Latin → Inter Latin Ext → IBM Plex Sans Thai), so Thai glyphs use Plex instead of the system font. Needs API 29, which matches minSdk.
3. **Strings** are Compose Multiplatform resources: `ui/src/commonMain/composeResources/values/strings.xml` (English, the fallback) and `values-th/strings.xml`, read with `stringResource(Res.string.x)` / `getString` (widgets, toasts). The in-app language (System / English / Thai) sets the process locale and a root `LocalConfiguration`, so every string re-resolves live.
4. **Storage** is SQLite per notebook instead of a custom journal: crash safety from WAL, random page access, one-file export.
5. **Page units** are PDF points, so imported PDF pages map 1:1.
6. **Ink rendering** draws opaque round-capped segments per sample pair; highlighters are a single translucent path under other ink. Pencil is opaque (no texture) in release 1.
7. **No motion prediction**: front-buffer rendering can't un-draw predicted points; the front buffer already removes most latency.
8. **Covers** are the first page thumbnail (no separate cover colors).
9. **Notebook title duplication**: the "(2)" suffix for duplicates is not localized.
10. **Tiles over vectors**: finished ink is rasterized into 512 px tiles per zoom level; panning composites bitmaps. A whole-page preview tile sits underneath so fast zooms never show blank paper.
11. **Same-frame handoff**: a finished stroke stays in the canvas view as a vector until every tile under it is re-rendered, then disappears in the same frame the tile appears. No flicker.
12. **Low-latency ink** uses `CanvasFrontBufferedRenderer` (front buffer) for pen/pencil. Highlighters stay in the normal path, since incremental translucent segments would darken where they join. A settings toggle turns it off.
13. **Palm rejection**: fingers are ignored while the pen hovers (400 ms grace); two-finger tap = undo, three-finger tap = redo.
14. **Legacy import**: `.mynote` lengths are microns, except PDF pages, which the old app sized in points × 1000. PDF pages are scaled to the PDF's real size; audio notes are skipped (release 1 scope).
15. **Covers** are generated lazily from the database when missing (imports, fixtures), on 3 background threads.
16. **Library screen to editor**: the incoming screen is on top during the transition, so a stroke started mid-animation lands in the editor.
17. **Release builds** keep Room database constructors (WorkManager, pulled in by Glance widgets, creates one reflectively).
