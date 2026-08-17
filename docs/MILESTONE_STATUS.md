# Milestone completion status

This file tracks implementation against `prompt.md`. A milestone is marked complete only when its user-facing workflow and persistence/recovery path are implemented and verified.

## Milestone 1 — Stylus proof of concept

Implemented: Compose Android application, AndroidX Ink stylus authoring, finger/stylus separation, pressure, smooth wet/dry rendering, undo/redo, pan/zoom, diagnostics, incremental journal autosave and reload.

Acceptance: implementation and automated checks complete. Repeat the real-device Xiaomi Pad 5 checklist for each release candidate because stylus latency and firmware palm rejection cannot be validated on the build host.

## Milestone 2 — Core ink engine

Implemented: independent width presets, colors, pen/pencil/highlighter/eraser, whole/partial erasing, pressure controls and calibration, smoothing engine, spatial index, checksummed persistence and command history.

Acceptance: automated 20,000-stroke spatial-index stress coverage is included. Final latency, battery and sustained-load profiling remains a physical-device release check.

## Milestone 3 — Notebook/page system

Implemented: versioned `.mynote` v6 with migration defaults, embedded image/audio/PDF assets, notebook create/open, custom creation sizes/orientation/colors/covers, six templates, insert-before/after setup chooser, delete, duplicate, reorder, page-operation undo/redo, rotation metadata, thumbnails, bookmark filtering, jump, backup/import, Android share/open intake, and cross-notebook page copy/move.

## Milestone 4 — Selection and objects

Implemented: freeform mixed lasso, handwriting/object move/rotate/recolor/width transforms, cut/copy/paste/duplicate/delete and cross-page moves; finger-first image selection; stylus/finger image move and resize; direct-manipulation image crop, rotate, lock and z-order; editable formatted text; six editable shape kinds; object undo/redo; embedded persistence and object-aware export.

## Milestone 5 — PDF

Implemented: durable embedded PDF import, mixed pages, asynchronous near-page rendering, bounded thumbnail cache, annotation, thumbnails, Android 15+ text search and navigable PDF-link UI, bookmarks, all/current/range PDF export, PNG export, sharing and Android Open With integration.

## Milestone 6 — Action Map

Implemented: arbitrary named multi-stroke maps, 3–5 examples, geometric recognizer, threshold, rotation option, explicit button/held-finger activation plus optional pause mode, generic mapped tool/page/image/zoom/width actions, feedback and two-column management UI.

## Milestone 7 — Advanced drawing

Implemented: configurable scribble-to-erase, whole/partial erasers, and conservative draw-and-hold correction for straight lines, rectangles and ellipses. Pencil uses pressure/opacity/tilt-ready samples; a textured bitmap pencil is intentionally not enabled without the renderer texture-store path.

## Milestone 8 — Library and polish

Implemented: tablet library, persistent nested folder paths, search/sort, recents, favorites, trash/restore/permanent deletion, real first-page thumbnails, fixed-height ellipsized cards, hover/long-press actions, covers, grid/list layouts, independent open-note tabs, 30 editable themes with visual color picker, sectioned settings, UI scale, persistent dock choice, responsive scrollable toolbars, staggered object controls, transitions and note-opening state.

## Milestone 9 — Reliability and optimization

Implemented: atomic/fallback manifest writes, incremental journal CRC recovery, safe ZIP extraction limits, v1–v6 migration defaults, atomic batch selection operations, bounded PDF caches, large spatial-index tests, full module unit tests and lint-clean builds.

## Post-milestone additions

Implemented: bundled offline printed-text OCR for inserted images with selectable/copyable results; persisted on-page WAV audio notes with recording, playback, seeking and transcript storage; Android 13+ can feed the saved PCM recording directly to the system recognizer, with a live-recognition fallback on older devices; Android Back returns from editor to library; PDF and `.mynote` receive-through-share integration.

Release gate: low-memory/storage/process-kill, large-image/huge-PDF endurance, stylus latency and battery measurements must still be repeated on the Xiaomi Pad 5. These are device validation tasks, not missing application workflows.
