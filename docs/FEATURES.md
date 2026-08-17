# Mheenotes Feature Inventory

This document records the user-visible features present in the current Mheenotes source tree (`0.9.0-rc11`). It is the canonical product checklist and behavior reference for the app. It describes what each workflow does, how related data is retained, and important platform-dependent behavior. Features that are only proposed or partially available are listed separately at the end and are not presented as completed work.

Last source audit: 2026-08-18.

## Notebook library

- Main, Recent, Favorites, and Trash sections.
- Main is the root folder rather than a separate catch-all view.
- Main expands in the sidebar into a navigable hierarchy of folders and notebooks.
- Nested folders can be expanded and collapsed independently in the sidebar.
- Grid and list layouts.
- Sorting by name, recently used, or page count.
- Search by notebook title or folder path.
- A blank search respects the currently selected Main folder; entering a query searches active notebooks across the folder hierarchy.
- Notebook cards with fixed-height, ellipsized titles.
- First-page thumbnail previews and customizable cover colors.
- Hover feedback for mouse and stylus users.
- Stationary long-press notebook menu; releasing after the menu appears cannot open the notebook underneath it.
- Moving into a notebook drag prevents the actions menu from opening on release.
- Notebook actions include:
  - Open.
  - Open in a new Android document/window.
  - Add to or remove from Favorites.
  - Rename.
  - Duplicate.
  - Export an editable `.mynote` file.
  - Move into a folder or back to Main.
  - Change cover color.
  - Move to Trash.
- Individual notebook Trash restoration and permanent deletion.
- Direct child folders displayed beside notes in the library grid.
- Nested folder paths.
- Folder action menu with rename and Move to Trash.
- Renaming a folder rewrites every descendant folder and notebook path.
- Folder cards can be dragged into another folder or back to Main as a complete subtree.
- Moving a folder to Trash moves every active notebook and nested folder inside it as one recoverable unit.
- Restoring a trashed folder restores the complete directory structure and only the notebooks moved by that folder operation.
- Notebooks that were already individually trashed remain in Trash when a containing folder is restored.
- New-folder icon beside the new-note button.
- New folders are created inside the currently selected folder, or directly in Main when no folder is selected.
- Folder-name collisions are resolved with numbered suffixes such as `Folder (2)`.
- Drag-and-drop notebooks into folder cards.
- Drag-and-drop notebooks into folders in the sidebar hierarchy.
- Drag-and-drop notebooks onto Main to move them to the root.
- High-layer drag preview uses the notebook thumbnail without a placeholder pencil or title label.
- Notebook-opening loading overlay and library-to-editor transition.
- Responsive library layouts for portrait, landscape, tablet, and resizable windows.
- Notebook display names do not have to be unique. Separate notebooks may intentionally share the same title because identity is based on an internal UUID rather than the visible name.

## Creating notebooks and pages

- New-notebook dialog with a title and cover color.
- Standard paper sizes:
  - A3.
  - A4.
  - A5.
  - US Letter.
  - Screen size.
  - 16:9.
  - Custom width and height.
- Portrait and landscape page orientation.
- Page templates:
  - Blank.
  - Ruled.
  - Grid.
  - Dotted.
  - Cornell.
  - Music staff.
- Custom paper background and template-line colors.
- Page-insertion dialog using the same size, orientation, template, and color choices.
- “Don’t ask again” option for page insertion; the chooser can be re-enabled in App Settings.

## Editor workspace

- Dark, tablet-oriented editor with the paper rendered below the app controls.
- Continuous document viewport: every page is positioned in one vertically scrolling canvas.
- One-finger scrolling with inertial/fling movement.
- Two-finger panning in every direction.
- Responsive pinch zoom across the complete page stack.
- Minimum zoom recenters the document.
- Each notebook remembers its last canvas center and zoom level, including after an app restart or `.mynote` backup/import.
- View memory stores the world-space center and zoom relative to minimum zoom, so restoration remains meaningful after orientation or window-size changes.
- Direct navigation and animated jumps to a selected page.
- Back button returns to the library instead of immediately quitting the app.
- Back first closes a dialog, crop session, or active object/lasso/text/audio selection.
- System status-bar inset is respected.
- Bottom system navigation bar is hidden in immersive mode and can be revealed transiently by swiping.
- Portrait/landscape changes are handled without recreating the editing activity.

