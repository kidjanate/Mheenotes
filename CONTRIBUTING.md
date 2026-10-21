# Contributing to Mheenotes

Thanks for helping. This guide covers how to report problems, suggest features and send changes.

## Reporting a bug

[Open an issue](https://github.com/kidjanate/Mheenotes/issues/new/choose) using the bug report
template and include:

- Your tablet model and Android version
- The Mheenotes version (from the APK file name or the release you downloaded)
- Steps to reproduce, what you expected and what happened
- A screen recording if it is about drawing or rendering

Never attach notebooks or PDFs with personal information.

## Suggesting a feature

Open an issue with the feature request template. Describe the problem first, then the idea.
Mheenotes aims to stay calm and fast, so features that add clutter or slow down the ink may be
declined even if they are good ideas.

## Working on the code

1. Fork the repository and create a branch from `main`: `fix/short-name` or `feature/short-name`.
2. Build and run with `./gradlew :app:installDebug` on a tablet or emulator.
3. Keep the change focused: one fix or feature per pull request.
4. Run `./gradlew :core:allTests` and `./gradlew :app:assembleRelease` before pushing.
5. Open a pull request and fill in the template.

### Code style

- Kotlin, following the existing code: small files, clear names, comments only for the *why*.
- UI is built from the house design components in `ui/.../design`, not Material widgets.
- Anything on the ink path (drawing a stroke while the pen is down) must not allocate per sample
  or wait on I/O. If you touch rendering, mention how you checked performance.
- New UI text goes in `strings.xml`, never in code.

### Strings and translations

All text lives in Compose resources:

- English (default): `ui/src/commonMain/composeResources/values/strings.xml`
- Thai: `ui/src/commonMain/composeResources/values-th/strings.xml`

To add a language, copy the Thai file into `values-<code>/strings.xml` (for example
`values-ja`), translate every entry and add the language to the picker in
`ui/.../library/LibraryModals.kt` (`LanguagePicker`) and `AppLanguage` in `ui/.../Settings.kt`.
Do not escape apostrophes: write `Don't`, not `Don\'t`.

## Pull request checklist

- [ ] Builds with `./gradlew :app:assembleRelease`
- [ ] `./gradlew :core:allTests` passes
- [ ] Tested on a device or emulator, with screenshots or a recording for UI changes
- [ ] New text added to both English and Thai `strings.xml` (ask in the PR if you need help with Thai)
- [ ] No new dependencies without a reason in the PR description; licenses added to
      `ui/.../settings/Licenses.kt`

## Code of conduct

Be kind and patient. Assume good intent, keep feedback about the work, and help newcomers.
Harassment of any kind is not tolerated.
