# Mheenotes Technical Design

Status: architecture baseline for Milestone 1  
Last reviewed: 2026-08-17

## Product and engineering principles

Mheenotes is an offline-first Android tablet document editor whose primary interaction is active-stylus handwriting. The editable notebook scene, not a PDF or rendered bitmap, is the source of truth. A visible stroke is acknowledged immediately in memory, committed incrementally in the background, and never waits for database or file I/O on the input/render thread.

The first release target is Xiaomi Pad 5, but no domain model or persisted format may encode Xiaomi-specific assumptions. Missing pressure, tilt, hover, button, and palm-rejection signals always have defined fallbacks.

Decisions made for the baseline:

- `minSdk = 23`; device-specific improvements are selected at runtime.
- `compileSdk = 37`, Android Gradle Plugin 9.3.0, Gradle 9.5.0, JDK 17.
- Stable Compose BOM `2026.06.00` and stable AndroidX Ink `1.0.0`.
- Stable APIs are preferred. AndroidX Ink 1.1 alpha-only partial erasing is not part of the persistence contract.
- Coordinates and sizes are stored in page space, expressed in micrometres, independent of screen density and zoom.
- IDs are UUID-compatible 128-bit values represented canonically as strings at interchange boundaries.
- Wall-clock timestamps are metadata only. Ordering and recovery use monotonic sequence numbers.

## 1. Application architecture

The app uses a layered, feature-oriented architecture with explicit ports between the editor and infrastructure:

```text
Compose screens
    -> presentation state holders
        -> editor/document use cases
            -> domain model and command engine
                -> repository ports
                    -> Room metadata / asset files / journal / PDF services

MotionEvent -> InputRouter -> StrokeBuilder -> LiveInkRenderer
                                      |-> FinalizeStroke command
                                      |-> autosave journal
```

Rules:

- UI observes immutable state and emits user intents; it never opens files or writes Room entities.
- Domain modules do not depend on Compose, Android views, Room, or PDF APIs.
- AndroidX Ink types are adapted at the ink boundary. They are not the native notebook schema.
- The editor owns one serialized document mutation actor. Rendering may be parallel, but document revisions are totally ordered.
- Slow work is cancellation-aware and runs on bounded dispatchers: document I/O, PDF rendering, thumbnails, image decode, and export.
- Dependency wiring begins manually. A DI framework is deferred until object-graph complexity justifies it.

## 2. Gradle/module structure

Modules are introduced only when their boundary is exercised; the target structure is:

```text
:app                         Android entry point and dependency wiring
:core:model                  platform-neutral document/value models
:core:commands               command history and document reducer
:core:storage                repository contracts, container and recovery logic
:core:database               Room entities/DAOs for library and open-workspace metadata
:core:designsystem           theme, typography, icons and reusable components
:ink:engine                  stroke building, pressure, smoothing and hit testing
:ink:android                 MotionEvent and AndroidX Ink adapters/rendering
:document:editor             editor orchestration and viewport state
:document:format             .mynote manifest, codecs and migrations
:document:pdf                PDF import, render, search and export adapters
:feature:library
:feature:editor
:feature:diagnostics
:feature:settings
:feature:actionmap
```

Milestone 1 starts with `:app`, `:core:model`, and `:ink:engine`. Android-specific authoring remains in `:app` until its port is proven, then moves to `:ink:android`. This avoids a large empty module graph while preserving the planned dependency direction.

Central version declarations live in `gradle/libs.versions.toml`. Every dependency version is pinned; no dynamic versions are allowed. Gradle configuration cache and build cache are enabled.

## 3. Kotlin package structure

Root namespace: `com.mheenotes`.

```text
com.mheenotes.app
com.mheenotes.core.model
com.mheenotes.core.commands
com.mheenotes.core.storage
com.mheenotes.core.database
com.mheenotes.core.designsystem
com.mheenotes.ink.engine
com.mheenotes.ink.android
com.mheenotes.document.editor
com.mheenotes.document.format
com.mheenotes.document.pdf
com.mheenotes.feature.library
com.mheenotes.feature.editor
com.mheenotes.feature.diagnostics
com.mheenotes.feature.settings
com.mheenotes.feature.actionmap
```

Packages describe a capability, not a generic `utils` or `managers` bucket. Public APIs are small interfaces and immutable values; implementation classes are internal by default.

