# Deferred Features

This file records valuable features that are intentionally postponed so core editing, persistence, reliability, and the milestone requirements can be completed first.

## Copy text from images and scanned PDF pages

Status: first useful slice implemented. Inserted images can be recognized fully offline and the result can be selected or copied. The word-under-finger overlay described below remains future precision UX.

Desired interaction:

1. Long-press an image or scanned PDF page with one finger.
2. Run on-device OCR for the touched content.
3. Select the word beneath the finger and display selection handles.
4. Allow the selection to expand across detected words and lines.
5. Offer Copy and Select all through a compact contextual menu.

Implementation direction:

- Use native PDF text selection when the PDF contains real text and the device API supports it.
- Use a bundled, offline text-recognition model for raster images and scanned PDF pages.
- Transform OCR word bounds between bitmap, page, object, and viewport coordinates.
- Cache OCR results by immutable asset digest and OCR model/language version.
- Keep ordinary tapping and dragging for object selection; reserve finger long-press for text extraction.
- Copy recognized text through the Android clipboard without changing the source image.

Known limitations:

- Printed text should be the initial target.
- Handwriting flattened into an image is less reliable than printed-text OCR.
- Digital-ink handwriting recognition requires original ordered stroke data and is a separate future feature.
- OCR must run off the rendering and stylus-input threads.

Remaining reason for deferral:

Recognition itself is only one part of the feature. A polished implementation also requires selection overlays, handles, coordinate transforms, clipboard behavior, caching, language/model handling, and PDF fallbacks. Those tasks should not delay essential notebook and object persistence, complete editing workflows, or data-loss testing.

## Additional motion polish

Status: the library/editor transition, animated page jumps and notebook-opening overlay are implemented. More decorative motion remains optional future polish and must stay off the handwriting path.
