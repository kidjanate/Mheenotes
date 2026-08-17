You are a senior Android engineer, software architect, and product-minded UI engineer.

I want you to help me design and implement a polished, full-featured Android tablet note-taking application inspired by the capabilities of apps such as Goodnotes, but with its own architecture, interaction design, and visual identity.

The primary target device is an Android tablet with an active stylus, initially:

* Xiaomi Pad 5
* Xiaomi stylus

The app should be designed Android-first.

The highest priorities are:

1. excellent handwriting feel
2. low stylus latency
3. reliability
4. clean and polished UI
5. PDF/note usability
6. good performance on large notebooks
7. extensible architecture
8. user-customizable stylus workflows

---

# Primary technology

Use:

* Kotlin
* Jetpack Compose
* AndroidX Ink where appropriate
* native Android `MotionEvent` APIs where lower-level stylus information is required
* Room for structured metadata
* Kotlin Coroutines
* Flow
* Android Storage Access Framework
* Android Photo Picker
* native Android PDF APIs where suitable

Avoid:

* Flutter
* React Native
* Unity
* Godot
* WebView-based UI

unless there is a very strong technical reason.

The handwriting/input engine should stay as close to native Android as practical.

Keep the underlying notebook format sufficiently platform-independent that an iOS or desktop frontend could potentially be implemented later.

---

# Overall product goal

Build a complete tablet handwriting/note-taking application supporting:

* handwritten notebooks
* imported PDFs
* PDF annotation
* page insertion/deletion/reordering
* pressure-sensitive stylus input
* pressure toggle
* configurable pressure response
* pen
* pencil
* highlighter
* erasers
* custom colors
* lasso selection
* text boxes
* images
* basic shapes
* page templates
* folders
* notebook organization
* bookmarks
* search
* clipboard
* undo/redo
* autosave
* crash recovery
* native editable notebook files
* PDF export
* image export
* Action Maps
* scribble-to-erase

The app should work fully offline for all core functionality.

---

# Architecture

Separate at least these concerns:

* Compose UI
* ink/stroke engine
* stylus/input handling
* document model
* page model
* object model
* PDF rendering
* PDF import/export
* persistence
* native notebook file format
* image handling
* text handling
* selection/lasso
* undo/redo
* Action Map recognition
* search
* thumbnail generation
* caching
* autosave/recovery
* library/folder management

Do not make the UI layer responsible for document persistence or ink storage.

Do not make the PDF itself the application's editable document model.

---

# Document model

The notebook should maintain its own editable internal document structure.

Conceptually:

Notebook

* metadata
* pages
* page ordering
* bookmarks
* source documents
* settings
* format version

Each page may contain:

Page

* background/template
* optional PDF source page
* ink strokes
* highlighter strokes
* images
* text objects
* shape objects
* future object types

Think of the page as an editable scene/document composition rather than as a bitmap.

Example:

Page
├── Background
├── PDFContent
├── InkStroke
├── InkStroke
├── HighlighterStroke
├── ImageObject
├── TextObject
├── ShapeObject
└── ...

Editable content should stay editable.

Do not flatten the entire page during normal editing.

Flatten/composite only when necessary for:

* export
* preview
* caching
* thumbnails

---

# Native notebook file format

The application's primary editable format must NOT be PDF.

Create a native notebook file format.

For example:

`Statistics.mynote`

From the user's perspective it should preferably behave as one portable file.

Internally it may be a container containing data such as:

Statistics.mynote
├── manifest
├── database
├── pages
├── ink
├── images
├── source PDFs
├── thumbnails
└── additional assets

Possible implementation approaches include:

* custom container
* ZIP-like container
* internal folder representation while editing + packaged representation for backup/export

Choose the architecture based on reliability and performance.

The native format must preserve:

* notebook title
* metadata
* creation/modification timestamps
* page ordering
* page dimensions
* page rotation
* page templates
* PDF source-page references
* ink strokes
* stylus pressure data where useful
* brush settings
* highlighter strokes
* images
* image transforms
* text objects
* shapes
* z-order
* bookmarks
* custom page backgrounds
* future object types