## 4. Document object model

```kotlin
data class Notebook(
    val id: NotebookId,
    val formatVersion: Int,
    val title: String,
    val createdAt: InstantValue,
    val modifiedAt: InstantValue,
    val orderedPageIds: List<PageId>,
    val bookmarks: List<Bookmark>,
    val sourceDocuments: List<SourceDocumentRef>,
    val settings: NotebookSettings,
)
```

Notebook loading is hierarchical. Opening a notebook loads its manifest and page index; page content is loaded only for visible/nearby pages. Objects use opaque `type` tags and length-delimited payloads so unknown future object types can be preserved when possible rather than discarded.

Domain invariants include unique IDs, no duplicate page-order entries, at least one page in an editable notebook, finite geometry, valid page dimensions, and resolvable asset references.

## 5. Stroke model

```kotlin
data class InkStroke(
    val id: StrokeId,
    val pageId: PageId,
    val tool: InkTool,
    val style: StrokeStyle,
    val input: StrokeInputData,
    val bounds: PageRect,
    val zIndex: Long,
)

data class StrokeSample(
    val xUm: Float,
    val yUm: Float,
    val elapsedMicros: Long,
    val pressure: Float?,
    val tiltRadians: Float?,
    val orientationRadians: Float?,
)
```

Samples are stored as a versioned binary blob per stroke, not one Room row per point. Encoding uses a small header, feature flags, first absolute point, delta-coded coordinates/time, and quantized optional axes. The original normalized input remains authoritative; generated meshes are disposable caches. Bounds are stored and verified during decode.

Pressure-off strokes retain raw pressure when supplied so changing or diagnosing the brush later does not destroy input evidence; the style records whether pressure affected rendering.

## 6. Page model

```kotlin
data class NotebookPage(
    val id: PageId,
    val size: PageSize,
    val rotation: QuarterTurn,
    val background: PageBackground,
    val pdfSource: PdfPageRef?,
    val orderedObjectIds: List<PageObjectId>,
    val revision: Long,
)
```

Page space has an origin at the unrotated page's top-left. Rotation and viewport transforms are presentation transforms and never destructively rewrite object geometry. Page ordering belongs to the notebook manifest/index rather than to the PDF.

## 7. Image, text and shape object model

Every object has an ID, page ID, affine transform, local bounds, z-index, visibility, and schema version.

- `ImageObject`: content-addressed asset ID, intrinsic pixel size, crop rectangle, transform, opacity and optional color adjustments.
- `TextObject`: UTF-8 text, box bounds, paragraph style, and versioned attributed runs. Fonts use portable family descriptors with deterministic fallback.
- `ShapeObject`: discriminated geometry (`line`, `arrow`, `rectangle`, `roundedRectangle`, `ellipse`, `triangle`, future path), stroke/fill styles and transform.
- `InkStrokeObject`: the stroke model above; highlighter is an ink tool/style, not a flattened layer.

Z-order uses sparse signed 64-bit keys. Rebalancing is a rare page transaction rather than renumbering on every move.

## 8. Native `.mynote` file design

The reliable working representation is an application-private directory; the portable `.mynote` is a ZIP-compatible package created for export/backup.

```text
<notebook-id>/
  manifest.pb
  workspace.db
  journal/
    segment-000001.log
  pages/
    <page-id>.pb
  ink/
    <stroke-id>.ink
  assets/
    sha256-<digest>
  sources/
    <source-id>.pdf
  cache/                    excluded from portable package
    thumbnails/
    pdf-renders/
    meshes/
```

The exported package contains a small uncompressed `manifest.json` for identification/interoperability plus canonical binary content. Entries have size limits and SHA-256 checksums. Import rejects path traversal, duplicate paths, decompression bombs, invalid finite numbers, unsupported mandatory features, and checksum failures. Import extracts into a staging directory, validates completely, then atomically publishes it.

The portable file is never edited in place. An export writes `name.mynote.partial`, flushes/closes it, validates its directory/checksums, then atomically replaces the destination when the provider supports it. Existing valid data is retained on failure.

## 9. Persistence strategy

Room stores library/folder/trash metadata, notebook locations, recent/favorite state, and searchable indexes. An open notebook's scene data lives in its workspace files so huge stroke blobs do not churn the library database.