## Tabs and opening notes

- Chrome-style tabs for multiple open notebooks.
- Select, close, and reorder tabs.
- Tab dragging is constrained to the visible tab strip.
- Opening an already open notebook reuses its tab.
- “Open in new window” starts a separate Android document task for system multi-window use.
- Floating Notes overlay:
  - Opens from a configurable physical/Bluetooth keyboard key or stylus barrel-button input.
  - Captures a replacement shortcut directly inside Settings, including physical keyboard and compatible stylus-button events.
  - Covers the complete app window and smoothly fades the editor blur/dimming beneath it.
  - Shows each notebook as a compact main-menu card with its first-page thumbnail, title, page count, and folder.
  - Scatters cards into randomized positions with gentle independent movement.
  - Progressively reduces card size as the notebook count grows, switching to compact thumbnail/title cards for crowded collections.
  - Full cards show title, page count, and folder; compact cards retain the thumbnail and title and progressively remove secondary metadata.
  - Cards collide with one another, can be dragged, and retain throw velocity after release.
  - Keeps notebooks from the same folder visually near one another.
  - Provides a centered pill-shaped title search; non-matching cards grey out without rearranging the scene.
  - Uses staggered fade/scale entrance animation for the overlay, search field, and every note card.
  - Opens a new tab or focuses the existing tab when a card is tapped.
  - Dismisses when the user taps any free background area.

## Document and page navigation

- Collapsible page-thumbnail side rail.
- Page thumbnails include ink, objects, templates, and imported PDF backgrounds.
- Tap a thumbnail to navigate to that page.
- Page reordering targets the measured gaps between thumbnails and highlights the exact insertion gap while dragging.
- Reorder targeting uses actual on-screen thumbnail bounds rather than an assumed fixed row height, so mixed page aspect ratios are supported.
- Bookmark individual pages.
- Bookmark-only page filter.
- Long-press drag page reordering with a visible origin placeholder and movement animation.
- Insert a page before or after the current page.
- Duplicate the current page, including its ink, objects, settings, and PDF background.
- Delete a page while guaranteeing the notebook retains at least one page.
- Rotate a page in 90-degree steps.
- Cycle the page template.
- Change paper background and template-line colors.
- Jump to a page by number.
- Separate undo and redo history for page operations.
- Copy or move the current page to another open notebook.

## Editor chrome and toolbar

- Compact document bar with:
  - Back to library.
  - Page-rail toggle.
  - Editable notebook name and save-state feedback.
  - Undo and redo.
  - Compact document-actions menu.
- Icon-based drawing toolbar.
- Toolbar can dock below the tabs, on the left, or on the right.
- Horizontal toolbar at the top and vertical toolbar at either side.
- Stylus-hover tooltips for toolbar and contextual-action icons.
- Theme-aware menus, surfaces, rounded corners, status bar, and system-bar icon contrast.

## Pen, pencil, and highlighter

- Active-stylus drawing powered by AndroidX Ink.
- Low-latency wet-ink preview followed by persistent dry-ink rendering.
- Stable final geometry: completing a stroke does not intentionally simplify or shrink it.
- Pressure-sensitive stroke width, with a toggle to disable pressure behavior.
- Separate pen, pencil, and translucent highlighter tools.
- Three independent color presets for each writing tool.
- Three independent width presets for each writing tool.
- Pen, pencil, and highlighter explicitly start on their middle width preset.
- Hold a toolbar color preset to edit it immediately in the circular color picker.
- Drag a width-preset circle to edit its value.
- Small animated center-canvas stroke-size preview while changing width.
- Per-tool selected color and width are visibly marked in the toolbar.
- Floating color editor with:
  - Circular hue/saturation wheel.
  - Brightness control.
  - Hex color entry.