PDF is an export/interchange format, not the source of truth.

---

# File format versioning

Every native notebook must contain a format version.

For example:

`formatVersion = 1`

Future app versions must be able to migrate older notebook formats.

Never silently make existing notebooks unreadable after updates.

Design migrations from the beginning.

---

# Autosave

The app should primarily autosave.

Users should not need to press Save after handwriting.

Examples:

draw stroke
→ save stroke

erase stroke
→ persist erase operation

move image
→ persist transformation

insert page
→ persist page change

reorder pages
→ persist page order

change text
→ persist text

Do not rewrite an entire 500-page notebook after every small action.

Use incremental persistence.

Show subtle status if useful:

* Saving…
* Saved
* Recovery available

Do not let save indicators interrupt handwriting.

---

# Crash safety and recovery

Losing handwriting is unacceptable.

Design for:

* process death
* app crashes
* low-memory termination
* device reboot
* interrupted writes
* low storage
* partial write failure

Consider:

* transactions
* journals
* write-ahead logging
* atomic file replacement
* incremental operation logs
* periodic snapshots

If a save operation fails midway, previously valid notebook data must remain recoverable.

Provide crash/session recovery where appropriate.

---

# Native notebook backup

Allow the user to export/share the editable native notebook.

For example:

`Statistics.mynote`

Another installation of the app should be able to import it and restore the fully editable notebook.

Support:

* native notebook export
* native notebook import
* backup
* restore
* duplicate notebook
* rename notebook
* move notebook
* copy notebook

---

# Library and folder system

Create a polished tablet library interface.

Support:

* folders
* nested folders if this remains architecturally clean
* create folder
* rename folder
* delete folder
* move notebooks
* notebook thumbnails
* recently opened documents
* favorite documents
* sorting
* search
* grid view
* list view
* document rename
* duplicate notebook
* delete notebook
* trash/recycle bin
* restore from trash
* permanent deletion
* import PDF
* import native notebook
* create notebook

The app should feel designed for a tablet rather than like a phone layout stretched to a larger screen.

---

# Trash / recycle bin

Deleting a notebook should not immediately destroy it.

Implement a trash system.

Support:

* move notebook to trash
* restore
* permanently delete
* optionally auto-delete old trash after a configurable period later

Page deletion should also be undoable.

---

# Notebook creation

When creating a notebook, allow options such as:

* title
* paper size
* orientation
* page template
* paper/background color
* cover
* default pen preset if useful

Initial paper sizes should include:

* A4
* A5
* Letter
* screen/tablet ratio
* custom size

Support portrait and landscape.

---

# Page templates

Support built-in templates such as:

* blank
* ruled
* grid
* dotted
* Cornell
* music staff if practical
* custom templates

Allow:

* different background colors
* different line/grid colors
* custom imported image/PDF page backgrounds
* per-page template changes

Do not require every page in a notebook to use the same template.

---

# Notebook covers

Support notebook covers.

Allow:

* built-in cover styles
* custom colors
* optional custom image
* change cover later

Covers are mostly visual metadata and should not interfere with the actual document model.

---

# Page management

Support:

* insert page before
* insert page after
* append page
* delete page
* duplicate page
* reorder pages with drag-and-drop
* move page between notebooks
* copy page between notebooks
* import page(s) from another PDF
* import page(s) from another notebook where feasible
* rotate page 90°
* rotate page 180°
* rotate page 270°
* jump to page number
* page thumbnails
* bookmark page

Operations must be undoable whenever practical.

---

# Page navigation

Support smooth tablet navigation.

Include:

* vertical or horizontal page navigation depending on chosen UX
* pinch-to-zoom
* two-finger pan
* fit width
* fit page
* reset zoom
* configurable minimum/maximum zoom
* jump to page
* page overview
* thumbnail strip/panel

Avoid loading every page simultaneously.

Use lazy loading.

---

# Stylus / ink engine

This is the most important subsystem.

Optimize for:

* extremely low perceived latency
* smooth handwriting
* accurate sampling
* pressure sensitivity
* palm rejection
* stylus/finger distinction
* predictable coordinate transforms
* efficient rendering
* large documents

Capture information where supported:

* X
* Y
* timestamp
* pressure
* tilt
* orientation
* tool type
* stylus button state
* historical/coalesced motion points
* hover where useful

Do not assume every device provides every signal.

Implement sensible fallbacks.

---

# Stylus diagnostics

Include a developer-accessible Stylus Diagnostics screen.

Display live information such as:

* X
* Y
* raw pressure
* normalized pressure
* tilt
* orientation
* hover/distance if available
* stylus buttons
* tool type
* event action
* pointer count
* device/source identifier if useful
* palm/cancel events where visible

This is especially important for testing Xiaomi Pad 5 stylus behavior.

---

# Stylus calibration

Provide optional stylus pressure calibration.

The user may draw:

* light strokes
* normal strokes
* hard strokes

Use collected data to help determine:

* minimum useful pressure
* maximum useful pressure
* pressure curve
* dead zone

Calibration should remain optional.

The app must still work with styluses that report no pressure.

---

# Pressure controls

Pressure sensitivity must be configurable.

At minimum provide:

Pressure Sensitivity:

* On
* Off

When disabled, the selected brush should behave consistently regardless of stylus pressure.

Allow per-tool pressure behavior.

## Pen

Possible controls:

* pressure enabled
* minimum width
* maximum width
* pressure curve
* minimum pressure/dead zone

Conceptually:

raw stylus pressure
→ normalization
→ configurable curve
→ brush response

Do not simply multiply width by the raw hardware pressure value.

Different styluses may report pressure very differently.

## Pencil

Pressure may affect:

* width
* opacity
* texture strength
* darkness

Tilt may affect:

* brush shape
* shading
* texture

## Highlighter

Allow choices such as:

* pressure disabled
* pressure affects width
* pressure affects opacity
* pressure affects both

Provide sensible defaults.

## Eraser

Pressure behavior should also be optional.

---

# Finger/stylus input modes

Support configurable input behavior.

Examples:

Draw with finger:

* On
* Off

Stylus-only writing:

* On
* Off

Suggested default:

Stylus:

* writes

One finger:

* UI interaction or navigation depending on context

Two fingers:

* pan/zoom

Palm:

* ignored/cancelled whenever possible

Writing must not unexpectedly pan because the user's palm touched the display.

---

# Stroke representation

Never save handwriting as screenshots.

Store vector/parametric stroke information.

A stroke should conceptually contain:

* ID
* tool/brush type
* color
* opacity
* base size
* input points
* pressure samples
* tilt/orientation where useful
* timestamp information where useful
* bounding box
* page ID
* z-order/layer info where required

Do not store every single point as an individual Room database row.

Use efficient binary serialization or equivalent.

Design for notebooks containing:

* tens of thousands
* hundreds of thousands
* potentially millions of input points

---

# Spatial indexing

Operations such as:

* eraser
* scribble erase
* lasso
* object selection
* hit testing

must not scan every stroke in the notebook.

Use:

* bounding boxes
* page-level indices
* spatial index such as R-tree/quadtree/grid if appropriate

Keep exact geometric intersection checks limited to nearby candidates.

---

# Pen tool

Provide a smooth pressure-sensitive pen.

Support:

* custom color
* width
* pressure toggle
* pressure curve
* smoothing/stabilization
* presets

The pen should feel good before advanced features are prioritized.

---

# Stroke stabilization

Provide optional stroke smoothing/stabilization.

Users should be able to choose approximately:

* none/raw
* low
* medium
* high

Do not create excessive lag in exchange for smoothing.

The writing experience should remain responsive.

---

# Pencil tool

Architect for a realistic pencil.

Eventually support:

* pressure
* tilt
* texture/grain
* opacity variation
* edge softness
* natural stroke width changes

A simple first implementation is acceptable, but the brush architecture must permit improvement later.

---