All mutations flow through a `DocumentStore` port:

1. validate a command against revision N;
2. assign operation sequence N+1;
3. append a checksum-framed operation to the journal;
4. publish the in-memory state and save status;
5. asynchronously compact operations into page snapshots;
6. truncate only journal segments fully covered by a durable snapshot.

Asset files are immutable and content-addressed. Writes use create-new temporary files followed by atomic rename. Reference counts are reconciled during maintenance; garbage collection has a grace period and never runs in the input path.

## 10. Autosave and recovery

Stroke rendering becomes visible before storage, but a completed stroke is queued immediately to the single-writer persistence actor. Autosave is incremental and debounced only for naturally coalescible transformations such as dragging an image; final gesture state is always flushed on pointer-up.

Save state is `Saved(revision)`, `Saving(from, to)`, `Failed(revision, reason)`, or `RecoveryRequired`. A small non-blocking indicator may observe it.

Journal records are length-prefixed and checksum-framed. Recovery scans until the last valid record; a torn tail is quarantined, not applied. Periodic page snapshots plus manifest checkpoints bound replay time. On lifecycle stop the app requests a bounded flush but correctness does not depend on receiving lifecycle callbacks.

Low-storage and write failure leave dirty in-memory state intact, stop compaction/export, show a persistent warning, and offer recovery/export once storage becomes available.

## 11. Undo/redo command architecture

```kotlin
interface DocumentCommand {
    val id: CommandId
    fun apply(document: DocumentState): CommandResult
    fun invert(before: DocumentState, result: CommandResult): DocumentCommand
}
```

Commands contain intent and compact inverse data, not full-document copies. A transaction groups many commands into one user-visible history entry. Continuous transforms coalesce by object and gesture ID. New edits clear the redo branch. Autosave persists both the resulting operations and the history checkpoint required for crash-consistent undo.

Large destructive inverses reference immutable tombstone payloads until the history entry expires. Page deletion therefore restores the full page, objects, bookmark state, and order.

## 12. Input and rendering pipeline

```text
MotionEvent dispatch
 -> classify pointers/tool/button/palm cancellation
 -> copy current + historical samples into a preallocated buffer
 -> screen-to-page transform using one captured viewport revision
 -> sanitize and normalize optional axes
 -> stroke builder / stabilizer
 -> AndroidX Ink in-progress renderer
 -> frame

pointer up
 -> finalize immutable stroke
 -> command actor
 -> spatial index + retained renderer
 -> autosave journal
```

The hot path does no file I/O, logging, database work, image decoding, or unbounded allocation. Diagnostics observes a throttled copy rather than intercepting the path. Viewport changes are frozen for an active stylus stroke, while two-finger navigation is rejected or deferred according to the selected input mode.

Finished rendering uses page tiles and culls by viewport. In-progress ink is a separate low-latency overlay. Document-to-screen matrices are shared by input, rendering, hit testing, selection, and export tests to prevent drift.

## 13. AndroidX Ink integration plan

Use stable AndroidX Ink 1.0.0 authoring Compose integration for in-progress low-latency ink and its stable stroke/render/storage primitives where they improve the implementation. An adapter converts sanitized `MotionEvent` samples into Ink inputs and converts completed Ink strokes into the Mheenotes stroke schema.

AndroidX Ink serialization can be retained as a cache/secondary payload, but `.mynote` owns an independent versioned input codec. This permits alternative renderers and non-Android clients.

The integration is behind:

```kotlin
interface LiveInkSurface {
    fun begin(pointer: PointerSample, brush: BrushSpec)
    fun append(current: PointerSample, history: List<PointerSample>)
    fun finish(pointer: PointerSample): CompletedStroke
    fun cancel()
}
```

Instrument Xiaomi API/device behavior before enabling prediction or device-specific low-latency paths by default. Use runtime feature detection for API 29/33-specific rendering implementations.

## 14. Pressure handling and normalization

Raw pressure is sanitized to a finite non-negative value. Calibration stores observed useful minimum `pMin`, useful maximum `pMax`, dead zone, and curve exponent per input device descriptor. Normalization is:

```text
t = clamp((raw - pMin) / max(pMax - pMin, epsilon), 0, 1)
response = t ^ gamma
width = lerp(minWidth, maxWidth, response)
```