- Configurable maximum writing-tool width, with a 5 mm default limit.
- Completed strokes participate in notebook undo/redo, saving, export, lasso, and erasing.

## Finger, stylus, and input behavior

- Stylus writes while a finger scrolls or manipulates eligible objects.
- Finger motion is distance-gated for text and audio creation so a scroll does not create an object.
- Finger and stylus can select, move, and resize supported objects after selection.
- Two simultaneous fingers always control viewport pan/zoom.
- Hover-aware controls for compatible pens.
- Stylus diagnostics display action/tool type, pressure, tilt, and pointer count.
- Activity-level hardware shortcut capture for Floating Notes, including physical/Bluetooth keyboards and compatible stylus buttons.

## Erasing and stroke correction

- Whole-stroke eraser.
- Partial-stroke eraser.
- Scribble-to-erase while using ordinary pen, pencil, or highlighter tools.
- Low, Medium, and High scribble sensitivity settings.
- Scribble recognition is prioritized independently from Action Maps.
- Draw-and-hold correction for:
  - Straight lines.
  - Rectangles.
  - Ellipses.
- Corrected geometry replaces the original wet preview while the hold is detected.
- Straight-line correction preserves the stroke's actual start and end direction.
- Corrected strokes and shape-tool geometry can be erased.

## Undo, redo, autosave, and recovery

- Ink undo and redo.
- Object-edit undo and redo.
- Page-operation undo and redo.
- Incremental autosave after completed edits.
- Checksummed recovery journal for completed strokes and undo/redo operations.
- Atomic manifest writing with fallback recovery data.
- Bounded in-memory histories to prevent unlimited growth.
- Deleting operations that cannot be undone use confirmation dialogs.

## Lasso and mixed selection

- Freeform lasso selection.
- Mixed selection of ink strokes and supported page objects.
- A lasso selection owns the interaction until it is dismissed; image or text tapping does not replace it accidentally.
- Live visual position updates while dragging selected content.
- Move selected content between pages.
- Rotate selected content by 90 degrees.
- Apply the current ink color or width where supported.
- Cut, copy, paste, duplicate, and delete.
- Screenshot any lassoed page region, including empty paper or a region containing ink and objects.
- Screenshot output is always a rectangle calculated from the lasso path's minimum and maximum X/Y bounds.
- The screenshot preview provides Cancel, Save, and Insert actions.
- Save writes a PNG through Android's file picker; Insert creates a normal editable image object without first writing an external file.
- Screenshot rendering includes the paper or PDF background, ink, shapes, text, and images while excluding editor toolbars and selection outlines.
- Move selection to the previous or next page.
- Compact icon-only contextual actions with stylus-hover tooltips.
- Contextual buttons are created transparent and animate in from left to right with staggered fade, slide, and scale motion.

## Images

- Import images through Android's document/photo picker.
- Preserve the imported image's original aspect ratio.
- Select an image with a short finger or stylus press-release while any normal tool is active.
- Selection-first behavior prevents an initial press from immediately moving the image.
- Drag a selected image with finger or stylus.
- Resize from an enlarged corner hit target with finger or stylus.
- Rotated-image selection borders and hit testing follow the image transform.
- Lock and unlock an image.
- Locked images cannot be moved and expose only the unlock action.
- Rotate 90 degrees.
- Bring to front or send to back.
- Cut, copy, paste, duplicate, and delete.
- Confirmation before irreversible deletion.
- Visual crop editor:
  - Drag crop handles directly on the image.
  - Dark overlay shows the excluded area.
  - No numeric crop sliders.
  - Applying a crop updates the displayed object's aspect ratio.
- Offline OCR for a complete inserted image.
- OCR result panel supports selecting and copying recognized text.
- Animated, icon-based contextual action bar with hover tooltips.

## Text