# Highlighter

Support:

* custom color
* custom opacity
* custom width
* pressure toggle/behavior
* straight-line behavior if useful

Highlighter overlap should look visually sensible.

Avoid ugly dark multiplication where multiple segments overlap unless intentionally designed.

---

# Eraser

Support:

## Whole-stroke eraser

Touching a stroke removes the whole stroke.

## Partial eraser

Where practical, split stroke geometry and remove only the intersecting portion.

Support:

* configurable size
* stylus pressure optional
* erase highlighter separately if desired
* erase all ink mode if useful

---

# Scribble-to-erase

Implement optional scribble-to-erase.

The scribble itself is NOT a predefined shape.

Detect aggressive scribbling using geometric properties such as:

* path length
* bounding box
* direction changes
* self-intersections
* stroke density
* velocity
* intersections with existing ink

Pipeline:

draw temporary stroke
→ determine whether it resembles intentional scribbling
→ query spatial index for nearby ink
→ compute intersections
→ erase appropriate content
→ discard scribble

Avoid false positives during ordinary handwriting.

Make the feature:

* On/Off
* optionally configurable in sensitivity

---

# Lasso / selection tool

Treat lasso selection as a first-class feature.

Support:

* freeform lasso
* select contained/intersecting strokes
* select images
* select text
* select shapes
* mixed selections where architecture permits

Operations:

* move
* resize
* rotate
* duplicate
* delete
* copy
* cut
* paste
* recolor handwriting
* change stroke width where appropriate
* move selection to another page
* move/copy content between notebooks where practical

Selection must work correctly at arbitrary zoom levels.

---

# Clipboard

Support internal clipboard operations:

* copy
* cut
* paste

For:

* handwriting
* images
* text
* shapes
* mixed selections where practical

Integrate with Android clipboard where appropriate.

Support pasting images/text from other Android apps when possible.

---

# Image insertion

Users must be able to insert images onto notebook pages.

Support:

* Android Photo Picker
* Android file picker
* PNG
* JPEG
* WebP where supported
* transparent PNG
* clipboard paste where available
* tablet drag-and-drop from another app where Android supports it

Images should be editable objects.

Do not flatten images immediately.

An image object should conceptually contain:

ImageObject

* ID
* source
* X/Y
* width
* height
* scale
* rotation
* crop
* opacity
* z-order

Support:

* move
* resize
* preserve aspect ratio by default
* optional free transform
* rotate
* duplicate
* delete
* crop
* flip horizontally
* flip vertically
* bring forward
* send backward
* bring to front
* send to back

Undo/redo must cover image operations.

PDF export must maintain reasonable image quality.

---

# Text boxes

Support typed text.

Text objects should remain editable.

Support:

* type text
* move
* resize text box
* wrapping
* font size
* text color
* bold
* italic
* underline if practical
* text alignment
* font choice
* duplicate
* copy/paste
* rotate if useful
* z-order

Architect text so additional formatting can be introduced later.

---

# Shape tool

Support basic shapes:

* straight line
* arrow
* rectangle
* rounded rectangle if useful
* ellipse/circle
* triangle
* polygon where practical

Shapes should remain editable objects where possible.

Support:

* stroke color
* fill color
* opacity
* line width
* resize
* rotate
* move

---

# Draw-and-hold shape correction

Consider a Goodnotes-like workflow where the user roughly draws:

* line
* rectangle
* circle
* arrow

then briefly holds the stylus at the end.

The app may convert the rough stroke into a clean geometric shape.

This should be optional.

Do not let shape recognition interfere with normal handwriting.

---

# Straight-line behavior

Support an efficient way to draw straight lines.

Possible interaction:

draw line
→ hold stylus briefly
→ snap to straight line

Useful for:

* underlining
* diagrams
* tables
* graphs

---

# Tool presets

Allow saved/favorite tool presets.

Example:

Black Pen

* black
* 1.2 mm
* pressure on

Blue Pen

* blue
* 0.8 mm
* pressure on

Yellow Highlighter