A monotonic piecewise cubic curve may replace `gamma` later. Device calibration, tool settings, and brush response remain separate. When pressure is absent or disabled, a stable configurable nominal response is used. Samples keep `null` distinct from hardware-reported zero.

Smoothing operates on position and optionally pressure with bounded look-ahead. Presets map to documented numeric parameters; “high” stabilization may never delay the visible stroke without a fixed latency budget.

## 15. PDF architecture

`PdfDocumentSource` abstracts metadata, outline, text/search, links and page rendering. `PdfExportEngine` abstracts vector-preserving and composited export independently.

Import through the Storage Access Framework copies source bytes into immutable app-controlled `sources/` storage while calculating a digest. Notebook pages reference `(sourceId, sourcePageIndex)` and may be freely mixed with blank pages. Rendering uses bounded worker concurrency, cancellation, page/tile cache keys including source digest, page index, scale bucket, crop and rotation.

The initial platform implementation uses Android PDF APIs where capable. Unsupported documents fail clearly or use a replaceable fallback. Export first composes source PDF content with editable overlays; a controlled raster fallback is per-page rather than automatically flattening the full notebook.

## 16. Spatial indexing strategy

Maintain one mutable R-tree per loaded page, indexing conservative page-space bounds for strokes and objects. Persisting the tree is optional; it is rebuildable from object bounds. Small pages may begin with a packed uniform grid if profiling proves it faster, behind the same `SpatialIndex` interface.

Queries first select nearby bounds, then use exact geometry:

- whole-stroke eraser: swept-circle versus polyline/mesh;
- partial eraser: segment intersection and stroke splitting;
- lasso: polygon containment/intersection;
- scribble: swept path candidates plus intersection/density features.

Index updates are part of the same document revision as object mutations.

## 17. Action Map recognition design

Action Map is a local template recognizer behind `GestureRecognizer`. Each template stores 3–5 multi-stroke examples, activation policy, rotation sensitivity, threshold, mapped action ID, and schema version.

Recognition pipeline:

1. reject invalid/too-short captures;
2. resample each stroke while preserving stroke boundaries and order;
3. translate to centroid and normalize bounding scale with aspect-ratio safeguards;
4. rotate only for rotation-invariant templates;
5. compare point-cloud/path distance, direction sequence, stroke count, intersections and aspect ratio;
6. aggregate against examples and estimate margin over the second-best match;
7. require both absolute confidence and ambiguity margin.

The generic action registry owns execution and destructive-risk classification. Recognition never directly calls editor features. Destructive actions require a higher threshold and confirmation/preview. Recognition runs only during explicit Action Map activation.

## 18. Cache strategy

All caches are derivable and independently clearable:

- in-progress/final stroke meshes keyed by stroke content hash + brush renderer version;
- PDF tiles keyed by source digest + page + render parameters;
- thumbnails keyed by page revision + theme/background parameters;
- decoded image bitmaps keyed by asset digest + target size + color space;
- page state LRU keyed by page ID and durable revision.

Use memory budgets derived from device memory class and disk budgets with an LRU journal. Visible and adjacent pages are pinned. Cache writes are atomic, corruption is a miss, and notebook content never resides only in cache.

## 19. UI design system proposal

Working visual identity: quiet paper-focused surfaces with graphite neutrals and one saturated ink-blue accent. The canvas remains dominant.

- spacing: 4, 8, 12, 16, 24, 32, 48 dp;
- radii: 8 dp controls, 12 dp panels, 20 dp sheets, pill tool selectors;
- touch targets: minimum 48 dp, 56 dp for primary stylus tools;
- typography: expressive but restrained display, highly legible UI/body scale;
- elevation: mostly tonal separation, with shadows only for floating tools and transient surfaces;
- motion: 100 ms state response, 180 ms small transitions, 280 ms panels; respect reduced motion;
- editor breakpoints: compact under 600 dp, medium 600–839 dp, expanded at 840 dp+;
- editor: collapsible thumbnail rail, centered page/canvas, movable dock with mirrored handedness modes;
- component states: selected, hovered, pressed, disabled, focused, saving/error, stylus proximity where useful.

Material 3 supplies accessibility semantics and primitives, but tokens and components are owned by `:core:designsystem`. Dynamic color is opt-in because paper/ink contrast must remain predictable.