- Create text objects with finger or stylus.
- Movement threshold prevents text creation while scrolling.
- Select existing text after creation.
- Tap another text object to switch selection.
- Tap empty paper while text is selected to deselect instead of immediately creating another object.
- Move text directly on the page.
- Edit text content.
- Text size and color controls.
- Bold, italic, and underline.
- Start, center, and end alignment.
- Sans-serif, serif, and monospace families.
- Import and use custom TrueType (`.ttf`) and OpenType (`.otf`) fonts.
- Text outline color and outline width.
- Curved text from -180 to +180 degrees.
- Rotation and z-order controls.
- Copy, duplicate, and delete.
- Object undo and redo.
- Text hit testing follows the rendered text transform.
- Animated contextual action bar shared with other selected objects.

## Shapes

- Shape types:
  - Line.
  - Arrow.
  - Rectangle.
  - Rounded rectangle.
  - Ellipse.
  - Triangle.
- Draw in any drag direction, not only toward the bottom-right.
- Select an existing shape after creation.
- Move and resize an existing shape without creating a replacement.
- Stroke color, stroke width, and optional fill color.
- Rotation, duplication, deletion, and z-order controls.
- Shape lines can be erased.

## Audio notes

- Place and record an audio note directly on a page.
- Android microphone-permission flow.
- WAV/PCM recording stored as a notebook asset.
- Audio creation occurs on a short release; scrolling gestures do not create recordings.
- Tapping an existing audio note while the Audio tool is active selects it instead of starting a new recording.
- Default icon size is calculated as 12% of the screen's shorter axis.
- Move audio notes directly or as part of a lasso selection.
- Scale audio-note icons within supported minimum and maximum bounds.
- Selection bar contains Listen and Delete actions.
- Nearly full-screen listening panel with:
  - Play and pause.
  - Seek slider.
  - Back 10 seconds.
  - Forward 10 seconds.
  - Current and total time.
- If a transcript is already embedded in an imported note, the panel displays it and highlights progress during playback.
- Confirmation dialog before deleting an audio note.
- Audio data is embedded in an exported `.mynote`; the original recording is not required separately.

## Action Maps

- User-trained gesture-to-action mappings.
- Available from App Settings and Editor Settings.
- Two-column manager: compact mapping list on the left and editor on the right.
- Create, edit, and delete mappings.
- Automatically generated `Gesture N` name when the name is left blank.
- Three to twelve recorded examples per mapping.
- Multi-stroke gesture examples.
- Recognition waits for the multi-stroke pause window instead of firing after the first stroke.
- Optional rotation-invariant matching.
- Adjustable confidence threshold.
- Recognition integrated with normal pen, pencil, and highlighter use; no separate lightning-bolt tool is required.
- Optional global enable/disable switch.
- Mappable actions:
  - Undo and redo.
  - Choose pen, pencil, highlighter, eraser, lasso, shapes, or text.
  - Add, previous, or next page.
  - Insert image.
  - Toggle page bookmark.
  - Toggle page overview.
  - Reset zoom.
  - Choose width preset 1, 2, or 3.
- Recognition feedback includes the matched action and confidence.

## PDF workflows

- Import a PDF from the library as a new notebook.
- Import a PDF inside the editor to append its pages to the current notebook.
- Imported pages remain in the same continuous viewport as native pages.
- Draw and place objects over PDF backgrounds.
- Imported source PDFs are copied into Mheenotes-managed notebook storage.
- Importing the same PDF more than once creates independent notebooks/assets. The visible titles may be identical, but their notebook UUIDs and stored PDF copies are different.
- Deleting or moving the original PDF does not break the notebook.
- PDF backgrounds survive app exit, reopen, and `.mynote` export/import.
- Lazy rendering of pages near the viewport.
- Bounded PDF page and thumbnail caches.
- PDF page thumbnails.
- Text search for supported text PDFs/platform versions, with navigation between matches.
- Page bookmarks.
- Export the full notebook or a chosen page range as PDF.
- PDF export dialog includes an editable filename, All pages, Current page, and custom Range choices, plus Save and Share destinations.
- Export the current page as PNG.
- Share exported PDF or page image through Android.
- Receive a PDF through Android Open With or Share, import it, and open the resulting note automatically.