* yellow
* 6 mm
* pressure off

Preset switching must be very fast.

---

# Colors

Support:

* preset colors
* arbitrary custom colors
* recent colors
* favorite colors
* color picker
* hex/RGB input if useful
* eyedropper if feasible later

Avoid forcing the user to reopen a complex picker every time.

---

# Toolbar

Create a clean, stylus-friendly toolbar.

Support quick access to:

* pen
* pencil
* highlighter
* eraser
* lasso
* shapes
* text
* image
* color
* tool presets
* undo
* redo

Consider:

* floating toolbar
* docked toolbar
* movable toolbar
* compact mode
* left/right-handed positioning

The canvas should remain visually dominant.

---

# Undo / redo

Implement a proper command-based history system.

Do not only undo strokes.

Commands may include:

* AddStroke
* DeleteStroke
* SplitStroke
* TransformSelection
* InsertImage
* MoveImage
* CropImage
* CreateText
* EditText
* AddShape
* DeleteObject
* InsertPage
* DeletePage
* MovePage
* RotatePage
* ChangePageTemplate
* ChangeObjectStyle

Avoid storing a complete document copy after every operation.

Undo must accurately restore complex operations.

Example:

delete page
→ Undo
→ page returns with all ink, images, text, bookmarks, and ordering intact

---

# Action Map

Implement a major feature called **Action Map**.

This lets users create their own arbitrary drawing gestures and map them to app commands.

The gestures must NOT come from a predefined list.

The user may draw anything.

Examples:

star-like symbol
→ Undo

Z-like gesture
→ Redo

spiral
→ Eraser

custom meaningless personal symbol
→ Add page

---

# Action Map creation

Flow:

Settings
→ Action Map
→ Add Action
→ Draw gesture
→ Draw it several more times
→ Select action
→ Configure recognition
→ Save

Prefer collecting around 3–5 examples.

Store templates locally.

Do not require the cloud.

Do not initially train a neural network for every user's gesture.

---

# Action Map gesture model

Support multi-stroke gestures.

Conceptually:

Gesture
├── Stroke
│   ├── Point
│   ├── Point
│   └── Point
└── Stroke
├── Point
└── Point

Do not reduce every gesture to one continuous stroke.

---

# Action Map recognition

Use lightweight geometric/template recognition initially.

Possible pipeline:

1. capture stroke(s)
2. resample
3. normalize translation
4. normalize size
5. optionally normalize rotation
6. compare against saved examples
7. calculate similarity
8. trigger only above threshold

Design the recognizer behind an interface so it can be replaced later.

---

# Rotation behavior for gestures

Allow each custom gesture to be:

* rotation sensitive
* rotation invariant

Example:

right arrow
→ next page

left arrow
→ previous page

must remain distinguishable.

---

# Action Map activation

Do NOT interpret all normal handwriting as commands.

Provide explicit command activation.

Potential mechanisms:

* hold stylus button + draw
* hold one finger + draw with stylus
* Action mode
* stylus shortcut
* configurable alternative trigger

Hardware without stylus buttons must still be supported.

---

# Action Map safety

Never execute destructive operations from a low-confidence gesture.

Support:

* confidence threshold
* visual feedback
* optional haptic feedback
* recognized-action preview
* unrecognized gesture handling
* optional cancel

If necessary, destructive mappings should require stronger confidence than benign mappings.

---

# Generic action system

Do not hardcode gestures directly into app features.

Create a generic command/action system.

Potential actions:

* Undo
* Redo
* Pen
* Pencil
* Highlighter
* Eraser
* Lasso
* Shape tool
* Text tool
* image insert
* previous page
* next page
* add page
* page overview
* reset zoom
* toggle toolbar
* choose color preset
* choose width preset
* choose saved tool preset

Eventually support Action Map macros.

Example:

Gesture P:

1. switch to Pen
2. choose black
3. choose 1.2 mm
4. turn pressure on

---

# PDF import

Support importing PDFs through Android's document picker.

Imported PDFs should become resources referenced by the notebook.

