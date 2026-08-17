# Milestone 1 Xiaomi Pad 5 acceptance test

Milestone 1 is accepted only after this checklist passes on the Xiaomi Pad 5 with the Xiaomi stylus. Record the Android/MIUI or HyperOS version and stylus firmware version with the results.

## Install

1. Enable Developer options and USB debugging on the tablet.
2. Connect the tablet and accept its debugging authorization prompt.
3. In Android Studio, select the tablet and run the `app` configuration.
4. Open **Diagnostics** in the Mheenotes toolbar.

## Input capability check

- Stylus tip reports `Stylus`, not `Finger`.
- Pressure changes continuously between light and firm strokes.
- Tilt and orientation are recorded if the device supplies them; unsupported axes may remain zero.
- Hover/distance changes before contact if the device supplies hover distance.
- Each stylus button changes the displayed button bitmask.
- Finger touches never create ink.
- Resting a palm while writing does not pan the page or leave ink.
- Cancelled palm events do not leave a partial stroke.

Record observed pressure values:

| Sample | Minimum | Typical | Maximum |
| --- | ---: | ---: | ---: |
| Light strokes | | | |
| Normal writing | | | |
| Firm strokes | | | |

## Writing feel

- The first stroke does not visibly pause while the Ink engine initializes.
- Fast diagonal strokes track the tip without obvious gaps.
- Small circles and cursive loops do not become angular.
- Slow strokes do not wobble excessively.
- Pressure transitions do not produce sudden width jumps.
- Turning **Pressure** off produces a consistent width.
- Turning **Pressure** on restores light-to-heavy width response.
- The wet AndroidX Ink stroke transitions to the saved stroke without a visible blank frame.

## Navigation and coordinates

- Two fingers pan without producing ink.
- Pinching zooms from fit-page to 8×.
- A stroke remains anchored to the same paper location after zooming and panning.
- **Fit page** restores the complete centered page.
- A palm touching during stylus contact does not begin a two-finger gesture.
- Portrait, landscape, and split-screen layouts remain usable.

## Commands and persistence

- **Undo** removes the last completed stroke exactly.
- **Redo** restores it exactly.
- Drawing after undo clears the redo branch.
- The indicator changes briefly to **Saving…** and returns to **Saved**.
- Force-stop and reopen the app; saved strokes reappear.
- Reboot the tablet; saved strokes reappear.
- Draw many strokes, force-stop during activity, and reopen; the last valid journal state loads.
- A recovered damaged journal shows **Recovered · Saved** and remains writable.

## Stress pass

Write continuously for ten minutes, including fast hatching, dots, long strokes, pressure changes, zooming, and undo/redo. Note:

- visible frame drops;
- delayed or missing strokes;
- unexpected palm marks;
- thermal throttling;
- crashes or save failures;
- pressure range and button behavior that differs from diagnostics expectations.

Do not start Milestone 2 tuning until handwriting feel and data recovery pass this checklist.