## Native `.mynote` notebooks

- Versioned, editable native notebook format.
- Manifest migrations through the current format version.
- Embedded ink, page properties, images, audio, and source-PDF assets.
- A `.mynote` is self-contained; imported source files may be deleted after a successful notebook save/export.
- Export an editable notebook backup.
- Import and reopen an editable notebook.
- Receive `.mynote` files from Android Open With or Share and open them automatically.
- Safe ZIP extraction with path validation and resource limits.
- Asset identifiers and stored paths reject traversal sequences.

## Exporting and sharing

- Import PDF.
- Three unified export actions replace separate Export and Share duplicates:
  - PDF.
  - Current-page PNG image.
  - Editable `.mynote` notebook.
- Every export dialog provides an editable filename and two destination buttons:
  - Save opens Android's file-location picker using the entered filename.
  - Share creates the file with the entered filename and opens Android's share chooser.
- PDF export additionally provides All pages, Current page, and custom comma/range selection such as `1-3, 7`.
- Image export is explicitly scoped to the current page.
- Editable-notebook export remains self-contained and suitable for later editing or restoration.
- Missing filename extensions are added automatically while an existing correct extension is preserved.
- Unicode and spaces in filenames are retained; unsafe path/control characters are replaced.
- Android `VIEW` and `SEND` intent support for PDF and `.mynote` files.
- FileProvider-based sharing without exposing internal filesystem paths.

## Themes and appearance

- Dark-first default appearance.
- Thirty bundled theme presets inspired by popular VS Code themes.
- Select, add, edit, delete, and reset theme presets.
- Two-column theme manager with presets on the left and editor on the right.
- Editable preset name, light/dark mode, primary color, secondary color, background, and surface.
- Circular hue/saturation color wheel, brightness control, and hex field for color editing.
- Theme colors are applied to library, editor, dialogs, dropdowns, status bar, widgets, and system-bar icon contrast.
- App-wide UI scale from 75% to 135%.
- Saved theme and appearance preferences.

## Settings

- Settings are divided into General, Appearance, Drawing, Fonts, and Input sections.
- General:
  - UI scale.
  - Ask-before-inserting-page preference.
- Appearance:
  - Theme presets and theme editor.
- Drawing:
  - Maximum tool width.
  - Per-tool ink color presets.
  - Draw-and-hold correction toggle.
- Fonts:
  - Import, list, and remove custom fonts.
- Input:
  - Floating Notes hardware shortcut capture.
  - Action Map recognition toggle.
- Editor Settings popup:
  - Current stroke width.
  - Pressure sensitivity.
  - Scribble erase and sensitivity.
  - Toolbar position.
  - Whole/partial eraser mode.
  - Manage Action Maps.
  - Stylus diagnostics.

## Localization and accessibility

- English resources.
- Thai localization.
- Android locale configuration.
- Right-to-left layout support at the application level.
- Content descriptions for primary icon actions.
- Hover tooltips for stylus and mouse use.
- Large selection/resize touch targets where precision is important.

## Home-screen widgets

- Recent Notes widget:
  - Displays up to three recently opened notebooks.
  - Uses the selected app theme.
  - Opens a notebook directly in a new Android document task.
- Seven-day Ink Activity widget:
  - Rolling seven-day window rather than a calendar-week reset.
  - Counts completed ink strokes.
  - Displays the history as a graph.
  - Preserves a wide, shallow graph aspect ratio when the widget is resized.
  - Uses the selected app theme.

## Performance and large-document behavior