Do not destructively modify the source PDF during normal note editing.

A notebook may conceptually contain:

Page 1
→ source PDF page 1

Page 2
→ source PDF page 2

Page 3
→ blank page

Page 4
→ source PDF page 3

This makes insertion/reordering simple.

---

# Imported PDF durability

Do not depend indefinitely on an external source URI that might disappear.

When appropriate, safely retain/copy imported source data into application-controlled notebook storage so a notebook remains usable if:

* Downloads file is moved
* source file is deleted
* URI access disappears
* external provider becomes unavailable

Handle storage efficiently.

---

# PDF viewer behavior

Support:

* smooth rendering
* zoom
* pan
* page thumbnails
* annotation
* PDF text selection/search where supported
* PDF outline/table of-contents where available
* links where practical
* bookmarks

PDF rendering must not block handwriting.

---

# PDF page ordering

Allow PDF-backed and normal pages to be mixed.

Example:

Original PDF:

A
B
C
D

Notebook:

A
B
Blank
Image page
C
D

This should be represented as notebook page metadata, not by rewriting the PDF after every edit.

---

# PDF export

Export the current notebook composition to a new PDF.

Support:

* entire notebook
* current page
* selected pages
* page range

Include:

* PDF-backed content
* blank pages
* handwritten ink
* highlighter
* images
* typed text
* shapes

Prefer preserving original vector/text PDF content where practical.

Do not rasterize everything unnecessarily.

However, provide a fallback rasterized/composited export path for unsupported PDF cases.

Isolate PDF export behind an abstraction so a more capable PDF engine can replace the initial implementation later.

---

# Image export

Allow page export as:

* PNG
* JPEG where useful

Support:

* current page
* selected pages

Respect page dimensions and reasonable output resolution.

---

# Android sharing integration

Support:

* Android share sheet
* share exported PDF
* share image
* share native notebook
* import/open PDFs from other apps
* Open With integration where possible

Opening a PDF from a file manager should be able to launch/import it into the app.

---

# Search

At minimum support:

* notebook title search
* folder search
* file/library search
* PDF text search where available

Later architecture should permit:

* handwriting recognition search
* OCR search

Do not make handwriting OCR mandatory for initial versions.

---

# Bookmarks

Allow users to bookmark notebook pages.

Support:

* add bookmark
* remove bookmark
* list bookmarks
* jump to bookmark
* optional bookmark title/color later

Large PDFs must be navigable without scrolling through hundreds of pages.

---

# PDF outline

When imported PDFs contain an outline/table of contents, expose it if Android's PDF capabilities or selected PDF engine permit it.

Allow users to jump directly to outline destinations.

---

# Recent and favorite notebooks

Support:

* recently opened
* favorites/pinned notebooks

Make frequently used notebooks easy to reach.

---

# Performance requirements

Treat performance as a feature.

A notebook might contain:

* 500+ pages
* large PDFs
* 100,000+ strokes
* large images
* many thumbnails

Requirements:

* lazy load pages
* render only visible/nearby pages
* cache PDF page rendering
* unload distant resources
* asynchronously generate thumbnails
* use spatial indexes
* avoid unnecessary Compose recomposition
* minimize allocation in stylus hot paths
* keep database/file IO away from rendering/input threads
* avoid decoding full-resolution images when thumbnails are sufficient
* use appropriately sized render caches

Profile actual performance.

Do not optimize only based on assumptions.

---

# Storage management

Add storage-awareness.

Eventually support:

* notebook storage size
* total app data usage
* cache usage
* clear render cache
* clear thumbnail cache safely
* low-storage error handling

Never treat user notebook content as disposable cache.

---

# UI / visual design

Visual quality matters greatly.

Create a clean and modern interface with:

* strong typography
* good spacing
* restrained visual hierarchy
* subtle animations
* smooth transitions
* responsive tablet layouts
* landscape support
* portrait support
* light theme
* dark theme
* stylus-friendly controls
* appropriately large touch targets
* polished icons
* uncluttered canvas

Do not merely use default Material components with no customization.

