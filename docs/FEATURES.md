# Mheenotes Feature Inventory

This document records the user-visible features present in the current Mheenotes source tree (`0.9.0-rc11`). It is the canonical product checklist for the app. Features that are only proposed or partially available are listed separately at the end and are not presented as completed work.

## Notebook library

- Main, Recent, Favorites, and Trash sections.
- Main is the root folder rather than a separate catch-all view.
- Main expands in the sidebar into a navigable hierarchy of folders and notebooks.
- Nested folders can be expanded and collapsed independently in the sidebar.
- Grid and list layouts.
- Sorting by name, recently used, or page count.
- Search by notebook title or folder path.
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
- Drag-and-drop notebooks into folder cards.
- Drag-and-drop notebooks into folders in the sidebar hierarchy.
- Drag-and-drop notebooks onto Main to move them to the root.
- High-layer drag preview uses the notebook thumbnail without a placeholder pencil or title label.
- Notebook-opening loading overlay and library-to-editor transition.
- Responsive library layouts for portrait, landscape, tablet, and resizable windows.

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
- Deleting or moving the original PDF does not break the notebook.
- PDF backgrounds survive app exit, reopen, and `.mynote` export/import.
- Lazy rendering of pages near the viewport.
- Bounded PDF page and thumbnail caches.
- PDF page thumbnails.
- Text search for supported text PDFs/platform versions, with navigation between matches.
- Page bookmarks.
- Export the full notebook or a chosen page range as PDF.
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
- Export notebook as PDF.
- Export current page as an image.
- Export editable `.mynote` backup.
- Share notebook PDF.
- Share current-page image.
- Share editable `.mynote` notebook.
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

## Not complete or intentionally deferred

The following ideas are not complete features in the current build:

- Automatic speech-to-text extraction for newly recorded audio. The player can display and synchronize a transcript already embedded in a notebook, but the app does not currently generate one.
- Word-under-finger OCR selection on images or scanned PDF pages. Full-image offline OCR is implemented; precision word/line handles remain deferred.
- General handwriting-to-text or digital-ink recognition.
- Textured raster pencil simulation beyond the current vector pencil style.
- Additional decorative transitions that are not essential to editing.
- Final real-device release validation for very large PDFs, low-memory recovery, long stylus sessions, latency, and battery use.

See [DEFERRED_FEATURES.md](DEFERRED_FEATURES.md) for the detailed OCR and motion notes, and [MILESTONE_STATUS.md](MILESTONE_STATUS.md) for engineering and release-gate status.