- AndroidX Ink wet/dry rendering rather than a chain of duplicated dot shapes.
- Spatial indexing for stroke/object lookup.
- Visible-page and near-page rendering in the continuous viewport.
- Bounded PDF background and thumbnail caches.
- Incremental persistence instead of rewriting all ink after every stroke.
- Viewport-aware pending wet ink while scrolling and zooming.
- Asynchronous thumbnail and PDF work away from stylus input.
- Stress-test coverage for large stroke counts.

## Motion and polish

- Loading overlay when opening a notebook.
- Library/editor transition.
- Animated page navigation and page reordering.
- Staggered contextual buttons that fade, slide upward, and scale into place from left to right.
- Animated stroke-width preview.
- Hover and selected-state feedback across notebook cards and tool controls.

## Detailed workflow and behavior reference

### Notebook identity, titles, and duplicate imports

- Every notebook has a persistent UUID independent of its title, folder, current tab number, and internal working-directory name.
- Renaming changes only the user-visible title; links to the notebook, widget opening, tabs, Trash recovery, and embedded assets continue to use its identity.
- Duplicate notebook titles are allowed.
- Importing two PDFs named `Example.pdf` creates two separate notebooks both titled `Example` unless the user renames one.
- Re-importing a PDF does not overwrite, merge with, or update an earlier import.
- Duplicating a notebook creates a new UUID, adds `copy` to the title, and copies the editable notebook contents and embedded assets.
- Opening a notebook that already has an editor tab focuses the existing tab instead of creating a second tab for the same UUID.

### Main, folders, and hierarchy semantics

- Main is the root directory. It is not a second copy of every notebook.
- With no search text, the Main grid shows only direct children of the selected location: its immediate subfolders and notebooks assigned directly to that folder.
- The expandable sidebar provides a complete hierarchy of active folders and notes.
- A folder is represented by its normalized slash-separated path; all missing parent paths are derived so nested structure remains navigable.
- Dragging a notebook to a folder updates its stored folder path. Dragging it onto Main clears the folder path.
- Dragging a folder moves the entire subtree, including empty descendants and every contained notebook.
- A folder cannot be moved into itself or one of its own descendants.
- When a folder move or rename would collide with an existing path, a numbered suffix is chosen instead of overwriting that folder.
- Renaming a folder rewrites descendant folder paths, notebook paths, expanded-state references, selected-location state, and any associated Trash transaction metadata.

### Trash and recovery semantics

- Moving a notebook to Trash sets a Trash timestamp; it does not immediately delete the notebook directory or embedded assets.
- Restoring an individual notebook clears that timestamp and returns it to its stored folder path.
- Permanently deleting a notebook removes its notebook directory and embedded image, audio, ink, thumbnail, and PDF files and requires confirmation.
- Moving a folder to Trash records a folder-level recovery transaction and moves every currently active notebook in that subtree to Trash.
- The complete folder hierarchy is retained while hidden, including empty nested folders.
- Restoring the folder reactivates only the notebooks moved by that folder transaction.
- A notebook that had already been individually trashed before its parent folder was trashed is deliberately not restored with the folder.
- Trashing a parent that contains an already-trashed child folder merges the child recovery membership into the parent transaction without losing structure.
- In Trash, notes belonging to a trashed folder transaction are represented by the recoverable folder card rather than duplicated as separate loose note cards.

### Canvas position and zoom memory

- Each notebook keeps one last-view state for its continuous page stack.
- The saved state contains the document-space point at the center of the viewport and the zoom multiple relative to that window's minimum zoom.
- View state is updated while panning or zooming and written after a short delay to avoid disk work on every input frame.
- Switching tabs, returning to the library, closing the app normally, or exporting a backup flushes the current view state.
- Restoring on a differently sized or rotated window recalculates screen offsets from the saved document-space center.
- View state is stored separately from the content manifest, so panning alone does not change the notebook's modified-time sort order.
- Reset Zoom replaces the remembered state with minimum zoom and the recentered document.

### Floating Notes crowd behavior