Create an intentional design system.

Define:

* spacing scale
* typography scale
* corner radius system
* elevation system
* icon style
* animation durations
* motion principles
* component states
* tablet breakpoints
* toolbar appearance
* dialog/sheet behavior

Material/Compose can be the implementation foundation, but the visual identity should feel custom and coherent.

---

# Responsive tablet layouts

Use larger displays intelligently.

Examples:

Library:

* side navigation + document grid

Editor:

* collapsible page thumbnail sidebar
* canvas
* toolbar

Settings:

* master/detail layout where appropriate

Avoid stretching phone layouts to tablet width.

---

# Left-handed/right-handed support

Consider stylus ergonomics.

Allow toolbar placement/preferences suitable for:

* left-handed users
* right-handed users

Avoid placing essential controls where the user's drawing hand constantly covers them.

---

# Multi-window and tablet interactions

Where practical, support Android tablet features such as:

* split-screen
* resizable windows
* drag-and-drop
* keyboard shortcuts
* hardware keyboard navigation

These should not compromise the core stylus experience.

---

# Read-only / presentation behavior

Architecture should permit a mode where accidental edits are disabled.

Potential uses:

* reading PDFs
* presenting notes
* reviewing documents

This can come after core editing features.

---

# Accessibility

Do not ignore accessibility.

Support where appropriate:

* content descriptions
* keyboard navigation
* sufficient touch target sizes
* sensible contrast
* scalable UI text
* reduced-motion considerations

The writing canvas itself may naturally have unique accessibility limitations, but surrounding UI should still be accessible.

---

# Future features

Do not necessarily implement these immediately, but avoid architecture choices that make them impossible:

* handwriting recognition
* handwriting-to-text
* OCR
* math recognition
* typed equations
* stickers
* audio recording
* synchronized audio notes
* hyperlinks
* object links
* cloud backup
* cloud sync
* collaborative editing
* version history
* shared notebooks
* infinite canvas
* desktop viewer
* iOS client
* handwriting search
* AI features

These are future capabilities, not requirements for the first milestone.

---

# Known limitations / risks

Design around these from the beginning.

## Stylus hardware

Different devices may expose different:

* pressure ranges
* tilt support
* buttons
* hover behavior
* palm rejection quality

Never assume Xiaomi behavior applies universally.

## Palm rejection

Some palm rejection behavior depends on Android/device firmware.

Build graceful handling but do not pretend the app can completely control hardware palm rejection.

## PDF compatibility

PDF files can contain:

* embedded fonts
* vector graphics
* annotations
* forms
* links
* unusual color spaces
* JavaScript
* protected/encrypted content

Perfect round-trip compatibility for every PDF is not guaranteed.

Prioritize reliable viewing/annotation/export for normal PDFs.

## Gesture recognition

User-defined gestures can be ambiguous.

Explicit Action Map activation is therefore important.

## Pencil rendering

A high-quality textured pencil is much harder than a basic pen.

Do not delay the basic usable application solely to perfect pencil simulation.

## Huge notebooks

A naïve architecture will perform poorly.

Lazy loading and incremental persistence are required from the beginning.

---

# Development strategy

Do NOT try to implement every feature at once.

Build vertical milestones.

---

# Milestone 1 — Stylus proof of concept

Create:

* Android project
* Compose UI
* AndroidX Ink integration
* blank page
* stylus detection
* finger/stylus distinction
* pressure-sensitive pen
* pressure On/Off
* basic pressure curve
* smooth rendering
* undo
* redo
* pan
* zoom
* save/reload strokes
* Stylus Diagnostics
* basic autosave

Test this milestone on real Xiaomi Pad 5 hardware.

The milestone succeeds only if writing feels good.

---

# Milestone 2 — Core ink engine

Add:

* pen presets
* multiple colors
* pencil
* highlighter
* whole-stroke eraser
* partial eraser
* smoothing controls
* pressure controls
* pressure calibration
* spatial indexing
* robust stroke persistence
* scalable rendering
* command-based undo/redo

