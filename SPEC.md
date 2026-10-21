# Mheenotes (rewrite)

Tablet-first handwriting notebook. Kotlin Multiplatform + Compose Multiplatform; release 1 targets Android tablets (API 29+). The legacy app lives on `main`.

## Goals
- Fast first: 1,000 notebooks in the library, 20,000 strokes on a page, 300-page PDFs, 120 fps pan/zoom on Xiaomi Pad 5.
- Calm, iPad-like UI built entirely from custom components (no Material widgets). Light/dark, Inter + IBM Plex Sans Thai, Lucide icons, English + Thai.
- Rich spring motion everywhere except the ink path.

## Release 1 scope
Library (folders, cover grid/list, search, sort, favorites, trash, multi-select), pen/pencil/highlighter, whole and partial eraser, undo/redo, instant autosave with crash safety, page templates and paper sizes, lasso (move/scale/rotate/copy/delete/recolor), images, text boxes, shapes (+ hold-to-snap), scribble erase, PDF import and annotation, export (PDF, PNG, notebook file), tabs, page overview rail, home-screen widgets, one-time import of legacy `.mynote` files.

Deferred: OCR, audio notes, Action Maps, Floating Notes, stylus hover/button/radial menu, custom themes, sync.

## Architecture
- `core` (KMP, commonMain): model, geometry, spatial grid, item codec, SQLite storage (androidx.sqlite bundled driver), document session with undo. Pure Kotlin, tested on the JVM.
- `ui` (KMP, Compose): design system, screens, shared page renderer (Compose graphics). `androidMain` holds the ink canvas (tile cache, front-buffered wet ink), PDF, import/export, fonts.
- `app` (Android application): entry point, widgets.

### Storage
- `library.db`: notebook and folder index, settings, ink activity. Loaded fully into memory at start.
- `notebooks/<id>/notebook.db` + `assets/`: pages and items. Every edit is one WAL transaction on a single ordered writer. Deleted rows are tombstoned so sync can be added later.

### Rendering
- Finished ink is rasterized into 512 px tiles per zoom level on background threads, from immutable page snapshots queried through a uniform grid. Pan/zoom only composites bitmaps.
- The stroke being drawn goes to a front-buffered SurfaceView, using the same geometry as the tiles, so wet and dry ink match exactly.