- Floating Notes always represents every active, non-trashed notebook in the device library.
- Card width is calculated from usable overlay area and notebook count, capped at the normal full-card size and a compact minimum size.
- As density increases, cards first shrink proportionally, then hide folder/page details while retaining the thumbnail and ellipsized title.
- Folder identity influences initial placement so related notes begin near one another, but cards remain independently movable.
- Each visible card is a physical body with boundary collision, card-to-card collision, gentle idle velocity, drag control, and release velocity.
- Search matches title text case-insensitively. Matching cards remain vivid; non-matching cards stay in position and become dim/grey rather than disappearing and rearranging the scene.
- Search does not change notebook identity or tab state. Clearing the query restores every card's normal appearance.
- Card positions are generated for each overlay session; the editor canvas position/zoom is persistent, but Floating Notes card positions themselves are not saved.
- With an exceptionally large library, cards stop shrinking at the compact minimum. At that point overlap/collision activity and device load can still increase; title search is the intended way to isolate a target.

### Input arbitration and gesture priority

- Stylus contact is reserved for drawing with pen, pencil, highlighter, or eraser when an ink tool is active.
- A single finger pans/scrolls the continuous document unless it completes a short eligible object interaction.
- Two fingers take ownership of the viewport for pan/zoom, including horizontal movement.
- Text and audio placement begin on release only when press-to-release distance remains below the scroll threshold.
- Images, text, shapes, and audio notes use selection-first behavior so the first short press selects rather than unexpectedly transforming the object.
- When a lasso selection exists, it owns object interaction; tapping an image or text object cannot silently replace the lasso selection.
- Back first dismisses the active crop/editor/player/dialog/selection state. Only after transient state is clear does Back return from editor to library.
- Toolbar stylus hover and long-press are separate: hover can show a tooltip without activating the control, while click/press performs the action.
- Physical keyboard and compatible stylus-button shortcut events are captured before focused Compose controls can consume them.

### Undo and confirmation boundaries

- Ink undo/redo covers committed drawing operations and supported stroke transforms.
- Object undo/redo covers additions and changes to image, text, shape, and audio objects.
- Page undo/redo covers insertion, deletion, duplication, movement, rotation, template/color changes, and supported cross-notebook page operations.
- The histories are deliberately separate so a page reorder does not consume a drawing undo step.
- Selection deletion and individual object deletion show confirmation before removal.
- Audio-note deletion shows its own confirmation because the embedded recording is part of the removed object.
- Permanent notebook deletion shows confirmation and is not recoverable through app undo.
- Moving a notebook or folder to Trash is the recoverable alternative to permanent deletion.

### File ownership and self-contained notebooks

- PDF import first copies the selected file into app-managed storage and then records page-to-source mappings.
- Image import copies image bytes into the notebook's asset directory.
- Audio recording writes a WAV/PCM asset into the notebook's asset directory.
- Ink journals and page/object metadata are stored in the notebook working directory.
- Exporting `.mynote` packages the notebook working directory, including embedded PDFs, images, audio, ink data, and saved view state.
- After a successful import/save, deleting the original PDF, image, or received `.mynote` file does not break the internal notebook.
- PDF export and PNG export are flattened output formats; use `.mynote` when future editing and embedded assets must be preserved.

### Persistence ownership matrix

| Data | Storage scope | Included in `.mynote` | Important behavior |
| --- | --- | --- | --- |
| Notebook title, UUID, pages, templates, folders, bookmarks, object metadata | Notebook manifest | Yes | Versioned and written atomically with a fallback manifest. |
| Ink strokes and recovery journals | Per-page notebook files | Yes | Incremental, checksummed persistence avoids rewriting all ink after every stroke. |
| Imported PDF source | Notebook asset | Yes | Original external PDF is no longer required. |
| Imported images | Notebook assets | Yes | Original external images are no longer required. |
| Audio recordings | Notebook assets | Yes | Playback remains available after source/export transfer. |
| Last canvas center and zoom | Notebook `viewport.properties` | Yes | Does not alter content modified-time sorting. |
| Theme presets, UI scale, tool presets, dock, insertion preference | App preferences | No | Shared by all notebooks on the current app installation. |
| Imported custom fonts | App-private font directory | No | Fonts are installation-wide and are not embedded in notebook backup. |
| Action Maps | App-private action-map file | No | Gesture mappings are installation-wide rather than notebook-specific. |
| Folder-list and folder-Trash transaction metadata | App library preferences | Partly | Notebook folder paths are backed up; empty-folder/library transaction state belongs to the installation. |
| Seven-day ink activity | App/widget preferences | No | Used by the rolling activity widget. |