Stress-test with a very large number of strokes.

---

# Milestone 3 — Notebook/page system

Add:

* native notebook format
* format versioning
* notebook create/open
* multiple pages
* page templates
* paper sizes
* page insert
* page delete
* page duplicate
* page reorder
* page rotation
* thumbnails
* bookmarks
* jump-to-page
* native notebook backup/import

---

# Milestone 4 — Selection and objects

Add:

* lasso
* copy/cut/paste
* images
* image resize/rotate/crop
* text boxes
* shapes
* z-order
* object selection
* selection transformations

---

# Milestone 5 — PDF

Add:

* PDF import
* reliable PDF source retention
* PDF page rendering
* annotation
* mixed PDF/blank pages
* PDF thumbnails
* PDF search
* PDF outline where possible
* selected-page export
* full PDF export
* image export

---

# Milestone 6 — Action Map

Add:

* arbitrary user-created gestures
* 3–5 training examples
* multi-stroke gestures
* geometric recognizer
* gesture threshold
* rotation settings
* explicit activation modes
* generic command integration
* feedback
* Action Map management UI

---

# Milestone 7 — Scribble erase and advanced drawing

Add:

* scribble recognition
* scribble-to-erase
* draw-and-hold straight line
* draw-and-hold shape correction
* improved pencil rendering
* improved partial erasing

---

# Milestone 8 — Library and polish

Add:

* folder system
* nested folders if appropriate
* library search
* recent notebooks
* favorites
* trash
* notebook covers
* grid/list layouts
* settings
* themes
* animations
* responsive tablet layouts
* left/right-handed toolbar options

---

# Milestone 9 — Reliability and optimization

Perform:

* crash testing
* autosave testing
* corrupt-file recovery testing
* very large notebook testing
* huge PDF testing
* large image testing
* low-memory testing
* low-storage testing
* process-kill testing
* performance profiling
* battery profiling where useful
* file migration tests

Do not call the application production-ready until data-loss scenarios have been tested aggressively.

---

# Testing expectations

Write tests for important non-UI logic.

Especially:

* stroke serialization
* document loading
* notebook migrations
* page ordering
* undo/redo
* crash recovery
* Action Map recognition
* spatial queries
* PDF page mapping
* notebook import/export
* coordinate transformations
* lasso geometry

Use instrumentation/device tests for stylus-dependent functionality where appropriate.

---

# Dependency policy

Prefer:

1. Android platform APIs
2. Jetpack libraries
3. small well-maintained third-party libraries only where they add meaningful value

Before introducing a third-party library:

* explain why it is needed
* explain alternatives
* check current maintenance
* check license
* ensure it does not compromise offline use
* ensure it does not lock the notebook format into proprietary structures

---

# How I want you to work

Do NOT immediately generate hundreds of files.

Start by producing a concrete technical design.

First provide:

1. proposed application architecture
2. Gradle/module structure
3. Kotlin package structure
4. document object model
5. stroke model
6. page model
7. image/text/shape object model
8. native `.mynote` file design
9. persistence strategy
10. autosave/recovery strategy
11. undo/redo command architecture
12. input and rendering pipeline
13. AndroidX Ink integration plan
14. pressure handling and normalization
15. PDF architecture
16. spatial indexing strategy
17. Action Map recognition design
18. cache strategy
19. UI design system proposal
20. key technical risks
21. concrete Milestone 1 task breakdown

Challenge requirements when they create a technically bad design.

Do not blindly agree with every proposed implementation.

If a simpler or more robust approach exists, explain it.

When Android APIs or dependency versions may have changed, verify them against current official documentation.

Once the architecture is sound, begin implementing Milestone 1 with clean production-quality Kotlin code.

Do not optimize for quickly producing a demo that will later need to be rewritten.

The architecture should be capable of growing into the complete application described above.

The final goal is not merely:

"a PDF viewer that lets me draw."

The goal is:

**a reliable, polished, tablet-first note-taking application that feels exceptionally good to write in and provides workflows designed specifically around an active stylus.**
