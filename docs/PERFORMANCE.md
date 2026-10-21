# Performance results (Xiaomi Pad 5, Android 13, 120 Hz)

Measured on 2026-10-21 with `adb shell dumpsys gfxinfo` after `cmd package compile -m speed-profile` (what a profile-guided install reaches). Janky = frames missing the 8.3 ms deadline as counted by the framework.

| Scenario | Frames | Janky | p50 | p90 | p99 |
|---|---|---|---|---|---|
| Library, 1,002 notebooks, 12 flings (release) | ~1,700 | 0.4–0.5% | 10 ms | 17 ms | 19 ms |
| Page with 20,000 strokes, 8 finger pans (release) | 1,008 | 0.4% | 9 ms | 12 ms | 13 ms |
| Writing on the 20,000-stroke page (release) | 565 | 1.4% | 10 ms | 12 ms | 23 ms |

Other numbers:
- Cold start to library: ~1.0–1.15 s (`am start -W` TotalTime).
- Opening the 20,000-stroke page: visible tiles fill in within ~1–2 s; the page is interactive immediately.
- Debug builds are much slower (85% janky in the library) and are not representative.

Reproduce (debug build for fixtures, release build for timing; same signing key):

```sh
adb shell am start -n com.mheenotes.next/com.mheenotes.app.MainActivity --es fixture library
adb shell am start -n com.mheenotes.next/com.mheenotes.app.MainActivity --es fixture dense
```

Not yet measured: pinch-zoom frame times (adb `input` cannot pinch), 300-page PDF scrolling, and pen-to-pixel latency (needs a high-speed camera).