### Android integration, permissions, and system behavior

- Minimum Android API is 23; the build targets API 37.
- Mheenotes uses Android's document/photo pickers and URI grants rather than requesting broad filesystem access.
- Microphone permission is requested only for recording audio notes.
- Share operations use a private `FileProvider`; internal filesystem paths are not sent directly to other apps.
- Android `VIEW` and `SEND` intents are registered for PDF and Mheenotes notebook MIME types, with ZIP/octet-stream fallbacks for compatible file managers.
- “Open in new window” uses Android document tasks and depends on the device's multi-window/task implementation.
- The editor opts into resizable activity behavior and handles orientation/screen-size changes without intentionally recreating the activity.
- Cloud backup and automatic device-transfer backup are disabled for app-private data; users should export `.mynote` backups for portable recovery.
- PDF text search and navigable PDF links require Android API 35 or newer because they use newer platform `PdfRenderer` APIs. PDF rendering/import itself works on older supported versions.
- Stylus barrel-button detection depends on the device/pen exposing standard Android stylus or button events.

### Rendering, caching, and responsiveness details

- Permanent ink uses AndroidX Ink stroke geometry rather than rendering a chain of overlapping dot shapes.
- Wet ink is shown through the low-latency authoring overlay; committed strokes are transferred to the persistent viewport renderer.
- Pending/action-candidate ink is rendered with viewport offsets so it remains attached to the page during scroll and zoom.
- The continuous canvas only traverses pages intersecting the visible world-space range.
- PDF rendering runs through a serialized background worker because Android's `PdfRenderer` opens one page at a time.
- PDF backgrounds and page thumbnails use bounded caches and recycle evicted bitmaps.
- Thumbnail requests are asynchronous and limited to the currently relevant library/side-rail pages.
- Stroke hit testing and scribble/lasso operations use spatial indexing rather than scanning every point in every stroke.
- Floating Notes collision work grows with the number of visible cards; adaptive sizing improves fit but does not make an unlimited library free to simulate.

### Error handling and safety

- Failed imports and exports are surfaced through an in-app error dialog rather than silently ignored.
- `.mynote` extraction rejects absolute paths, traversal segments, escaping canonical paths, excessive entry counts, oversized individual entries, and oversized total archives.
- Imported source paths stored in a notebook reject traversal syntax.
- Manifest publication writes a temporary file, retains a previous backup during replacement, and restores the backup if publication fails.
- Stroke recovery records use checksums so corrupt or incomplete journal entries are not accepted as valid edits.
- A notebook always retains at least one page; deleting the sole page is prevented.

## Not complete or intentionally deferred

The following ideas are not complete features in the current build:

- Automatic speech-to-text extraction for newly recorded audio. The player can display and synchronize a transcript already embedded in a notebook, but the app does not currently generate one.
- Word-under-finger OCR selection on images or scanned PDF pages. Full-image offline OCR is implemented; precision word/line handles remain deferred.
- General handwriting-to-text or digital-ink recognition.
- Textured raster pencil simulation beyond the current vector pencil style.
- Additional decorative transitions that are not essential to editing.
- Final real-device release validation for very large PDFs, low-memory recovery, long stylus sessions, latency, and battery use.

See [DEFERRED_FEATURES.md](DEFERRED_FEATURES.md) for the detailed OCR and motion notes, and [MILESTONE_STATUS.md](MILESTONE_STATUS.md) for engineering and release-gate status.