## 20. Key technical risks

1. **Perceived ink latency:** validate on Xiaomi hardware before expanding scope; instrument event-to-frame time and allocations.
2. **Palm rejection variance:** combine tool classification, cancellation handling and explicit input modes; never claim firmware-level control.
3. **AndroidX Ink evolution:** pin stable 1.0.0 and isolate adapters; do not persist alpha-only mesh structures.
4. **Data loss/torn writes:** append-only checksummed journal, atomic snapshots, recovery tests and low-storage fault injection.
5. **Coordinate drift:** one page-space convention and property tests for transform round-trips across zoom/rotation.
6. **Huge notebooks:** hierarchical loading, bounded caches, per-page indices and incremental persistence from the first milestone.
7. **PDF fidelity:** keep a replaceable engine boundary and explicit per-page raster fallback.
8. **Partial erase complexity:** preserve original data/inverses and delay alpha library APIs until durable serialization exists.
9. **ZIP import security:** strict path, count, size, compression ratio and checksum validation.
10. **Over-modularization:** introduce target modules only when real code exercises the boundary.

## 21. Milestone 1 task breakdown

### M1.1 Project and measurement foundation

- Create Gradle project, Compose shell, theme and CI-ready test tasks.
- Add a single blank page and editor viewport model.
- Add frame/input timing counters available only in diagnostics builds.

Exit: app launches on a tablet and presents a stable blank page in portrait, landscape and split-screen.

### M1.2 Input diagnostics

- Capture pointer ID, action, source, device, tool type, button state, current/historical samples, pressure, tilt, orientation, distance and cancellation.
- Build a throttled diagnostics screen and raw session export for comparison.
- Test Xiaomi stylus tip, buttons, hover, palm and finger combinations.

Exit: a device capability report documents actual Xiaomi signals and ranges.

### M1.3 Pen vertical slice

- Add stable AndroidX Ink authoring/rendering.
- Implement screen/page transforms, stylus/finger routing and a pressure-sensitive pen.
- Add pressure on/off, initial normalization curve and low/medium stabilization.
- Ensure the hot path is allocation-bounded and has no persistence calls.

Exit: subjective writing test passes and measured event-to-visible latency has no unexplained spikes.

### M1.4 Commands, undo and redo

- Finalize strokes into the platform-neutral model.
- Implement `AddStroke`/`DeleteStroke` commands and bounded history.
- Render finished strokes and keep live/final transition visually seamless.

Exit: repeated undo/redo preserves exact geometry and style.

### M1.5 Viewport

- Implement two-finger pan, pinch zoom, fit page and transform tests.
- Freeze the active stylus stroke transform and reject palm-driven navigation.

Exit: ink remains anchored at arbitrary zoom and after rotation/configuration changes.

### M1.6 Autosave and reload

- Implement a small versioned stroke codec, checksum journal and atomic snapshot.
- Queue completed commands to the persistence actor.
- Reload on launch and expose Saved/Saving/Failed state.
- Add process-kill, torn-tail and corrupt-snapshot recovery tests.

Exit: killing the process after strokes never invalidates the last durable state and a recoverable tail is reported.

### M1.7 Hardware acceptance

- Profile Xiaomi Pad 5 release build with long writing sessions.
- Record pressure distribution, dropped/cancelled events, frame timing, allocations and battery/thermal observations.
- Tune default pressure and stabilization values from evidence.

Exit: the user approves handwriting feel before Milestone 2 begins.

## Verification and dependency references

- [AndroidX Ink setup](https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-setup)
- [AndroidX Ink releases](https://developer.android.com/jetpack/androidx/releases/ink)
- [Advanced stylus features](https://developer.android.com/develop/ui/compose/touch-input/stylus-input/advanced-stylus-features)
- [Compose BOM](https://developer.android.com/develop/ui/compose/bom)
- [Android Gradle Plugin 9.3 compatibility](https://developer.android.com/build/releases/agp-9-3-0-release-notes)
- [Built-in Kotlin in AGP 9](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Room releases](https://developer.android.com/jetpack/androidx/releases/room)

Dependency versions must be rechecked against official release notes before intentional upgrades. Upgrades are isolated changes with build, migration, serialization and on-device ink regression tests.
